# WhatsApp Business Locker (Android)

This repo is focused on **WhatsApp Business locking first** (`com.whatsapp.w4b`) with optional additional app locking.

## What you get
- WhatsApp Business launch detection + lock screen challenge.
- PIN / Password / Pattern credential modes.
- Timeout-based temporary unlock.
- Encrypted credential storage (Keystore + EncryptedSharedPreferences).
- Debug + release APK build workflow.
- Web-download friendly APK list (`web-apk/apk-list.json`) generated automatically.

---

## Termux “run am” (clone + build + install flow)

If you want one command style for Termux, run this exactly:

```bash
pkg update -y && pkg install -y git

git clone https://github.com/YOUR_USERNAME/WhatsappLocker.git
cd WhatsappLocker
chmod +x scripts/termux_run_ham.sh
./scripts/termux_run_ham.sh https://github.com/YOUR_USERNAME/WhatsappLocker.git WhatsappLocker
```

What it does:
1. Installs Termux dependencies.
2. Clones your GitHub repo.
3. Builds debug + release APK.
4. Copies debug APK to `/sdcard/Download/WhatsappBusinessLocker-debug.apk`.
5. Gives direct install options (tap APK or adb install).

---

## Build APK (PC)

### One command (recommended)
```bash
./build.sh            # builds debug + release + apk list
./build.sh debug      # debug only
./build.sh release    # release only
```

### Manual Gradle
```bash
chmod +x gradlew
./gradlew assembleDebug assembleRelease
./scripts/generate_apk_list.sh
```

APK files:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `web-apk/apk-list.json`

---

## Install from web (APK form)

1. Push code to your fork on GitHub.
2. Open **Actions → Android CI**.
3. Run workflow (or push commit).
4. Download artifacts:
   - `WhatsappBusinessLocker-debug-apk`
   - `WhatsappBusinessLocker-release-unsigned-apk`
   - `WhatsappBusinessLocker-web-apk-list`
5. Transfer APK to Android and tap install.

The `web-apk` artifact includes:
- `index.html` (human-readable page)
- `apk-list.json` (machine-readable list of APK outputs)

---

## ADB install commands

### USB
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Wireless
```bash
adb tcpip 5555
adb connect <PHONE_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## WhatsApp Business setup checklist

1. Install WhatsApp Business on your phone.
2. Open WhatsApp Business Locker.
3. Grant Usage Access and Overlay permissions.
4. Set lock credential and timeout.
5. Enable locker.
6. Launch WhatsApp Business and verify lock screen appears.

---

## Push to your GitHub fork

```bash
git checkout -b feat/termux-run-ham
git remote set-url origin https://github.com/YOUR_USERNAME/WhatsappLocker.git
git push -u origin feat/termux-run-ham
```

Use GitHub PAT for HTTPS authentication, or change remote to SSH.
