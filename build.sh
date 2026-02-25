#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-all}"
case "$MODE" in
  debug)
    TASKS=(assembleDebug)
    ;;
  release)
    TASKS=(assembleRelease)
    ;;
  all)
    TASKS=(assembleDebug assembleRelease)
    ;;
  *)
    echo "Usage: ./build.sh [debug|release|all]"
    exit 1
    ;;
esac

JAVA_MAJOR=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
if [[ -z "${JAVA_MAJOR}" ]]; then
  JAVA_MAJOR=0
fi

if [[ ${JAVA_MAJOR} -gt 21 || ${JAVA_MAJOR} -lt 17 ]]; then
  echo "Detected unsupported Java runtime (${JAVA_MAJOR}). Bootstrapping JDK 17..."
  ./scripts/bootstrap_jdk17.sh
  export JAVA_HOME="$(cd .jdk17 && pwd)"
  export PATH="$JAVA_HOME/bin:$PATH"
  java -version
fi

echo "Building WhatsApp Business Locker with tasks: ${TASKS[*]}"
chmod +x ./gradlew
./gradlew "${TASKS[@]}"

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

echo "APK outputs:"
[[ -f "$DEBUG_APK" ]] && echo " - $DEBUG_APK"
[[ -f "$RELEASE_APK" ]] && echo " - $RELEASE_APK"

./scripts/generate_apk_list.sh

if command -v adb >/dev/null 2>&1; then
  if [[ -f "$DEBUG_APK" ]]; then
    echo "Installing debug via ADB..."
    adb install -r "$DEBUG_APK" || true
    adb shell monkey -p com.example.whatsapplocker -c android.intent.category.LAUNCHER 1 || true
  fi
else
  echo "ADB not found. Install manually from generated APK files."
fi
