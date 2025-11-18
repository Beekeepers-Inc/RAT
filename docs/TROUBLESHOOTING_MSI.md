# Windows MSI Build Troubleshooting Guide

This guide helps diagnose why the Windows MSI installer is not being generated in CI/CD.

## Quick Diagnosis Steps

### 1. Check Build Log Artifact

After a failed build:
1. Go to the **Actions** tab in GitHub
2. Click on the failed workflow run
3. Scroll to **Artifacts** section
4. Download `msi-build-log`
5. Open the text file and search for:
   - `error` or `ERROR`
   - `fail` or `FAIL`
   - `exception` or `Exception`
   - `jpackage`
   - `Cannot find`

### 2. Common Issues and Solutions

#### Issue: "jpackage not found"

**Cause**: JDK installation doesn't include jpackage tool

**Solution**:
- Verify JDK 17+ is being used (check "Set up JDK" step)
- jpackage is included in JDK 16+ as a standard tool
- Check if `jpackage --version` appears in build log

#### Issue: "Cannot find WiX" or "WiX Toolset not found"

**Cause**: WiX isn't installed or not in PATH

**Solution**:
- Check "Install WiX Toolset" step succeeded
- Verify `wix.exe` is found in "Verify WiX Toolset" step
- WiX is required for jpackage to create MSI files on Windows

**Manual fix**:
```yaml
- name: Install WiX Toolset
  run: |
    dotnet tool install --global wix
    $env:PATH += ";$env:USERPROFILE\.dotnet\tools"
```

#### Issue: "build/compose directory not found"

**Cause**: Compose Desktop packaging task not running

**Solution**:
- Check if `packageMsi` task exists: `./gradlew tasks --all`
- Verify `build.gradle.kts` has:
  ```kotlin
  targetFormats(TargetFormat.Msi)
  ```
- Ensure Compose Desktop plugin version is compatible

#### Issue: Task runs but no output

**Cause**: Missing required metadata in build configuration

**Solution**: Ensure `build.gradle.kts` has all required fields:
```kotlin
nativeDistributions {
    targetFormats(TargetFormat.Msi)
    packageName = "RATS"
    packageVersion = "1.0.0"
    description = "Desktop data analysis application"  // Required
    vendor = "RATS"                                     // Required

    windows {
        menuGroup = "RATS"
        upgradeUuid = "UNIQUE-UUID-HERE"
    }
}
```

#### Issue: "Cannot run program... error=2, No such file or directory"

**Cause**: jpackage trying to execute a tool that doesn't exist

**Solution**:
- This often means WiX tools (candle.exe, light.exe) aren't in PATH
- Verify WiX installation completed successfully
- Check if running on correct OS (windows-latest)

### 3. Verify Build Configuration

Check these files for correct configuration:

**build.gradle.kts**:
```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

compose.desktop {
    application {
        mainClass = "com.rats.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "RATS"
            packageVersion = "1.0.0"
            description = "Desktop data analysis application"
            vendor = "RATS"

            windows {
                menuGroup = "RATS"
                upgradeUuid = "61DAB35E-17CB-43B4-B698-C1A92CAB0D2B"
            }
        }
    }
}
```

**Workflow file** (`.github/workflows/build-installers.yml`):
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'

- name: Install WiX Toolset
  run: dotnet tool install --global wix
```

### 4. Test Locally on Windows

To test MSI generation locally:

```powershell
# Install WiX
dotnet tool install --global wix

# Verify jpackage
jpackage --version

# Build MSI
./gradlew packageMsi

# Check output
dir build/compose/binaries/main/msi
```

### 5. Check GitHub Actions Environment

Expected environment in workflow:
- **Runner**: `windows-latest` (Windows Server 2022)
- **JDK**: 17 (Temurin distribution)
- **WiX**: Installed via dotnet tool
- **Gradle**: Using wrapper (7.x or 8.x)

### 6. Review Full Build Output

Look for these indicators in the build log:

**Success indicators**:
```
> Task :packageMsi
Building application image
Building MSI package
Successfully built MSI installer
```

**Failure indicators**:
```
Cannot find WiX tools
jpackage failed with error
No tool provider found
Task :packageMsi FAILED
```

### 7. Enable Debug Logging

For more detailed output, modify the workflow:

```yaml
- name: Package Windows MSI (debug)
  run: ./gradlew packageMsi --no-daemon --info --stacktrace --debug
```

**Warning**: `--debug` produces very verbose output (10,000+ lines)

## Still Having Issues?

If none of the above solutions work:

1. **Check Compose Desktop version compatibility**:
   - Some versions have issues with MSI generation
   - Try updating to latest stable: `id("org.jetbrains.compose") version "1.5.11"`

2. **Verify Windows runner has required tools**:
   - GitHub's `windows-latest` should have .NET SDK pre-installed
   - WiX should install without issues via `dotnet tool`

3. **Check if issue is specific to CI**:
   - Test locally on Windows machine
   - If local build works but CI fails, it's an environment issue

4. **Review Compose Desktop documentation**:
   - [Packaging](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution)
   - [Known Issues](https://github.com/JetBrains/compose-multiplatform/issues)

## Reference Commands

```powershell
# Check JDK installation
java -version
jpackage --version

# Check WiX installation
wix --version
Get-Command wix.exe

# List Gradle tasks
./gradlew tasks --all | Select-String "package"

# Build with full output
./gradlew packageMsi --info --stacktrace 2>&1 | Tee-Object msi-build.log

# Search build log
Select-String -Path msi-build.log -Pattern "error|jpackage" -CaseSensitive:$false
```
