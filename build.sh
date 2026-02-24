#!/bin/bash

# Ensure we exit on failure
set -e

echo "======================================"
echo " Building WhatsApp Locker Android App "
echo "======================================"

# Check if gradlew exists, else use globally installed gradle
if [ -f "./gradlew" ]; then
    echo "Using local gradle wrapper..."
    chmod +x ./gradlew
    ./gradlew assembleDebug
else
    echo "Using system gradle..."
    gradle assembleDebug
fi

echo ""
echo "======================================"
echo " Installing APK on connected device   "
echo "======================================"

if command -v adb &> /dev/null; then
    # Try installing the APK
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        adb install -r "$APK_PATH"
        echo "Installation successful!"

        echo "Launching the app..."
        adb shell am start -n com.example.whatsapplocker/.MainActivity
    else
        echo "Error: APK not found at $APK_PATH"
    fi
else
    echo "Notice: adb is not installed or not in PATH. Please install manually to your device."
    echo "Compiled APK is at: app/build/outputs/apk/debug/app-debug.apk"
fi
