# CI/CD Pipeline Documentation

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline for building and distributing RATS desktop application installers.

## Overview

The CI/CD pipeline uses GitHub Actions to automatically build native installers for:
- **macOS**: DMG installer package
- **Windows**: MSI installer package

## Pipeline Triggers

The pipeline runs automatically on:

1. **Push to main branch**: Builds and tests the application
2. **Pull requests to main**: Validates changes before merging
3. **Git tags (v*)**: Creates a GitHub Release with downloadable installers
4. **Manual dispatch**: On-demand builds via GitHub Actions UI

## Workflow Files

### `.github/workflows/build-installers.yml`

Main workflow that handles building installers for both platforms.

## Pipeline Jobs

### 1. Build macOS Installer (`build-macos`)

**Runs on**: `macos-latest` (macOS 14 Sonoma)

**Steps**:
1. Checkout source code
2. Set up JDK 17 (Temurin distribution)
3. Cache Gradle dependencies for faster builds
4. Build the project (`./gradlew build`)
5. Run tests (`./gradlew test`)
6. Create DMG package (`./gradlew packageDmg`)
7. Upload DMG as artifact

**Output**: `RATS-macOS-DMG` artifact containing the `.dmg` file

### 2. Build Windows Installer (`build-windows`)

**Runs on**: `windows-latest` (Windows Server 2022)

**Prerequisites**:
- WiX Toolset (installed automatically via `dotnet tool install --global wix`)
- JDK 17 with jpackage tool
- MSI creation requires WiX for jpackage

**Steps**:
1. Checkout source code
2. Set up JDK 17 (Temurin distribution)
3. Cache Gradle dependencies
4. Verify Gradle wrapper JAR exists
5. Install WiX Toolset (required for MSI packaging)
6. Verify WiX installation and locate executables
7. List available Gradle packaging tasks
8. Build the project (`./gradlew build`)
9. Run tests (`./gradlew test`)
10. Create MSI package (`./gradlew packageMsi --stacktrace --info`)
11. Search for generated MSI files in build directory
12. Upload MSI as artifact

**Output**: `RATS-Windows-MSI` artifact containing the `.msi` file

### 3. Create GitHub Release (`create-release`)

**Runs on**: `ubuntu-latest`
**Condition**: Only runs when a version tag is pushed (e.g., `v1.0.0`)

**Steps**:
1. Download macOS DMG artifact
2. Download Windows MSI artifact
3. Create GitHub Release with both installers
4. Auto-generate release notes from commits

## Creating a Release

To create a new release with installers:

```bash
# 1. Update version in build.gradle.kts
# Edit: packageVersion = "1.1.0"

# 2. Commit the change
git add build.gradle.kts
git commit -m "Bump version to 1.1.0"

# 3. Create and push a tag
git tag v1.1.0
git push origin main
git push origin v1.1.0
```

The pipeline will automatically:
- Build installers for both platforms
- Create a GitHub Release at `https://github.com/<owner>/<repo>/releases`
- Attach DMG and MSI files to the release
- Generate release notes from commits

## Artifact Retention

- Build artifacts are retained for **30 days**
- Artifacts from tagged releases are permanently available in GitHub Releases
- Each artifact includes the platform-specific installer

## Accessing Build Artifacts

### From Pull Requests/Commits

1. Go to the GitHub repository
2. Click on **Actions** tab
3. Select the workflow run
4. Scroll to **Artifacts** section
5. Download `RATS-macOS-DMG` or `RATS-Windows-MSI`

### From Releases

1. Go to **Releases** page on GitHub
2. Find the desired version
3. Download the installer under **Assets**

## Build Environment

### Java Configuration

- **Version**: JDK 17 (LTS)
- **Distribution**: Eclipse Temurin (AdoptOpenJDK successor)
- **Reason**: Compose Desktop requires JDK 17+ for modern features

### Gradle Configuration

- Uses Gradle Wrapper for consistent builds
- Caching enabled for faster subsequent builds
- `--no-daemon` flag used for CI stability

### Windows MSI Requirements

The `build.gradle.kts` includes Windows-specific configuration for MSI generation:

```kotlin
windows {
    menuGroup = "RATS"
    upgradeUuid = "61DAB35E-17CB-43B4-B698-C1A92CAB0D2B"
}
```

**Required fields**:
- `packageName`: Application name (set at `nativeDistributions` level)
- `packageVersion`: Version number (set at `nativeDistributions` level)
- `description`: Application description (used in MSI metadata)
- `vendor`: Publisher name (required for MSI)
- `menuGroup`: Start Menu folder location
- `upgradeUuid`: Unique GUID for installer upgrades (keeps same ID across versions)

**Note**: The `upgradeUuid` should remain constant across versions to allow proper upgrades. Generate a new UUID only for a completely different application.

## Caching Strategy

The pipeline caches:
- Gradle wrapper binaries
- Dependency caches (`~/.gradle/caches`)
- Downloaded libraries

Cache key is based on:
- Operating system
- Hash of `*.gradle*` and `gradle-wrapper.properties` files

This typically reduces build time by 2-3 minutes.

## Code Signing (Production Setup)

For production releases, you should implement code signing:

### macOS Code Signing

