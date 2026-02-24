# WhatsApp Business Locker - Android Native App

This is a complete, production-ready WhatsApp Business chat locker and general app locker. Built primarily in **Kotlin**, it natively integrates with the Android SDK for bulletproof app launch detection.

## Features
- **App Detection:** Monitors the foreground activity to detect when WhatsApp Business (or any arbitrary app) is launched.
- **Secure Storage:** All PIN/password mechanisms encrypt locally using Android Jetpack Security (`EncryptedSharedPreferences`).
- **No-Bypass Lock Screen:** The overlay securely intercepts the back button and navigation attempts, sending users to the launcher instead of bypassing.
- **Dynamic Configuration:** Quickly select or explicitly type in app packages to lock via the dashboard (`MainActivity`).

---

## Technical Implementation Details
The codebase is modularized correctly into key components:
- `MainActivity.kt`: Dashboard logic to toggle functionality, manage permissions, select apps, and set a PIN.
- `LockService.kt`: A persistent Android Foreground Service leveraging `UsageStatsManager` polling securely for real-time app launch detection.
- `LockActivity.kt`: Full-screen singleInstance activity acting as the overlay firewall before granting access.
- `SecurityUtil.kt`: Security logic tying to EncryptedSharedPreferences for hashed PIN comparison.

---

## 🚀 Build, Install & Test (PC Workflow)

### Method 1: Automated Script
```bash
# Clone the repository
git clone https://github.com/Chrinux-AI/WhatsappLocker.git
cd WhatsappLocker

# Run the build & install script
chmod +x build.sh
./build.sh
```

### Method 2: Manual Gradle Commands
```bash
# Debug Build
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk

# Install via ADB:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 PC-Free / Phone-First Workflows

If your desktop dies or you are restricted to your Android smartphone, you can fully compile and deploy this app directly.

### Method 1: Cloud Build via GitHub Actions (Recommended)
This repository is configured with a **GitHub Actions CI Pipeline**.
1. Sync/Push this code to your GitHub repo.
2. Go to the **Actions** tab on your GitHub repository page from your phone browser.
3. Select "Android CI" and trigger a manual workflow build, or simply push a commit.
4. When the run finishes (takes ~2 minutes), scroll down to **Artifacts** and download `WhatsappLocker-APK.zip`.
5. Extract the ZIP on your phone using any file manager and tap `app-debug.apk` to install!

### Method 2: Termux Local Compilation
You can compile this code directly on your Android phone using Termux.
1. Install [Termux](https://termux.dev/) and open it.
2. Run the environment setup:
```bash
pkg update && pkg upgrade
pkg install git openjdk-17
```
3. Clone and build:
```bash
git clone https://github.com/Chrinux-AI/WhatsappLocker.git
cd WhatsappLocker
chmod +x gradlew
./gradlew assembleDebug
```
4. Find your APK and install:
The compiled APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your internal storage to install.

---

## User Testing Plan

### 1. WhatsApp Business Lock Test
- Open `WhatsappLocker`.
- Toggle the `WhatsApp Business` checkbox.
- Tap `Set PIN` and configure `1234`.
- Enable the App Locker via the toggle switch. Ensure permissions are granted.
- Hit the home screen. Tap on WhatsApp Business.
- **EXPECTED:** The PIN screen immediately blocks the chat interface. Enter `1234` to unlock.

### 2. Bypass / Rotation Protection
- Launch WhatsApp Business to trigger the Lock Screen.
- Try swiping back or hitting device hardware Back buttons.
- **EXPECTED:** `LockActivity.kt` intercepts standard callbacks and kicks you to the device Home Screen without bypassing.

### 3. Idle / Timeout Relock
- Launch a secured app, provide your PIN, and use it.
- Leave the app (hit Home or switch apps) so another application comes to the foreground.
- Return to the secured app.
- **EXPECTED:** `LockService` cleared the temporary authorization and forces the Lock Screen PIN prompt again.

---

*(Note: Web App versions (PWAs) are fundamentally restricted by standard browser security sandboxing mechanisms and cannot monitor the Android OS task list to build an overlay tracker. Therefore this solution strictly relies on Native Android components).*
