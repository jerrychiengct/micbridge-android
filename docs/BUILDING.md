# Build and test

[← Project overview](../README.md)

## Requirements

- JDK 17.
- Android SDK platform 35 and Android build tools.
- Gradle 8.9 for the Gradle route; Android Gradle Plugin 8.7.3 is declared by the project.
- Android 8.0+ for installation.

The app is Java with native Android UI and no third-party app runtime dependencies. A Gradle wrapper is not included.

## Android Studio / Gradle

Open the repository as a project in Android Studio and use a Gradle 8.9 installation. Build `:app:assembleDebug` or run:

```bash
gradle :app:assembleDebug
```

The debug APK appears under `app/build/outputs/apk/debug/`. Its debug certificate may differ from the supplied beta certificate; an independently signed build may require uninstalling the earlier APK.

## SDK-only build

From the repository root:

```bash
MICBRIDGE_PLATFORM="$ANDROID_HOME/platforms/android-35" \
MICBRIDGE_TOOLS="$ANDROID_HOME/build-tools/35.0.0" \
./build-apk.sh
```

Output: `dist/MicBridge-0.4.1-beta.apk`. If no JDK compiler is available, `MICBRIDGE_ECJ` can point to an Eclipse ECJ JAR. Keep the locally generated signing key for consistent updates. Signing keys, SDK paths and build directories are excluded from git.

## Digital tests

With JDK 17 installed:

```bash
./tests/run-tests.sh
```

Or use an installed ECJ JAR:

```bash
MICBRIDGE_ECJ=/absolute/path/to/ecj.jar ./tests/run-tests.sh
```

The script compiles only the platform-independent DSP / format-planning classes, runs both test suites and removes its temporary output. These tests do not require an Android device. They do not establish device compatibility or measure acoustic latency; see [Testing](TESTING.md).

## Source map

| Location | Role |
| --- | --- |
| `app/src/main/java/com/jerry/micbridge/MainActivity.java` | UI, permissions, selection, local preferences and creator links |
| `AudioEngine.java` | Capture / playback loop, route checks, buffers and calibration |
| `SignalProcessor.java` | Streaming effects, mute state and output limiting |
| `VoiceEnhancer.java` / `VoiceEqualizer.java` | Voice care and EQ |
| `AudioRoutePlan.java` | Format negotiation, block / buffer arithmetic and stereo downmix |
| `VoicePresets.java` / `EffectSettings.java` | Original preset tunings and immutable settings |
| `SpeakerCatalog.java` / `AppInfo.java` | Speaker examples and creator information |
| `tests/` | Standalone Java verification |

Class filenames after the first row are relative to `app/src/main/java/com/jerry/micbridge/`.