1. **Obtain Apple Developer Certificate**:
   - Enroll in Apple Developer Program ($99/year)
   - Create "Developer ID Application" certificate

2. **Add secrets to GitHub**:
   ```
   MACOS_CERTIFICATE: Base64-encoded .p12 certificate
   MACOS_CERTIFICATE_PASSWORD: Certificate password
   APPLE_ID: Your Apple ID email
   APPLE_ID_PASSWORD: App-specific password
   APPLE_TEAM_ID: Team ID from developer portal
   ```

3. **Update build.gradle.kts**:
   ```kotlin
   macOS {
       bundleID = "com.rats.desktop"
       signing {
           sign.set(true)
           identity.set("Developer ID Application: Your Name (TEAM_ID)")
       }
       notarization {
           appleID.set(System.getenv("APPLE_ID"))
           password.set(System.getenv("APPLE_ID_PASSWORD"))
           teamID.set(System.getenv("APPLE_TEAM_ID"))
       }
   }
   ```

### Windows Code Signing

1. **Obtain Code Signing Certificate**:
   - Purchase from certificate authority (DigiCert, Sectigo, etc.)
   - EV certificates provide better SmartScreen reputation

2. **Add secrets to GitHub**:
   ```
   WINDOWS_CERTIFICATE: Base64-encoded .pfx certificate
   WINDOWS_CERTIFICATE_PASSWORD: Certificate password
   ```

3. **Add signing step to workflow**:
   ```yaml
   - name: Sign Windows MSI
     run: |
       echo "${{ secrets.WINDOWS_CERTIFICATE }}" | base64 --decode > certificate.pfx
       # Use signtool or jsign to sign the MSI
   ```

## Troubleshooting

### Common Issues

**Build fails on macOS**:
- Check that `compose.desktop.currentOs` is properly configured
- Ensure Java version matches (17+)
- Verify Gradle wrapper has execute permissions

**Windows MSI packaging fails**:
- WiX Toolset is installed via `dotnet tool install --global wix`
- If WiX installation fails, MSI packaging will fail
- Verify WiX is in PATH: `Get-Command wix.exe`
- Check actual MSI output location in build logs
- MSI may be in `build/compose/binaries/main-release/msi/` instead of `main/msi/`
- jpackage (used by Compose Desktop) requires WiX for MSI creation
- Check Gradle output for jpackage errors
- Ensure `windows {}` block is configured in `build.gradle.kts` with:
  - `menuGroup`: Start menu folder name
  - `upgradeUuid`: Unique identifier for MSI upgrades
  - `vendor` and `description` are required for MSI metadata

**Artifacts not found**:
- Verify build output path matches artifact upload path
- Check `if-no-files-found: error` catches missing files

**Tests fail in CI but pass locally**:
- CI runs with `--no-daemon` for isolation
- Check for environment-specific dependencies
- Verify no hardcoded paths exist

### Debugging

1. **Download build logs**:
   - Go to failed workflow run
   - Scroll to **Artifacts** section
   - Download `msi-build-log` (Windows builds only)
   - Search for "error", "fail", or "jpackage" to identify issues

2. Add verbose output:
   ```yaml
   - name: Build with debug
     run: ./gradlew build --info --stacktrace
   ```

3. Check runner environment:
   ```yaml
   - name: Environment info
     run: |
       java -version
       ./gradlew --version
       echo "OS: $RUNNER_OS"
   ```

4. **Common MSI build issues**:
   - Check if `jpackage` is available: `jpackage --version`
   - Verify WiX installation in build log
   - Look for "Cannot find WiX" or "jpackage failed" messages
   - Ensure `build/compose` directory is being created

## Security Considerations

1. **Secrets Management**:
   - Store signing certificates as GitHub Secrets
   - Never commit certificates to repository
   - Use environment variables for sensitive data

2. **Dependency Security**:
   - Gradle caches are isolated per repository
   - Dependencies fetched from official Maven repositories
   - Consider adding dependency scanning (e.g., Dependabot)

3. **Artifact Security**:
   - Artifacts are stored in GitHub's secure infrastructure
   - Access controlled by repository permissions
   - Consider adding checksums for release artifacts

## Cost Considerations

GitHub Actions provides:
- **2,000 minutes/month** for free (private repos)
- **Unlimited minutes** for public repos
- macOS runners consume 10x minutes (most expensive)
- Windows runners consume 2x minutes

**Typical build times**:
- macOS: ~8-12 minutes
- Windows: ~6-10 minutes

## Future Enhancements

1. **Add Linux builds**:
   ```yaml
   build-linux:
     runs-on: ubuntu-latest
     # Add steps for packageDeb
   ```

2. **Automated version bumping**:
   - Use semantic-release or similar tools
   - Auto-increment version based on commits

3. **Beta/Nightly builds**:
   - Scheduled builds for continuous testing
   - Separate release channel for beta testers

4. **Performance testing**:
   - Add benchmarks for large file imports
   - Track performance across releases

5. **Multi-architecture support**:
   - Apple Silicon (arm64) native builds
   - Windows ARM64 support

## Related Documentation

- [README.md](../README.md) - Project overview
- [CLAUDE.md](../CLAUDE.md) - Development guidelines
- [Compose Desktop Packaging](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
