#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="web-apk"
mkdir -p "$OUT_DIR"

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

debug_size=0
release_size=0
[[ -f "$DEBUG_APK" ]] && debug_size=$(wc -c < "$DEBUG_APK")
[[ -f "$RELEASE_APK" ]] && release_size=$(wc -c < "$RELEASE_APK")

cat > "$OUT_DIR/apk-list.json" <<JSON
{
  "appId": "com.example.whatsapplocker",
  "generatedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "apks": [
    {
      "name": "debug",
      "path": "$DEBUG_APK",
      "sizeBytes": $debug_size,
      "notes": "Best for quick install/testing"
    },
    {
      "name": "release-unsigned",
      "path": "$RELEASE_APK",
      "sizeBytes": $release_size,
      "notes": "For signing/distribution workflow"
    }
  ]
}
JSON

cat > "$OUT_DIR/index.html" <<HTML
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>WhatsApp Business Locker APK Downloads</title>
  <style>body{font-family:system-ui;max-width:760px;margin:2rem auto;padding:0 1rem}code{background:#f4f4f4;padding:.1rem .3rem}li{margin:.6rem 0}</style>
</head>
<body>
  <h1>WhatsApp Business Locker APK Downloads</h1>
  <p>Download APK artifacts from GitHub Actions or Releases.</p>
  <ol>
    <li>Run CI workflow <code>Android CI</code> on GitHub.</li>
    <li>Open workflow artifacts and download APK zip files.</li>
    <li>Install on Android: allow unknown sources, then tap APK.</li>
  </ol>
  <p>Machine-readable APK list: <a href="apk-list.json">apk-list.json</a></p>
  <p><strong>Debug APK path:</strong> <code>$DEBUG_APK</code></p>
  <p><strong>Release APK path:</strong> <code>$RELEASE_APK</code></p>
</body>
</html>
HTML

echo "Generated: $OUT_DIR/apk-list.json and $OUT_DIR/index.html"
