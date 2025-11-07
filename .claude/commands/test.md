---
description: Test the PlasticSCM plugin by running a Jenkins job and monitoring results
---

# Test PlasticSCM Plugin

Test the plugin by triggering a Jenkins build and monitoring its output.

**Prerequisites:**
- Plugin must be installed in Jenkins
- Jenkins credentials must be set: JENKINS_URL, JENKINS_USER, JENKINS_API_TOKEN
- Default test job: `game_trucks_off_road_ios_development`

**Workflow:**

1. **Check Jenkins credentials**
   - Verify JENKINS_URL, JENKINS_USER, JENKINS_API_TOKEN are set
   - If not set, show error and instructions

2. **Trigger build**
   - Use Jenkins API to trigger build with parameters
   - Default job: `game_trucks_off_road_ios_development`
   - Get crumb: `curl -s "$JENKINS_URL/crumbIssuer/api/json" --user "$JENKINS_USER:$JENKINS_API_TOKEN"`
   - Trigger: `curl -X POST "$JENKINS_URL/job/{job-name}/build" --user "$JENKINS_USER:$JENKINS_API_TOKEN" -H "Jenkins-Crumb: {crumb}"`
   - Get build number from queue

3. **Wait for build to start**
   - Poll queue URL until build starts
   - Get build number from queue item

4. **Monitor console output**
   - Stream console output: `curl "$JENKINS_URL/job/{job}/{build-number}/consoleText" --user "$JENKINS_USER:$JENKINS_API_TOKEN"`
   - Show output to user in real-time
   - Look for key indicators:
     - "===== PlasticSCM Plugin v" - confirms plugin version loaded
     - "PlasticSCMFileSystem.Builder.build() CALLED" - confirms our code is running
     - "Parameter resolution" logs
     - Success or failure messages

5. **Analyze results**
   - Check if build succeeded or failed
   - Look for specific errors:
     - "Unable to find jenkinsfile" - parameter resolution didn't work
     - "The handle is invalid" - stderr capture didn't work
     - Missing logs - plugin not being called
   - Report findings to user

6. **Suggest next steps**
   - If successful: Report success with key observations
   - If failed: Suggest specific fixes based on error analysis
   - Offer to rebuild and test again

**Usage:**
```
/test
/test game_trucks_off_road_ios_development
```

**Example Output:**
```
Testing PlasticSCM plugin...
Triggering build for job: game_trucks_off_road_ios_development
Build #42 started...

Console Output:
===== PlasticSCM Plugin v4.5.005-SNAPSHOT =====
PlasticSCMFileSystem.Builder.build() CALLED
...

✓ Build succeeded!
✓ Plugin version confirmed: 4.5.005-SNAPSHOT
✓ Parameter resolution logs found
✓ No errors detected
```
