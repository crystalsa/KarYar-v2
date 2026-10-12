#!/usr/bin/env bash
set -e

# Arguments:
# $1: mode ("test" or "build")
MODE="${1:-test}"
TARGET_PATH="${GITHUB_WORKSPACE:-.}/debug.keystore"

echo "Checking debug keystore in mode: $MODE"

# 1. Try restore from DEBUG_KEYSTORE_BASE64 environment variable (GitHub Secret)
if [ -n "$DEBUG_KEYSTORE_BASE64" ]; then
  echo "Restoring debug.keystore from DEBUG_KEYSTORE_BASE64 environment variable..."
  echo "$DEBUG_KEYSTORE_BASE64" | base64 --decode > "$TARGET_PATH" 2>/dev/null || echo "$DEBUG_KEYSTORE_BASE64" | base64 -d > "$TARGET_PATH"
fi

# 2. Try restore from repository debug.keystore.base64 file
if [ ! -f "$TARGET_PATH" ] && [ -f "debug.keystore.base64" ]; then
  echo "Restoring debug.keystore from repository debug.keystore.base64..."
  base64 --decode debug.keystore.base64 > "$TARGET_PATH" 2>/dev/null || base64 -d debug.keystore.base64 > "$TARGET_PATH"
fi

# 3. Check if keystore was restored
if [ -f "$TARGET_PATH" ] && [ -s "$TARGET_PATH" ]; then
  echo "Debug keystore restored successfully with persistent signature."
  exit 0
fi

# 4. Handle fallback when neither Secret nor base64 file is present
if [ "$MODE" = "test" ]; then
  echo "No persistent debug keystore found. Generating temporary fallback debug.keystore for local JVM/unit tests..."
  keytool -genkey -v -keystore "$TARGET_PATH" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || true
elif [ "$MODE" = "build" ]; then
  echo "::warning::DEBUG_KEYSTORE_BASE64 تنظیم نشده است! امضای debug هر بار متفاوت است و آپدیت روی نسخه‌ی نصب‌شده امکان‌پذیر نیست."
  echo "Generating temporary debug.keystore for artifact packaging..."
  keytool -genkey -v -keystore "$TARGET_PATH" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || true
fi
