# Debugging DMG App Crashes

## Fixed: NoClassDefFoundError: java/sql/Driver

**Problem**: The error `java.lang.NoClassDefFoundError: java/sql/Driver` means the bundled JRE is missing the `java.sql` module.

**Solution**: Added explicit Java modules to `build.gradle.kts`:
```kotlin
modules(
    "java.sql",           // Required for JDBC (DuckDB)
    "java.naming",        // Required for JDBC DataSource
    "java.desktop",       // Required for Compose UI
    "java.prefs",         // Required for preferences
    "jdk.unsupported"     // Required for some native libraries
)
```

This tells jpackage to include SQL/JDBC support in the minimal JRE.

## Run from Terminal to See Errors

Instead of double-clicking the app (which hides errors), run it from Terminal:

```bash
# Open the DMG
open RATS-1.0.0.dmg

# Drag RATS.app to Applications, then:
/Applications/RATS.app/Contents/MacOS/RATS
```

This will show all error messages in the terminal.

## Common Errors and Solutions

### Error: "dyld: Library not loaded: libduckdb.dylib"

**Cause**: DuckDB native library not found

**Solution**: Check if the dylib is packaged:
```bash
# Check if native library is included
find /Applications/RATS.app -name "*.dylib" -o -name "*.jnilib"

# Expected output should include DuckDB library
```

### Error: "UnsatisfiedLinkError"

**Cause**: Native library exists but can't be loaded

**Solution**: Check library architecture:
```bash
# Check if library matches your Mac architecture
file /Applications/RATS.app/Contents/runtime/Contents/Home/lib/*.dylib

# Should show:
# - "Mach-O 64-bit dynamically linked shared library x86_64" (Intel Mac)
# - "Mach-O 64-bit dynamically linked shared library arm64" (Apple Silicon)
```

### Error: "App is damaged and can't be opened"

**Cause**: Gatekeeper blocking unsigned app

**Solution**:
```bash
# Remove quarantine attribute
sudo xattr -rd com.apple.quarantine /Applications/RATS.app

# Or, allow in System Preferences:
# System Preferences > Security & Privacy > General > "Open Anyway"
```

### Error: Nothing happens / Silent crash

**Cause**: Multiple possible issues

**Solution**: Check Console.app logs:
1. Open **Console.app** (in /Applications/Utilities/)
2. Click "Start" to start streaming logs
3. Run the RATS app
4. Filter logs by "RATS" to see crash reports

## Verify Package Contents

```bash
# List all contents
ls -R /Applications/RATS.app/Contents/

# Check Java runtime is bundled
ls /Applications/RATS.app/Contents/runtime/Contents/Home/bin/java

# Check app JAR
ls /Applications/RATS.app/Contents/app/

# Check for DuckDB JDBC JAR
find /Applications/RATS.app/Contents -name "duckdb*.jar"
```

## Test with Verbose JNI Logging

Create a launcher script:

```bash
#!/bin/bash
# Save as ~/run-rats-debug.sh

/Applications/RATS.app/Contents/MacOS/RATS \
  -verbose:jni \
  -Djava.library.path=/Applications/RATS.app/Contents/runtime/Contents/Home/lib \
  -Dorg.duckdb.lib.path=/tmp
```

Then run:
```bash
chmod +x ~/run-rats-debug.sh
~/run-rats-debug.sh
```

## Rebuild with Debug Flags

If none of the above works, rebuild the DMG with additional debug options.

In `build.gradle.kts`, add:

```kotlin
macOS {
    bundleID = "com.rats.desktop"

    // Add verbose logging
    jvmArgs += listOf(
        "-verbose:jni",
        "-Xlog:library=info"
    )
}
```

## Share Debug Info

If the issue persists, share:

1. **Error message from Terminal** when running from command line
2. **Console.app crash logs** (filter by "RATS")
3. **Output of these commands**:
   ```bash
   # System info
   sw_vers
   uname -m

   # App contents check
   find /Applications/RATS.app -name "*.dylib" -o -name "duckdb*"

   # Java runtime check
   /Applications/RATS.app/Contents/runtime/Contents/Home/bin/java -version
   ```

This will help identify the exact issue with the DMG package.
