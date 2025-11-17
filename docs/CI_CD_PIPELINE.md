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

**Steps**:
1. Checkout source code
2. Set up JDK 17 (Temurin distribution)
3. Cache Gradle dependencies
4. Build the project (`./gradlew build`)
5. Run tests (`./gradlew test`)
6. Create MSI package (`./gradlew packageMsi`)
7. Upload MSI as artifact

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
- WiX Toolset is pre-installed on GitHub Windows runners
- Check Windows-specific path separators in build scripts

**Artifacts not found**:
- Verify build output path matches artifact upload path
- Check `if-no-files-found: error` catches missing files

**Tests fail in CI but pass locally**:
- CI runs with `--no-daemon` for isolation
- Check for environment-specific dependencies
- Verify no hardcoded paths exist

### Debugging

1. Add verbose output:
   ```yaml
   - name: Build with debug
     run: ./gradlew build --info --stacktrace
   ```

2. Check runner environment:
   ```yaml
   - name: Environment info
     run: |
       java -version
       ./gradlew --version
       echo "OS: $RUNNER_OS"
   ```

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
