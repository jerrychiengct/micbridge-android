#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?ANDROID_HOME is required}"
PLATFORM="$ANDROID_HOME/platforms/android-35"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
BUILD="$PWD/build/x8boost"
SRC="app/src/x8boost"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/res" "$BUILD/generated" dist

"$TOOLS/aapt2" compile --dir "$SRC/res" -o "$BUILD/res"
mapfile -t RES < <(find "$BUILD/res" -name '*.flat' -print)
"$TOOLS/aapt2" link   -o "$BUILD/base.apk"   -I "$PLATFORM/android.jar"   --manifest "$SRC/AndroidManifest.xml"   --java "$BUILD/generated"   --min-sdk-version 26   --target-sdk-version 35   --version-code 1   --version-name 0.1-test   "${RES[@]}"

mapfile -t JAVA < <(find "$SRC/java" "$BUILD/generated" -name '*.java' -print)
javac --release 17 -classpath "$PLATFORM/android.jar" -d "$BUILD/classes" "${JAVA[@]}"
(cd "$BUILD/classes" && zip -qr "$BUILD/classes.jar" .)
"$TOOLS/d8" --min-api 26 --lib "$PLATFORM/android.jar" --output "$BUILD/dex" "$BUILD/classes.jar"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q "$BUILD/unsigned.apk" classes*.dex)
"$TOOLS/zipalign" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

if [[ ! -f "$BUILD/test-signing.jks" ]]; then
  keytool -genkeypair -keystore "$BUILD/test-signing.jks" -storepass android -keypass android     -alias x8boost-test -keyalg RSA -keysize 2048 -validity 3650     -dname 'CN=X8 Boost Test, O=Personal Prototype, C=MY'
fi

"$TOOLS/apksigner" sign   --ks "$BUILD/test-signing.jks"   --ks-pass pass:android   --key-pass pass:android   --out dist/X8Boost-0.1-test.apk   "$BUILD/aligned.apk"

"$TOOLS/apksigner" verify --verbose --print-certs dist/X8Boost-0.1-test.apk
sha256sum dist/X8Boost-0.1-test.apk > dist/X8Boost-0.1-test.apk.sha256
ls -lh dist/X8Boost-0.1-test.apk
