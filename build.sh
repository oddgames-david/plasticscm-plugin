#!/bin/bash
# Build script that ensures correct JDK is used

export JAVA_HOME="/c/Users/David/scoop/apps/openjdk17/current"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java version:"
java -version

echo ""
echo "Building plugin..."
/c/Users/David/scoop/apps/maven/current/bin/mvn clean package -DskipTests
