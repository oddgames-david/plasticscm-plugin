# Build Environment & Process

## Current Version

**Always check `pom.xml` for the actual current version.**

**Development Workflow:**
1. **Before building:** Increment the version number in pom.xml (e.g., `4.5.003-SNAPSHOT` → `4.5.004-SNAPSHOT`)
2. Build the plugin
3. Upload to Jenkins
4. The version number ensures Jenkins loads the new code

Using SNAPSHOT suffix allows rebuilding the same version during active development of a specific feature.

## Build Requirements

- **Java**: JDK 17 (OpenJDK 17)
  - Location: `C:\Users\David\scoop\apps\openjdk17\current`
  - DO NOT use JDK 25 - causes Groovy compilation errors
- **Maven**: 3.5 or newer
  - Location: `C:\Users\David\scoop\apps\maven\current`
- **Target Java**: Java 11 (Jenkins plugin requirement)

## Build Commands

### Using VS Code Task (Recommended)
1. Press `Ctrl+Shift+B` (Run Build Task)
2. Select "Maven: Build Plugin"

### Using Command Line
```bash
cd /c/Workspaces/plasticscm-plugin
export JAVA_HOME="/c/Users/David/scoop/apps/openjdk17/current"
/c/Users/David/scoop/apps/maven/current/bin/mvn clean package -DskipTests
```

### Using Build Script
```bash
./build.sh
```

### Automated Deployment (Recommended)

The `/build` command can automatically deploy to Jenkins using the REST API.

**Setup:**
1. Get your Jenkins API token:
   - Go to Jenkins → User Icon (top right) → Configure
   - Click "Add new Token" under API Token section
   - Copy the generated token

2. Set environment variables (add to your shell profile):
```bash
export JENKINS_URL="http://localhost:8080"  # Your Jenkins URL
export JENKINS_USER="your-username"          # Your Jenkins username
export JENKINS_API_TOKEN="your-api-token"    # Your Jenkins API token
```

3. Run `/build` and it will:
   - Increment version
   - Build plugin
   - Uninstall old plugin
   - Restart Jenkins
   - Upload new plugin
   - Restart Jenkins again

## Build Output

- **Plugin file**: `target/plasticscm-plugin.hpi`
- **Expected size**: ~1.4 MB
- **Compiled for**: Java 11 (major version 55)

## Installation in Jenkins

### Development Workflow (Recommended)

**Option A: Use SNAPSHOT Versioning**
1. Change version in `pom.xml` to `4.5-SNAPSHOT`
2. Build plugin
3. Upload via Jenkins UI - it will overwrite automatically
4. Restart Jenkins

**Option B: Use Update Script**
```bash
# First, update JENKINS_HOME path in update-plugin.sh
./update-plugin.sh
```
The script will:
- Backup old plugin
- Remove old plugin files
- Install new plugin
- Remind you to restart Jenkins

### Clean Install (First Time)

**Method 1: Manual Copy**
1. Stop Jenkins
2. Copy `target/plasticscm-plugin.hpi` to `JENKINS_HOME/plugins/`
3. Start Jenkins

**Method 2: Web UI Upload**
1. Go to Jenkins → Manage Jenkins → Plugins → Advanced
2. Upload `target/plasticscm-plugin.hpi`
3. Restart Jenkins when prompted

### Why Updates Don't Overwrite

Jenkins caches plugins aggressively. When uploading the same version number:
- Old .hpi file remains
- Expanded plugin directory isn't cleared
- Jenkins uses cached version

Solutions:
- Use SNAPSHOT versioning for development
- Use update script to properly clean old files
- Increment version number for each build (current approach)

## Features Added

### 1. Parameter Resolution for Pipeline SCM
- Resolves `${BRANCH}` and other parameters BEFORE fetching Jenkinsfile
- Works in "Pipeline script from SCM" configurations
- Uses job default parameters + last build parameters

### 2. Custom Workspace Names
- New field: "Workspace name" in both Pipeline and Freestyle configurations
- Allows multiple jobs to share same Plastic SCM workspace
- Supports parameter expansion (e.g., `${JOB_NAME}`)

### 3. Hot-reload Support
- Fixed `CmTool.onLoaded()` to handle hot-reload gracefully
- No longer throws exception during plugin upload

### 4. CM Tool Validation
- Validates that `cm` executable is available before running commands
- Runs `cm version` on first use to verify Plastic SCM is properly installed
- Provides clear error messages if cm is not found or cannot be executed
- Shows cm version in Jenkins build log

### 5. Enhanced Error Logging
- Fixed "The handle is invalid" error by:
  - Closing output stream after process finishes (moved consoleStream.close() after proc.join())
  - Capturing stderr in addition to stdout in tryExecute() method
- Added console logging for parameter resolution and file fetching
- Shows in job console:
  - Parameter values used
  - "Resolved Plastic SCM selector" confirmation
  - "Fetching Jenkinsfile from Plastic SCM: [path]"
  - "Successfully retrieved Jenkinsfile from Plastic SCM"

## Debugging

### Enable Debug Logging in Jenkins
1. Manage Jenkins → System Log → Add new log recorder
2. Name: `PlasticSCM Debug`
3. Add logger: `com.codicesoftware.plugins.jenkins.PlasticSCMFile`
4. Set level: `ALL`
5. Save

### Debug Output Shows
- Original selector with `${BRANCH}`
- Resolved selector with actual branch name
- Parameters found and their values
- File being fetched
- RepObjectSpec being used

## Common Issues

### Issue: Descriptor is null during hot-reload
**Fixed**: Version 4.5.001+ handles this gracefully

### Issue: Wrong JDK used
**Solution**: Always set JAVA_HOME to JDK 17 before building
```bash
export JAVA_HOME="/c/Users/David/scoop/apps/openjdk17/current"
```

### Issue: Jenkins cache corruption
**Solution**: Stop Jenkins, delete `JENKINS_HOME/war/` and `JENKINS_HOME/work/`, restart

## Files Modified

### Core Changes
- `PlasticSCMFile.java` - Parameter resolution for Jenkinsfile fetch
- `PlasticSCM.java` - Custom workspace name field
- `PlasticSCMStep.java` - Custom workspace name for pipeline
- `WorkspaceManager.java` - Accept custom workspace name
- `CmTool.java` - Hot-reload fix
- `PlasticTool.java` - CM tool validation and enhanced error logging

### UI Changes
- `PlasticSCM/config.jelly` - Added workspace name field
- `PlasticSCMStep/config.jelly` - Added workspace name field
- `workspaceName.html` - Help documentation

### Build Configuration
- `.vscode/tasks.json` - VS Code build task
- `build.sh` - Shell build script
- `update-plugin.sh` - Plugin update script for Jenkins
- `pom.xml` - Version tracking
