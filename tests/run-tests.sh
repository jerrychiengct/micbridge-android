#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_TEST_DIR="$(mktemp -d)"
trap 'rm -rf "$TASK_TEST_DIR"' EXIT
TASK_SOURCE_DIR="app/src/main/java/com/jerry/micbridge"
TASK_SOURCES=("$TASK_SOURCE_DIR/SignalProcessor.java" "$TASK_SOURCE_DIR/EffectSettings.java" "$TASK_SOURCE_DIR/VoiceEqualizer.java" "$TASK_SOURCE_DIR/VoiceEnhancer.java" "$TASK_SOURCE_DIR/VoicePresets.java" "$TASK_SOURCE_DIR/AudioRoutePlan.java" "$TASK_SOURCE_DIR/DeviceChoices.java" tests/SignalProcessorTest.java tests/VoiceCareTest.java tests/DeviceChoicesTest.java)
if command -v javac >/dev/null; then
    javac --release 17 -d "$TASK_TEST_DIR" "${TASK_SOURCES[@]}"
else
    : "${MICBRIDGE_ECJ:?Install JDK 17 or set MICBRIDGE_ECJ to an ECJ compiler JAR}"
    java -jar "$MICBRIDGE_ECJ" -17 -d "$TASK_TEST_DIR" "${TASK_SOURCES[@]}"
fi
java -cp "$TASK_TEST_DIR" SignalProcessorTest
java -cp "$TASK_TEST_DIR" VoiceCareTest
java -cp "$TASK_TEST_DIR" DeviceChoicesTest
