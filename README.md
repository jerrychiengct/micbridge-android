# MicBridge 0.1.0 — Android test prototype

Live USB microphone monitoring through a Bluetooth media speaker. Prepared for a Poco X7 Pro, ONSMO USB-C wireless microphone receiver and Tribit Bluetooth speaker. Compatibility with that hardware has **not** been verified on a physical phone.

## Download beta APK

[Download MicBridge 0.1.0 beta](dist/MicBridge-0.1.0-beta.apk). On GitHub, open the file and select **Download raw file**.

## Install and test

1. Copy `MicBridge-0.1.0-beta.apk` to the phone, open it and allow installation from the app used to open the file, if Android prompts you.
2. Plug the ONSMO USB-C receiver into the phone. Power on the microphone and receiver.
3. Pair the Tribit in Android Bluetooth settings. Enable its media audio output and check that normal music plays through it. Stop the music before testing MicBridge.
4. Open MicBridge. Allow **Microphone** and **Nearby devices** permissions. The app uses Android's existing Bluetooth pairing; it does not scan for or pair devices itself.
5. Tap **Refresh devices**, then select the USB microphone and Bluetooth media speaker. Names may appear as generic USB audio labels rather than ONSMO.
6. Put the speaker away from the microphone and keep its volume low. Start at microphone gain **1.0×**.
7. Tap **Start microphone**. The app captures audio and sends silence while checking actual routes. It enables Unmute only after both routes match your selections.
8. Speak and check that the microphone level meter moves. Tap **Unmute speaker**, then gradually raise speaker volume.
9. Test speech for one minute. Assess delay, crackles and stability. Tap **Mute speaker**, then **Stop microphone** when finished.

Voice playback requires the app to remain on screen. It keeps the display awake while active and stops when you leave the app, lock the phone, lose audio focus or unplug a selected device. Background and screen-off operation are intentionally absent from this first prototype.

## What this prototype provides

- USB input and Bluetooth A2DP / BLE speaker output selection.
- Actual route verification before enabling voice playback; no deliberate phone-microphone or phone-speaker fallback.
- Start muted, manual mute, gain from 0.1× to 3.0× and raw microphone peak meter.
- Mono input duplicated to stereo output; 48 kHz preferred with 44.1 kHz fallback.
- Block peak limiter at approximately 90% of digital full scale.
- Audio focus handling and device disconnect handling.
- No recording files, cloud processing, network permission, analytics or advertising.
- Android 8.0 and later. Physical hardware compatibility must be tested.

## Practical limitations

Bluetooth introduces hardware/codec buffering that this app cannot remove. It is a live monitoring prototype, not a promise of delay-free PA or singing performance. Mute and Stop prevent new audio from being sent; a short tail already buffered in the Bluetooth speaker may still play. If feedback starts, use the speaker's physical volume control or power button.

The limiter reduces digital output clipping. It does not remove clipping at the microphone transmitter or prevent acoustic feedback. Increasing gain can amplify noise and feedback. The app does not measure end-to-end latency; the sample rate displayed is not a latency measurement.

Android treats requested devices as preferences. The app checks actual routing while streaming, but changes at the operating-system level can occur between checks. It stops on a detected route mismatch. Automatic routing can vary by Android version and vendor firmware.

## Troubleshooting

- **No USB microphone:** reconnect the receiver; check whether another recording app can use the external microphone. Camera/recorder compatibility alone does not prove simultaneous USB input and Bluetooth output will work.
- **No Bluetooth media speaker:** pair it in system settings, enable media audio, then refresh. Bluetooth hands-free/SCO devices are not supported by this prototype.
- **Route could not be verified:** ensure the Tribit is selected as system media output, stop other audio apps and retry. The phone's audio policy may prevent this combination.
- **Meter moves but no voice:** verify that Unmute was pressed, media volume is up and the transmitter is not muted. Built-in microphone capture is deliberately excluded.
- **Voice arrives late:** this is likely Bluetooth buffering. A wired speaker/audio interface or microphone connected directly to a PA system is the better route for latency-sensitive use.
- **App stops after switching apps:** expected for this foreground-only prototype.

## Build from source

Open this directory in Android Studio with JDK 17. Install Android SDK platform 35 and let Android Studio synchronise Gradle. This project pins Android Gradle Plugin 8.7.3; use Gradle 8.9. It uses native Android Java APIs and has no app-library dependencies. No Gradle wrapper is included. Android Studio can use an installed Gradle distribution, or run `gradle :app:assembleDebug` with Gradle 8.9 installed.

Alternatively use the included SDK-only build script with JDK 17 and Android build-tools 35.0.0:

```bash
MICBRIDGE_PLATFORM="$ANDROID_HOME/platforms/android-35" \
MICBRIDGE_TOOLS="$ANDROID_HOME/build-tools/35.0.0" \
./build-apk.sh
```

This produces `dist/MicBridge-0.1.0-beta.apk`, signed with a locally generated test key. This is not a Play Store production release. Signing keystores are excluded from this repository. Keep your signing key separately for consistent APK updates; the manual script creates a new local test key if one is absent. Android Studio's default debug key differs; uninstalling the supplied app may be necessary before installing a separately signed build.

## Validation

The delivered APK was compiled against Android SDK 35, converted to DEX and checked with Android `apksigner` and `aapt`. Signal-processing checks cover mute silence, limiter ceiling, stereo channel matching, gain scaling and block bounds. Physical Poco/ONSMO/Tribit testing and end-to-end latency measurement remain outstanding. No emulator or real-device UI validation was available in the build environment.

## Android API references

- https://developer.android.com/reference/android/media/AudioRecord
- https://developer.android.com/reference/android/media/AudioTrack
- https://developer.android.com/reference/android/media/AudioRouting
- https://developer.android.com/ndk/guides/audio/audio-latency

Created for Jerry Chieng Chin Tung. Version 0.1.0, 1 October 2026.
