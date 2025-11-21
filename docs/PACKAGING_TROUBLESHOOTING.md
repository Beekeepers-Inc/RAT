# Packaging and Distribution Troubleshooting

## Application Starts and Immediately Closes

### Symptom
After installing from DMG/MSI, the application launches briefly and then closes immediately without showing any window.

### Root Causes

#### 1. Missing Java Modules (Most Common)

**Error**: `java.lang.NoClassDefFoundError: java/sql/Driver`

**Cause**: jpackage creates a minimal JRE that excludes the `java.sql` module by default.

**Solution**: Explicitly include required modules in `build.gradle.kts`:
```kotlin
modules(
    "java.sql",           // Required for JDBC (DuckDB)
    "java.naming",        // Required for JDBC DataSource
    "java.desktop",       // Required for Compose UI
    "java.prefs",         // Required for preferences
    "jdk.unsupported"     // Required for some native libraries
)
```

#### 2. Missing Native Libraries

**Error**: `UnsatisfiedLinkError` or `dyld: Library not loaded`

**Cause**: The **DuckDB native library** (DLL on Windows, dylib on macOS) fails to load when the application is packaged. DuckDB JDBC requires platform-specific native libraries that must be properly extracted at runtime.

### Solution Applied

#### 1. Build Configuration (`build.gradle.kts`)

Added the following to `nativeDistributions` block:

```kotlin
// Include all dependencies and their native libraries
includeAllModules = true

windows {
    menuGroup = "RATS"
    upgradeUuid = "61DAB35E-17CB-43B4-B698-C1A92CAB0D2B"
    // Enable console for error logging
    console = true
    // Allow user to choose install directory (needed for temp files)
    dirChooser = true
}

// JVM arguments to help with native library loading
jvmArgs += listOf(
    "-Djava.io.tmpdir=\${APPDIR}/temp",
    "-Dorg.duckdb.tmp.dir=\${APPDIR}/temp"
)
```

**What this does:**
- `includeAllModules = true` - Ensures all JAR dependencies and their native libraries are included
- `console = true` - Shows console window on Windows to see error messages
- `dirChooser = true` - Allows user to choose install location (writable directory)
- JVM args - Configures temp directories for DuckDB native library extraction

#### 2. Error Handling (`DuckDBCore.kt`)

Added try-catch blocks to catch native library loading errors:

```kotlin
init {
    try {
        Class.forName("org.duckdb.DuckDBDriver")
        connection = DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection
        // ... configuration
    } catch (e: UnsatisfiedLinkError) {
        System.err.println("Failed to load DuckDB native library: ${e.message}")
        throw RuntimeException("Failed to load DuckDB native library.", e)
    }
}
```

#### 3. User-Facing Error Dialog (`Main.kt`)

Updated main application to catch initialization errors and show them:

```kotlin
var initError by remember { mutableStateOf<String?>(null) }
val appState = remember {
    try {
        AppState()
    } catch (e: Exception) {
        initError = "Failed to initialize application:\n\n${e.message}"
        null
    }
}

// Show AlertDialog with error message if initialization fails
```

### Testing the Fix

After rebuilding the MSI/DMG:

**If the fix worked:**
- Application window opens and stays open
- Data import/analysis features work correctly

**If still failing:**
1. **Windows**: Look for console window with error messages (if `console = true`)
2. **Check error dialog** - It should now show the actual error instead of silently crashing
3. **Common errors:**
   - "UnsatisfiedLinkError" - Native library still not loading
   - "Cannot write to directory" - Permissions issue
   - "File not found" - Missing dependency

### Manual Verification

To verify native libraries are packaged:

**Windows MSI:**
```powershell
# After installation, check app directory
cd "C:\Program Files\RATS\app"
dir /s *.dll
# Should see duckdb_jdbc.dll or similar
```

**macOS DMG:**
```bash
# Inside the .app bundle
cd /Applications/RATS.app/Contents/runtime/Contents/Home/lib
ls -la *.dylib
# Should see libduckdb.dylib or similar
```

### Alternative Solutions

If the above doesn't work:

#### Option 1: Prebundle Native Library

Extract and bundle the DuckDB native library explicitly:

```kotlin
// In build.gradle.kts
nativeDistributions {
    // ...

    // Copy DuckDB native library to app resources
    fromFiles(
        project.file("libs/duckdb_jdbc.dll"),  // Windows
        project.file("libs/libduckdb.dylib")   // macOS
    )
}
```

Then extract it manually in code before loading:
```kotlin
val nativeLib = javaClass.getResourceAsStream("/duckdb_jdbc.dll")
// Write to temp file and load via System.load()
```

#### Option 2: Use Exploded JAR Format

Configure jpackage to use exploded format instead of embedded resources:

```kotlin
windows {
    // ...
    // Force exploded JAR format
    exeArgs += "--win-dir-chooser"
}
```

#### Option 3: Set Explicit Library Path

Set `java.library.path` in JVM arguments:

```kotlin
jvmArgs += listOf(
    "-Djava.library.path=\${APPDIR}/lib",
    "-Djna.library.path=\${APPDIR}/lib"
)
```

## Debugging Packaged Applications

### Enable Logging

**Windows:**
Set `console = true` in `build.gradle.kts` to see console output

**macOS:**
Run from Terminal to see stdout/stderr:
```bash
/Applications/RATS.app/Contents/MacOS/RATS
```

**Both Platforms:**
Add logging to file:
```kotlin
jvmArgs += listOf(
    "-Dlog.file=\${user.home}/rats.log",
    "-verbose:jni"  // Verbose JNI/native library loading
)
```

### Check System Requirements

Ensure target system has:
- **Java 17+ JRE** (bundled with jpackage, but verify)
- **Write permissions** to install directory
- **Temp directory** accessible
- **No antivirus blocking** native library extraction

### Test in Isolated Environment

Test the packaged application on a clean Windows/macOS machine without development tools to catch missing dependencies.

## Related Issues

- **"ClassNotFoundException"** - Missing JAR in package
- **"NoSuchMethodError"** - Dependency version mismatch
- **"AccessDeniedException"** - Permissions issue on install/temp directory
- **Silent crash** - Check Windows Event Viewer or macOS Console.app

## References

- [Compose Desktop Packaging Docs](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution)
- [DuckDB JDBC Documentation](https://duckdb.org/docs/api/java)
- [jpackage Documentation](https://docs.oracle.com/en/java/javase/17/jpackage/)
