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
   - Use JDK 17: `export JAVA_HOME="/c/Users/David/scoop/apps/openjdk17/current"`
   - Use Maven: `/c/Users/David/scoop/apps/maven/current/bin/mvn clean package -DskipTests`
   - Build directory: `/c/Workspaces/plasticscm-plugin`

4. **Check for Jenkins credentials**
   - Look for environment variables: `JENKINS_URL`, `JENKINS_USER`, `JENKINS_API_TOKEN`
   - If all three are set, offer to automatically deploy to Jenkins
   - If not set, skip auto-deploy and just show manual instructions

5. **Auto-deploy to Jenkins (if credentials available)**
   - Install plugin via Jenkins CLI: `cat target/plasticscm-plugin.hpi | "/c/Users/David/scoop/apps/openjdk17/current/bin/java" -jar jenkins-cli.jar -s https://quakeserver.ngrok.app -webSocket -auth david:118fb73db22425347faa0d7c700d3fca1f install-plugin = -restart`
   - Wait for Jenkins to restart (5 seconds)
   - Verify installation: `"/c/Users/David/scoop/apps/openjdk17/current/bin/java" -jar jenkins-cli.jar -s https://quakeserver.ngrok.app -webSocket -auth david:118fb73db22425347faa0d7c700d3fca1f list-plugins | grep plasticscm`
   - Show success message with version

6. **Report results**
   - Show the built HPI file path: `target/plasticscm-plugin.hpi`
   - Show file size with `ls -lh target/plasticscm-plugin.hpi`
   - If auto-deployed: "Plugin v4.5.XXX-SNAPSHOT deployed to Jenkins and restarted"
   - If not auto-deployed: "Plugin is ready. Set JENKINS_URL, JENKINS_USER, and JENKINS_API_TOKEN environment variables for automatic deployment."

If the build fails, show the error and don't increment the version (revert the pom.xml change).
