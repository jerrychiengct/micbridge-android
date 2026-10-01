#!/usr/bin/env bash
# Dependency-free alternative to Gradle using installed Android SDK tools.
set -euo pipefail
cd "$(dirname "$0")"
: "${MICBRIDGE_PLATFORM:?Set MICBRIDGE_PLATFORM to the directory containing android.jar}"
: "${MICBRIDGE_TOOLS:?Set MICBRIDGE_TOOLS to the Android build-tools directory}"
TASK_BUILD="$PWD/build/manual"
rm -rf "$TASK_BUILD/classes" "$TASK_BUILD/dex" "$TASK_BUILD/res" "$TASK_BUILD/generated" "$TASK_BUILD/classes.jar"
mkdir -p "$TASK_BUILD/classes" "$TASK_BUILD/dex" "$TASK_BUILD/res"
"$MICBRIDGE_TOOLS/aapt2" compile --dir app/src/main/res -o "$TASK_BUILD/res"
mapfile -t TASK_RES < <(find "$TASK_BUILD/res" -name '*.flat' -print)
sed 's/<manifest xmlns:/<manifest package="com.jerry.micbridge" xmlns:/' app/src/main/AndroidManifest.xml > "$TASK_BUILD/AndroidManifest.xml"
"$MICBRIDGE_TOOLS/aapt2" link -o "$TASK_BUILD/base.apk" -I "$MICBRIDGE_PLATFORM/android.jar" \
    --manifest "$TASK_BUILD/AndroidManifest.xml" --java "$TASK_BUILD/generated" \
    --min-sdk-version 26 --target-sdk-version 35 --version-code 2 --version-name 0.2.0 \
    "${TASK_RES[@]}"
mapfile -t TASK_JAVA < <(find app/src/main/java "$TASK_BUILD/generated" -name '*.java' -print)
if command -v javac >/dev/null; then
    javac --release 17 -classpath "$MICBRIDGE_PLATFORM/android.jar" -d "$TASK_BUILD/classes" "${TASK_JAVA[@]}"
else
    : "${MICBRIDGE_ECJ:?Install a JDK or set MICBRIDGE_ECJ to Eclipse ecj.jar}"
    java -jar "$MICBRIDGE_ECJ" -source 1.8 -target 1.8 -bootclasspath "$MICBRIDGE_PLATFORM/android.jar" \
        -classpath "$MICBRIDGE_TOOLS/core-lambda-stubs.jar" -d "$TASK_BUILD/classes" "${TASK_JAVA[@]}"
fi
(cd "$TASK_BUILD/classes" && zip -qr "$TASK_BUILD/classes.jar" .)
"$MICBRIDGE_TOOLS/d8" --min-api 26 --lib "$MICBRIDGE_PLATFORM/android.jar" --output "$TASK_BUILD/dex" "$TASK_BUILD/classes.jar"
cp "$TASK_BUILD/base.apk" "$TASK_BUILD/unsigned.apk"
(cd "$TASK_BUILD/dex" && zip -q "$TASK_BUILD/unsigned.apk" classes*.dex)
"$MICBRIDGE_TOOLS/zipalign" -f 4 "$TASK_BUILD/unsigned.apk" "$TASK_BUILD/aligned.apk"
if [[ ! -f "$TASK_BUILD/test-signing.jks" ]]; then
    keytool -genkeypair -keystore "$TASK_BUILD/test-signing.jks" -storepass android -keypass android \
      -alias micbridge-test -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=MicBridge Test, O=Personal Prototype, C=MY'
fi
mkdir -p dist
"$MICBRIDGE_TOOLS/apksigner" sign --ks "$TASK_BUILD/test-signing.jks" --ks-pass pass:android \
    --key-pass pass:android --out dist/MicBridge-0.2.0-beta.apk "$TASK_BUILD/aligned.apk"
"$MICBRIDGE_TOOLS/apksigner" verify --verbose dist/MicBridge-0.2.0-beta.apk
echo 'Built dist/MicBridge-0.2.0-beta.apk'
