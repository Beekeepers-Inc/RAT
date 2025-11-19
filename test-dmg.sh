#!/bin/bash

# Script to test and debug the DMG package

set -e

echo "======================================"
echo "RATS DMG Testing Script"
echo "======================================"
echo ""

# Find the DMG file
DMG_FILE=$(find build/compose/binaries/main/dmg -name "*.dmg" 2>/dev/null | head -1)

if [ -z "$DMG_FILE" ]; then
    echo "ERROR: DMG file not found in build/compose/binaries/main/dmg"
    echo "Run './gradlew packageDmg' first"
    exit 1
fi

echo "Found DMG: $DMG_FILE"
echo ""

# Mount the DMG
echo "Mounting DMG..."
hdiutil attach "$DMG_FILE" -mountpoint /tmp/rats_dmg

# Copy app to /tmp for testing
echo "Copying app to /tmp for testing..."
rm -rf /tmp/RATS.app
cp -R /tmp/rats_dmg/RATS.app /tmp/RATS.app

# Unmount DMG
hdiutil detach /tmp/rats_dmg

echo ""
echo "======================================"
echo "Checking app structure..."
echo "======================================"

# Check if runtime is bundled
if [ -d "/tmp/RATS.app/Contents/runtime" ]; then
    echo "✓ Java runtime is bundled"
    /tmp/RATS.app/Contents/runtime/Contents/Home/bin/java -version
else
    echo "✗ Java runtime NOT found"
fi

echo ""

# Check for native libraries
echo "Searching for DuckDB native libraries..."
DYLIBS=$(find /tmp/RATS.app -name "*.dylib" -o -name "*.jnilib" 2>/dev/null)
if [ -z "$DYLIBS" ]; then
    echo "✗ No dylib files found!"
else
    echo "✓ Found dylib files:"
    echo "$DYLIBS"
fi

echo ""

# Check for DuckDB JAR
echo "Searching for DuckDB JAR..."
DUCKDB_JAR=$(find /tmp/RATS.app -name "duckdb*.jar" 2>/dev/null)
if [ -z "$DUCKDB_JAR" ]; then
    echo "✗ DuckDB JAR not found!"
else
    echo "✓ Found DuckDB JAR: $DUCKDB_JAR"
fi

echo ""
echo "======================================"
echo "Removing quarantine attribute..."
echo "======================================"
xattr -rd com.apple.quarantine /tmp/RATS.app 2>/dev/null || echo "No quarantine attribute to remove"

echo ""
echo "======================================"
echo "Running app from Terminal..."
echo "======================================"
echo "This will show any error messages:"
echo ""

# Run the app
/tmp/RATS.app/Contents/MacOS/RATS

echo ""
echo "======================================"
echo "App exited"
echo "======================================"
