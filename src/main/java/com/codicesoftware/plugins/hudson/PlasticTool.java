package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.jenkins.tools.CmTool;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class that encapsulates the Plastic SCM command client.
 */
public class PlasticTool {

    private static final Logger LOGGER = Logger.getLogger(PlasticTool.class.getName());

    private static final int MAX_RETRIES = 3;
    private static final int TIME_BETWEEN_RETRIES = 1000;

    @Nonnull
    private static String getPluginVersion() {
        try {
            hudson.PluginWrapper plugin = Jenkins.get().getPluginManager().getPlugin("plasticscm-plugin");
            return plugin != null ? plugin.getVersion() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @CheckForNull
    private final CmTool tool;
    private boolean toolValidated = false;
    @Nonnull
    private final Launcher launcher;
    @Nonnull
    private final TaskListener listener;
    @CheckForNull
    private final FilePath workspace;
    @Nonnull
    private final ClientConfigurationArguments clientConfigurationArguments;

    public PlasticTool(
        @CheckForNull CmTool tool,
        @Nonnull Launcher launcher,
        @Nonnull TaskListener listener,
        @CheckForNull FilePath workspace,
        @Nonnull ClientConfigurationArguments clientConfigurationArguments) {
        this.tool = tool;
        this.launcher = launcher;
        this.listener = listener;
        this.workspace = workspace;
        this.clientConfigurationArguments = clientConfigurationArguments;
    }

    /**
     * Validate that the cm tool is available and executable.
     * This runs 'cm version' to check if cm is available.
     *
     * @throws IOException Operation error
     * @throws InterruptedException Process has been interrupted
     */
    private void validateCmTool() throws IOException, InterruptedException {
        if (tool == null) {
            return;
        }

        String cmPath = tool.getCmPath();
        LOGGER.info("Validating Plastic SCM cm tool at: " + cmPath);
        listener.getLogger().println("===== PlasticSCM Plugin v" + getPluginVersion() + " =====");
        listener.getLogger().println("Validating Plastic SCM cm tool: " + cmPath);

        ArgumentListBuilder versionCmd = new ArgumentListBuilder(cmPath);
        versionCmd.add("version");

        ByteArrayOutputStream consoleStream = new ByteArrayOutputStream();

        try {
            Launcher.ProcStarter procL = launcher.launch()
                .cmds(versionCmd)
                .stdout(consoleStream)
                .stderr(consoleStream)
                .pwd(workspace);

            if (tool.isUseInvariantCulture()) {
                Map<String, String> envsMap = new HashMap<>();
                envsMap.put("DOTNET_SYSTEM_GLOBALIZATION_INVARIANT", "1");
                procL.envs(envsMap);
            }

            Proc proc = procL.start();
            int exitCode = proc.join();

            consoleStream.close();
            String output = consoleStream.toString(StandardCharsets.UTF_8.name()).trim();

            if (exitCode == 0) {
                LOGGER.info("Plastic SCM cm tool validated successfully. Version info: " + output);
                listener.getLogger().println("Plastic SCM cm tool validated: " + output);
            } else {
                String errorMessage = String.format(
                    "Failed to validate Plastic SCM cm tool at '%s'. " +
                    "Exit code: %d. Output: %s. " +
                    "Please check that Plastic SCM is installed and the cm executable path is correct.",
                    cmPath, exitCode, output);
                LOGGER.severe(errorMessage);
                listener.fatalError(errorMessage);
                throw new AbortException(errorMessage);
            }
        } catch (IOException e) {
            String errorMessage = String.format(
                "Failed to execute Plastic SCM cm tool at '%s'. " +
                "Error: %s. " +
                "Please check that Plastic SCM is installed and the cm executable path is correct in Jenkins configuration.",
                cmPath, e.getMessage());
            LOGGER.severe(errorMessage);
            listener.fatalError(errorMessage);
            throw new AbortException(errorMessage);
        }
    }

    /**
     * Execute the arguments, and return the console output as a Reader
     *
     * @param arguments arguments to send to the command-line client.
     * @return a Reader containing the console output
     * @throws IOException Operation error
     * @throws InterruptedException Process has been interrupted
     */
    @Nonnull
    public Reader execute(@Nonnull String[] arguments) throws IOException, InterruptedException {
        return execute(arguments, null, true);
    }

    @Nonnull
    public Reader execute(
            @Nonnull String[] arguments,
            @CheckForNull FilePath executionPath,
            boolean printOutput) throws IOException, InterruptedException {
        if (tool == null) {
            throw new InterruptedException("You need to specify a Plastic SCM tool");
        }

        // Validate cm tool is available on first use
        if (!toolValidated) {
            validateCmTool();
            toolValidated = true;
        }

        ArgumentListBuilder cmdArgs = getToolArguments(arguments, clientConfigurationArguments);
        String cliLine = cmdArgs.toString();

        int retries = 0;
        while (retries < MAX_RETRIES) {
            Reader result = tryExecute(cmdArgs, executionPath, printOutput);
            if (result != null) {
                return result;
            }

            retries++;
            LOGGER.warning(String.format(
                    "The cm command '%s' failed. Retrying after %d ms... (%d)",
                    cliLine, TIME_BETWEEN_RETRIES, retries));
            Thread.sleep(TIME_BETWEEN_RETRIES);
        }

        String errorMessage = String.format(
                "The cm command '%s' failed after %d retries", cliLine, MAX_RETRIES);
        listener.fatalError(errorMessage);
        throw new AbortException(errorMessage);
    }

    @Nonnull
    private ArgumentListBuilder getToolArguments(
            @Nonnull String[] cmArgs,
            @Nonnull ClientConfigurationArguments clientConfigurationArguments) {
        if (tool == null) {
            return new ArgumentListBuilder();
        }
        ArgumentListBuilder result = new ArgumentListBuilder(tool.getCmPath());

        result.add(cmArgs);
        return clientConfigurationArguments.fillParameters(result);
    }

    @Nullable
    private Reader tryExecute(
            ArgumentListBuilder args,
            FilePath executionPath,
            boolean printOutput) throws IOException, InterruptedException {
        if (tool == null) {
            return null;
        }

        if (executionPath == null) {
            executionPath = workspace;
        }
        ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();

        // Don't capture stderr at all on Windows - causes handle issues
        Launcher.ProcStarter procL = launcher.launch()
            .cmds(args)
            .stdout(stdoutStream)
            // .stderr() - let stderr go to parent process without capturing
            .pwd(executionPath);

        if (tool.isUseInvariantCulture()) {
            Map<String, String> envsMap = new HashMap<>();
            envsMap.put("DOTNET_SYSTEM_GLOBALIZATION_INVARIANT", "1");
            procL.envs(envsMap);
        }

        Proc proc = procL.start();
        int exitCode = proc.join();

        stdoutStream.close();

        if (exitCode == 0) {
            LOGGER.fine("Command succeeded: " + args);
            return new InputStreamReader(new ByteArrayInputStream(stdoutStream.toByteArray()), StandardCharsets.UTF_8);
        } else {
            String stdoutOutput = stdoutStream.toString(StandardCharsets.UTF_8.name());
            if (!stdoutOutput.isEmpty()) {
                LOGGER.warning("Command failed: " + args + "\nOutput: " + stdoutOutput);
            } else {
                LOGGER.warning("Command failed with exit code " + exitCode + ": " + args);
            }
            return null;
        }
    }
}
