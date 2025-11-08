---
description: Test the PlasticSCM plugin by running a Jenkins job and monitoring results
---

# Test PlasticSCM Plugin

Test the plugin by deploying it to Jenkins and triggering a build.

**Prerequisites:**
- Jenkins credentials in .env file: JENKINS_URL, JENKINS_USER, JENKINS_API_TOKEN
- Test job configured from .env: JENKINS_TEST_JOB (default: `plastic_scm_plugin`)
- Built plugin file at: `target/plasticscm-plugin.hpi`

**Usage:**
```bash
/test                                # Test with default job from .env
/test my_custom_job                  # Test with specific job name
```

**Script Workflow:**

Run the following automated script that handles everything:

```bash
#!/bin/bash
set -e

# Load environment variables
if [ ! -f .env ]; then
    echo "ERROR: .env file not found"
    echo "Create .env with: JENKINS_URL, JENKINS_USER, JENKINS_API_TOKEN, JENKINS_TEST_JOB"
    exit 1
fi

source .env

# Override test job if provided as argument
if [ ! -z "$1" ]; then
    JENKINS_TEST_JOB="$1"
fi

# Validate environment variables
if [ -z "$JENKINS_URL" ] || [ -z "$JENKINS_USER" ] || [ -z "$JENKINS_API_TOKEN" ]; then
    echo "ERROR: Missing Jenkins credentials in .env"
    echo "Required: JENKINS_URL, JENKINS_USER, JENKINS_API_TOKEN"
    exit 1
fi

if [ -z "$JENKINS_TEST_JOB" ]; then
    echo "ERROR: JENKINS_TEST_JOB not set in .env or command line"
    exit 1
fi

echo "===== Testing PlasticSCM Plugin ====="
echo "Jenkins URL: $JENKINS_URL"
echo "Test Job: $JENKINS_TEST_JOB"
echo ""

# Step 1: Ensure Jenkins CLI is available
if [ ! -f jenkins-cli.jar ]; then
    echo "Downloading jenkins-cli.jar..."
    curl -s -O "$JENKINS_URL/jnlpJars/jenkins-cli.jar"
    echo "✓ Downloaded jenkins-cli.jar"
else
    echo "✓ jenkins-cli.jar found"
fi
echo ""

# Step 2: Check if plugin file exists
if [ ! -f target/plasticscm-plugin.hpi ]; then
    echo "ERROR: Plugin file not found at target/plasticscm-plugin.hpi"
    echo "Run /build first to build the plugin"
    exit 1
fi
echo "✓ Plugin file found ($(ls -lh target/plasticscm-plugin.hpi | awk '{print $5}'))"
echo ""

# Step 3: Deploy plugin via Jenkins CLI
echo "Deploying plugin to Jenkins via CLI..."
cat target/plasticscm-plugin.hpi | java -jar jenkins-cli.jar \
    -s "$JENKINS_URL" \
    -auth "$JENKINS_USER:$JENKINS_API_TOKEN" \
    install-plugin = -restart

echo "Waiting for Jenkins to restart..."
sleep 25

# Step 4: Verify plugin installation
echo ""
echo "Verifying plugin installation..."
PLUGIN_VERSION=$(java -jar jenkins-cli.jar \
    -s "$JENKINS_URL" \
    -auth "$JENKINS_USER:$JENKINS_API_TOKEN" \
    list-plugins | grep plasticscm | awk '{print $NF}')

if [ -z "$PLUGIN_VERSION" ]; then
    echo "ERROR: Plugin not found in Jenkins"
    exit 1
fi

echo "✓ Plugin deployed: $PLUGIN_VERSION"
echo ""

# Step 5: Check if job exists
echo "Checking if job exists..."
JOB_EXISTS=$(java -jar jenkins-cli.jar \
    -s "$JENKINS_URL" \
    -auth "$JENKINS_USER:$JENKINS_API_TOKEN" \
    list-jobs | grep "^${JENKINS_TEST_JOB}$" || echo "")

if [ -z "$JOB_EXISTS" ]; then
    echo "ERROR: Job '$JENKINS_TEST_JOB' not found in Jenkins"
    echo ""
    echo "Available jobs:"
    java -jar jenkins-cli.jar \
        -s "$JENKINS_URL" \
        -auth "$JENKINS_USER:$JENKINS_API_TOKEN" \
        list-jobs
    exit 1
fi

echo "✓ Job found: $JENKINS_TEST_JOB"
echo ""

# Step 6: Trigger build and wait for results
echo "===== Triggering Build ====="
echo ""

BUILD_OUTPUT=$(java -jar jenkins-cli.jar \
    -s "$JENKINS_URL" \
    -auth "$JENKINS_USER:$JENKINS_API_TOKEN" \
    build "$JENKINS_TEST_JOB" -s -v 2>&1)

echo "$BUILD_OUTPUT"
echo ""

# Step 7: Analyze results
echo "===== Build Analysis ====="

# Extract key information
BUILD_NUMBER=$(echo "$BUILD_OUTPUT" | grep "Started $JENKINS_TEST_JOB" | awk '{print $3}' | sed 's/#//')
BUILD_STATUS=$(echo "$BUILD_OUTPUT" | grep "Finished:" | awk '{print $2}')
PLUGIN_VER=$(echo "$BUILD_OUTPUT" | grep "PlasticSCM Plugin v" | head -1 | sed 's/.*PlasticSCM Plugin v/v/' | awk '{print $1}')
JENKINSFILE_STATUS=$(echo "$BUILD_OUTPUT" | grep "Obtained Jenkinsfile from Plastic SCM" || echo "")

echo "Build Number: #$BUILD_NUMBER"
echo "Build Status: $BUILD_STATUS"

if [ ! -z "$PLUGIN_VER" ]; then
    echo "✓ Plugin version confirmed: $PLUGIN_VER"
else
    echo "✗ Plugin version not found in output"
fi

if [ ! -z "$JENKINSFILE_STATUS" ]; then
    echo "✓ Jenkinsfile obtained from Plastic SCM"
    echo "  $(echo "$JENKINSFILE_STATUS" | sed 's/Obtained Jenkinsfile from Plastic SCM //')"
else
    echo "✗ Jenkinsfile fetch failed"
fi

# Check for parameter resolution
if echo "$BUILD_OUTPUT" | grep -q "Branch:"; then
    BRANCH=$(echo "$BUILD_OUTPUT" | grep "Branch:" | awk '{print $2}')
    echo "✓ Parameter resolution working (Branch: $BRANCH)"
fi

# Check for errors
if echo "$BUILD_OUTPUT" | grep -qi "error\|failed\|exception"; then
    echo ""
    echo "⚠ Errors detected in build output:"
    echo "$BUILD_OUTPUT" | grep -i "error\|failed\|exception" | head -5
fi

if [ "$BUILD_STATUS" = "SUCCESS" ]; then
    echo ""
    echo "===== ✓ Test Successful ====="
    exit 0
else
    echo ""
    echo "===== ✗ Test Failed ====="
    echo "Check the full build output above for details"
    exit 1
fi
```

**Quick Test Commands:**

Test with default job:
```bash
bash -c 'source .env && [script above]'
```

Test with specific job:
```bash
bash -c 'export JENKINS_TEST_JOB=my_job && source .env && [script above]'
```

**Example Output:**
```
===== Testing PlasticSCM Plugin =====
Jenkins URL: http://localhost:8080
Test Job: plastic_scm_plugin

✓ jenkins-cli.jar found
✓ Plugin file found (1.4M)

Deploying plugin to Jenkins via CLI...
Waiting for Jenkins to restart...

✓ Plugin deployed: 4.5.013-SNAPSHOT (private-44147724-david)
✓ Job found: plastic_scm_plugin

===== Triggering Build =====

Started plastic_scm_plugin #10
Obtained Jenkinsfile from Plastic SCM repository "mock_plastic_scm_plugin@oddgames_external@cloud"
[... build output ...]
Finished: SUCCESS

===== Build Analysis =====
Build Number: #10
Build Status: SUCCESS
✓ Plugin version confirmed: v4.5.013-SNAPSHOT
✓ Jenkinsfile obtained from Plastic SCM
✓ Parameter resolution working (Branch: main)

===== ✓ Test Successful =====
```
