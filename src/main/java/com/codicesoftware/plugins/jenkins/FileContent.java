package com.codicesoftware.plugins.jenkins;

import com.codicesoftware.plugins.hudson.PlasticTool;
import com.codicesoftware.plugins.hudson.commands.CommandRunner;
import com.codicesoftware.plugins.hudson.commands.GetFileCommand;
import com.codicesoftware.plugins.hudson.util.DeleteOnCloseFileInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;

public class FileContent {
    private FileContent() {
    }

    @Nonnull
    public static DeleteOnCloseFileInputStream getFromServer(
            @Nonnull final PlasticTool tool,
            @Nonnull final String serverFile,
            @Nonnull final String repObjectSpec) throws IOException, InterruptedException {
        Path tempFile = TempFile.create();

        String serverPathRevSpec = String.format("serverpath:%s#%s", serverFile, repObjectSpec);

        // Don't pass temp file path to cm command - causes Windows handle issues
        // Instead, capture stdout and write to file ourselves
        GetFileCommand command = new GetFileCommand(serverPathRevSpec, "");

        // Capture output from cm cat command (stdout)
        java.io.Reader reader = null;
        try {
            reader = CommandRunner.execute(tool, command);

            // Read content from stdout and write to temp file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile.toFile());
                 java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader)) {

                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = bufferedReader.read(buffer)) != -1) {
                    writer.write(buffer, 0, charsRead);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return new DeleteOnCloseFileInputStream(tempFile);
    }
}
