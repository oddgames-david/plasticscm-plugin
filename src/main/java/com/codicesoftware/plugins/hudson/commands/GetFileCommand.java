package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public class GetFileCommand implements Command {

    private final String revSpec;
    private final String filePath;

    public GetFileCommand(String revSpec, String filePath) {
        this.revSpec = revSpec;
        this.filePath = filePath;
    }

    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();
        arguments.add("cat");
        arguments.add(revSpec);
        // Don't use --file= parameter on Windows - causes "The handle is invalid" error
        // Instead, we'll capture stdout directly and write to file in Java code
        // Only add --file if filePath is explicitly set (for backward compatibility)
        if (filePath != null && !filePath.isEmpty()) {
            // Skip --file parameter to avoid Windows handle issues
            // arguments.add("--file=" + filePath);
        }
        return arguments;
    }
}
