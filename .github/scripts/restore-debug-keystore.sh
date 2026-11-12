#!/usr/bin/env bash
set -e

# Arguments:
# $1: mode ("test" or "build")
MODE="${1:-test}"
TARGET_PATH="${GITHUB_WORKSPACE:-.}/debug.keystore"

echo "Checking keystore in mode: $MODE"

# 1. Restore from GitHub Actions Secrets (DEBUG_KEYSTORE_BASE64 or KEYSTORE_BASE64)
SECRET_B64="${DEBUG_KEYSTORE_BASE64:-$KEYSTORE_BASE64}"
if [ -n "$SECRET_B64" ]; then
  echo "Restoring keystore from GitHub Secret environment variable..."
  echo "$SECRET_B64" | tr -d '[:space:]' | base64 --decode > "$TARGET_PATH" 2>/dev/null || \
    echo "$SECRET_B64" | tr -d '[:space:]' | base64 -d > "$TARGET_PATH"
fi

# 2. Local environment fallback (only when local debug.keystore.base64 exists outside GitHub)
if { [ ! -f "$TARGET_PATH" ] || [ ! -s "$TARGET_PATH" ]; } && [ -f "debug.keystore.base64" ]; then
  echo "Restoring keystore from local debug.keystore.base64 file..."
  tr -d '[:space:]' < debug.keystore.base64 | base64 --decode > "$TARGET_PATH" 2>/dev/null || \
    tr -d '[:space:]' < debug.keystore.base64 | base64 -d > "$TARGET_PATH"
fi

# 3. Verify if persistent keystore was restored
if [ -f "$TARGET_PATH" ] && [ -s "$TARGET_PATH" ]; then
  echo "Persistent keystore restored successfully."
  exit 0
fi

# 4. In "test" mode only, generate an ephemeral debug keystore so unit tests can run on forks
if [ "$MODE" = "test" ]; then
  echo "Generating temporary debug.keystore for unit test execution..."
  keytool -genkey -v \
    -keystore "$TARGET_PATH" \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || true
  exit 0
fi

echo "::warning::No signing keystore secret (KEYSTORE_BASE64 / DEBUG_KEYSTORE_BASE64) found in environment."
exit 0
