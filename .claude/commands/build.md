---
description: Build the PlasticSCM Jenkins plugin (increments version, builds with JDK 17)
---

# Build PlasticSCM Jenkins Plugin

Follow this workflow:

1. **Read current version from pom.xml**
   - Extract the current version number from the `<version>` tag

2. **Increment version**
   - If version is `X.Y.ZZZ-SNAPSHOT`, increment to `X.Y.(ZZZ+1)-SNAPSHOT`
   - Update the version in pom.xml
   - Show the user: "Incremented version from X.Y.ZZZ-SNAPSHOT to X.Y.(ZZZ+1)-SNAPSHOT"

3. **Build the plugin**
   - Use JDK 17: `export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"`
   - Use Maven: `mvn clean package -DskipTests`
   - Build directory: current workspace

4. **Report results**
   - Show the built HPI file path: `target/plasticscm-plugin.hpi`
   - Show file size with `ls -lh target/plasticscm-plugin.hpi`
   - Show success message: "Plugin v4.5.XXX-SNAPSHOT built successfully"
   - Remind user: "Use /test to deploy and test the plugin"

If the build fails, show the error and don't increment the version (revert the pom.xml change).
