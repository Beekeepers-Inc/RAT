# Installer Fix Summary

## Problem
DMG and MSI installers were crashing immediately on startup with error:
```
java.lang.NoClassDefFoundError: java/sql/Driver
```

## Root Cause
**jpackage creates a minimal JRE that excludes the `java.sql` module by default.**

Since RATS uses DuckDB (which requires JDBC), the `java.sql` module is essential but was not being included in the packaged runtime.

## Solution Applied

### Critical Fix: Added Java Modules Declaration

**File**: `build.gradle.kts` (lines 52-59)

```kotlin
// Explicitly include required Java modules for JDBC/SQL support
modules(
    "java.sql",           // Required for JDBC (DuckDB)
    "java.naming",        // Required for JDBC DataSource
    "java.desktop",       // Required for Compose UI
    "java.prefs",         // Required for preferences
    "jdk.unsupported"     // Required for some native libraries
)
```

### Additional Improvements

1. **macOS-specific configuration** (lines 61-67)
   - Added Info.plist configuration
   - Set minimum macOS version (10.15)
   - Enabled high-resolution support

2. **Windows-specific configuration** (lines 69-77)
   - Enabled console window for debugging
   - Allow user to choose install directory
   - Set writable directory for temp files

3. **JVM arguments** (lines 80-86)
   - Configure temp directories for DuckDB
   - Added placeholders for verbose logging (commented out)

4. **Enhanced error handling**
   - `Main.kt`: Catches initialization errors and shows dialog
   - `DuckDBCore.kt`: Better error messages for native library loading

## Testing

### Before Fix
```bash
/Applications/RATS.app/Contents/MacOS/RATS
# Result: Crash with NoClassDefFoundError
```

### After Fix
```bash
/Applications/RATS.app/Contents/MacOS/RATS
# Result: Application window opens successfully
```

## Files Modified

```
M  build.gradle.kts                           (Added modules configuration)
M  src/main/kotlin/com/rats/Main.kt          (Error dialog on init failure)
M  src/main/kotlin/com/rats/core/DuckDBCore.kt (Better error handling)
M  docs/CI_CD_PIPELINE.md                     (Documented modules requirement)
A  DEBUG_DMG.md                               (DMG debugging guide)
A  docs/PACKAGING_TROUBLESHOOTING.md          (General packaging troubleshooting)
A  test-dmg.sh                                (Automated DMG testing script)
```

## How to Verify Fix

1. **Rebuild installers:**
   ```bash
   ./gradlew clean
   ./gradlew packageDmg    # macOS
   ./gradlew packageMsi    # Windows
   ```

2. **Test DMG (macOS):**
   ```bash
   ./test-dmg.sh
   # Or manually:
   /Applications/RATS.app/Contents/MacOS/RATS
   ```

3. **Test MSI (Windows):**
   ```powershell
   # After installing from MSI
   "C:\Program Files\RATS\RATS.exe"
   ```

4. **Expected behavior:**
   - ✅ Application window opens
   - ✅ No errors in console
   - ✅ Can import and analyze data files

## Key Learnings

### jpackage Minimal Runtime
- jpackage creates a custom, minimal JRE to reduce installer size
- It automatically detects some modules but **not all**
- JDBC/SQL modules are **not** auto-detected
- **Always explicitly declare required modules** for production apps

### Required Modules for RATS
- `java.sql` - JDBC API (DuckDB uses this)
- `java.naming` - JNDI (required by JDBC drivers)
- `java.desktop` - Swing/AWT (required by Compose Desktop)
- `java.prefs` - Preferences API
- `jdk.unsupported` - sun.misc.Unsafe (used by some native libraries)

### Debugging Packaged Apps
- **macOS**: Run from Terminal to see stdout/stderr
- **Windows**: Set `console = true` in build config
- **Both**: Add error handling with user-visible dialogs
- **Check Console.app** (macOS) or Event Viewer (Windows) for crashes

## Related Issues

If you encounter other module-related errors:

```
ClassNotFoundException: javax.naming.*
→ Add "java.naming" module

NoClassDefFoundError: javax.swing.*
→ Add "java.desktop" module

NoClassDefFoundError: java.util.prefs.*
→ Add "java.prefs" module
```

## References

- [jpackage Module Detection](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [Java Platform Module System](https://www.oracle.com/corporate/features/understanding-java-9-modules.html)
- [Compose Desktop Packaging](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution)
