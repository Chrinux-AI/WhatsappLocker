#!/usr/bin/env bash
set -euo pipefail

TARGET_DIR=".jdk17"
if [[ -x "${TARGET_DIR}/bin/java" ]]; then
  echo "JDK17 already present at ${TARGET_DIR}"
  exit 0
fi

mkdir -p "${TARGET_DIR}"
ARCHIVE="/tmp/jdk17.tar.gz"
URL="https://github.com/adoptium/temurin17-binaries/releases/latest/download/OpenJDK17U-jdk_x64_linux_hotspot.tar.gz"

echo "Downloading Temurin JDK 17..."
curl -L --fail "$URL" -o "$ARCHIVE"

echo "Extracting JDK 17..."
TMP_DIR="/tmp/jdk17_extract"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"
tar -xzf "$ARCHIVE" -C "$TMP_DIR"

EXTRACTED_DIR=$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)
cp -r "$EXTRACTED_DIR"/* "$TARGET_DIR"/

"$TARGET_DIR/bin/java" -version
echo "JDK17 installed at ${TARGET_DIR}"
