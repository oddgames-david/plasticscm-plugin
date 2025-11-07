#!/bin/bash
# Deploy PlasticSCM plugin to Jenkins via CLI

set -e

JENKINS_URL="https://quakeserver.ngrok.app"
JENKINS_USER="david"
JENKINS_API_TOKEN="118fb73db22425347faa0d7c700d3fca1f"
JAVA_BIN="/c/Users/David/scoop/apps/openjdk17/current/bin/java"
CLI_JAR="./jenkins-cli.jar"
PLUGIN_HPI="./target/plasticscm-plugin.hpi"

echo "=== PlasticSCM Plugin Deployment ==="
echo ""

# Check if CLI jar exists
if [ ! -f "$CLI_JAR" ]; then
    echo "Downloading Jenkins CLI..."
    curl -s "$JENKINS_URL/jnlpJars/jenkins-cli.jar" -o "$CLI_JAR"
fi

# Check if plugin exists
if [ ! -f "$PLUGIN_HPI" ]; then
    echo "Error: Plugin HPI not found at $PLUGIN_HPI"
    echo "Run /build first to build the plugin."
    exit 1
fi

# Get plugin version
VERSION=$(grep -oP '<version>\K[^<]+' pom.xml | head -1)
echo "Deploying plugin version: $VERSION"
echo ""

# Install plugin
echo "Installing plugin..."
$JAVA_BIN -jar "$CLI_JAR" -s "$JENKINS_URL" -auth "$JENKINS_USER:$JENKINS_API_TOKEN" install-plugin "$PLUGIN_HPI"

# Safe restart
echo ""
echo "Restarting Jenkins..."
$JAVA_BIN -jar "$CLI_JAR" -s "$JENKINS_URL" -auth "$JENKINS_USER:$JENKINS_API_TOKEN" safe-restart

echo ""
echo "=== Deployment Complete ==="
echo "Jenkins is restarting. Wait ~30 seconds before testing."
echo "Plugin version $VERSION should be active after restart."
