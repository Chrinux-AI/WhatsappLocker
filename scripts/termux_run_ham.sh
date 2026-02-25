#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_URL="${1:-https://github.com/YOUR_USERNAME/WhatsappLocker.git}"
PROJECT_DIR="${2:-WhatsappLocker}"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "[RUN HAM 1/6] Update and install tools..."
pkg update -y
pkg install -y git openjdk-17 android-tools

export JAVA_HOME="/data/data/com.termux/files/usr/lib/jvm/openjdk-17"
export PATH="$JAVA_HOME/bin:$PATH"

echo "[RUN HAM 2/6] Clone repo..."
rm -rf "$PROJECT_DIR"
git clone "$REPO_URL" "$PROJECT_DIR"
cd "$PROJECT_DIR"

echo "[RUN HAM 3/6] Build APK..."
chmod +x gradlew scripts/termux_build_and_install.sh
./scripts/termux_build_and_install.sh

echo "[RUN HAM 4/6] Copy APK to Downloads..."
termux-setup-storage >/dev/null 2>&1 || true
if [[ -f "$APK_PATH" && -d "/sdcard/Download" ]]; then
  cp -f "$APK_PATH" /sdcard/Download/WhatsappBusinessLocker-debug.apk
  echo "APK copied -> /sdcard/Download/WhatsappBusinessLocker-debug.apk"
fi

echo "[RUN HAM 5/6] Install options"
echo "Option A (tap install): /sdcard/Download/WhatsappBusinessLocker-debug.apk"
if command -v adb >/dev/null 2>&1; then
  echo "Option B (adb): adb install -r $APK_PATH"
fi

echo "[RUN HAM 6/6] Done."
