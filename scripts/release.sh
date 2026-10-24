#!/usr/bin/env bash
# Builds the release APK and signs it with the personal key.
#
# The signature includes the rotation lineage "debug key -> personal key", so the APK
# installs as an update over versions up to 0.1.4, which were signed with the debug key.
#
# Signing material is read from $SIGNING_DIR (default ~/.android/gps-trigger), outside the
# repository: release.jks, lineage.bin and signing.properties.
set -euo pipefail

cd "$(dirname "$0")/.."
SIGNING_DIR="${SIGNING_DIR:-$HOME/.android/gps-trigger}"
# shellcheck source=/dev/null
source "$SIGNING_DIR/signing.properties"
export KEY_PASS OLD_KEY_PASS

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties)"
BUILD_TOOLS="$SDK_DIR/build-tools/35.0.0"

./gradlew --quiet assembleRelease

VERSION="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
OUT=app/build/outputs/apk/release
APK="$OUT/gps-trigger-$VERSION.apk"

# 16 KB page alignment for the native library, as Android 15 devices with 16 KB pages need.
"$BUILD_TOOLS/zipalign" -f -P 16 4 "$OUT/app-release-unsigned.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" sign \
    --ks "$OLD_KEYSTORE" --ks-key-alias "$OLD_KEY_ALIAS" --ks-pass env:OLD_KEY_PASS \
    --next-signer --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" --ks-pass env:KEY_PASS \
    --lineage "$LINEAGE" --rotation-min-sdk-version 28 \
    --out "$APK" "$OUT/aligned.apk"
rm "$OUT/aligned.apk"

"$BUILD_TOOLS/apksigner" verify --min-sdk-version 28 --print-certs "$APK" \
    | grep -E "certificate (DN|SHA-256)"
echo "$APK"
