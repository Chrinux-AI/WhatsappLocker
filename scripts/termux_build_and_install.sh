#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

APK_DEBUG="app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="app/build/outputs/apk/release/app-release-unsigned.apk"

echo "[1/5] Installing dependencies in Termux..."
pkg update -y
pkg install -y git openjdk-17 android-tools

export JAVA_HOME="/data/data/com.termux/files/usr/lib/jvm/openjdk-17"
export PATH="$JAVA_HOME/bin:$PATH"

echo "[2/5] Ensuring Gradle wrapper is executable..."
chmod +x gradlew


echo "[3/5] Building debug + release APK..."
./gradlew assembleDebug assembleRelease
./scripts/generate_apk_list.sh

echo "[4/5] APK outputs:"
echo "Debug:   ${APK_DEBUG}"
echo "Release: ${APK_RELEASE}"

termux-setup-storage >/dev/null 2>&1 || true
if [[ -d "/sdcard/Download" ]]; then
  cp -f "$APK_DEBUG" /sdcard/Download/WhatsappLocker-debug.apk || true
  cp -f "$APK_RELEASE" /sdcard/Download/WhatsappLocker-release-unsigned.apk || true
  echo "Copied APK(s) to /sdcard/Download for one-tap install."
fi

if command -v adb >/dev/null 2>&1; then
  echo "[5/5] Optional ADB install command:"
  echo "adb install -r ${APK_DEBUG}"
else
  echo "ADB unavailable in current Termux env; install APK manually from Downloads."
fi

echo "Done."
