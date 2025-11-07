#!/bin/bash
# Script to update Plastic SCM plugin in Jenkins

# Configuration - UPDATE THESE PATHS
JENKINS_HOME="${JENKINS_HOME:-/c/ProgramData/Jenkins/.jenkins}"
PLUGIN_NAME="plasticscm-plugin"
NEW_PLUGIN="./target/${PLUGIN_NAME}.hpi"

echo "=== Jenkins Plugin Updater ==="
echo "Plugin: $PLUGIN_NAME"
echo "Jenkins Home: $JENKINS_HOME"
echo ""

# Check if new plugin exists
if [ ! -f "$NEW_PLUGIN" ]; then
    echo "ERROR: Plugin file not found: $NEW_PLUGIN"
    echo "Did you run the build? Try: ./build.sh"
    exit 1
fi

# Check if Jenkins is running
echo "Checking if Jenkins is running..."
JENKINS_RUNNING=$(ps aux | grep -i jenkins | grep -v grep)
if [ -n "$JENKINS_RUNNING" ]; then
    echo "WARNING: Jenkins appears to be running. You should stop it first."
    echo "Continue anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
fi

# Backup old plugin
if [ -f "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi" ]; then
    echo "Backing up old plugin..."
    cp "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi" "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi.backup"
fi

# Remove old plugin
echo "Removing old plugin files..."
rm -f "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi"
rm -rf "$JENKINS_HOME/plugins/${PLUGIN_NAME}/"
rm -f "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi.pinned"

# Copy new plugin
echo "Installing new plugin..."
cp "$NEW_PLUGIN" "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi"

# Create .pinned file to prevent auto-updates
touch "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi.pinned"

echo ""
echo "=== Update Complete ==="
echo "Installed: $(ls -lh "$JENKINS_HOME/plugins/${PLUGIN_NAME}.hpi" | awk '{print $5}')"
echo ""
echo "Next steps:"
echo "1. Start Jenkins (if stopped)"
echo "2. OR Restart Jenkins (if running)"
echo "3. Verify plugin version in Manage Jenkins > Plugins"
