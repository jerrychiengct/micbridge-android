# MicBridge 0.2.0 beta

An Android live microphone app with a redesigned interface and an offline voice-effects engine. Prepared for Jerry's Poco X7 Pro, ONSMO USB-C microphone receiver and Tribit speaker. The app routes audio by Android device capabilities; Xiaomi and other brands do not require separate drivers or a model whitelist.

## Download

[Download MicBridge 0.2.0 beta APK](dist/MicBridge-0.2.0-beta.apk). Open the file on GitHub and select **Download raw file**. This repository is private: sign in with an authorised account to download. [Previous 0.1.0 beta](dist/MicBridge-0.1.0-beta.apk) remains available.

The new beta uses version code 2 and the same local signing certificate as 0.1.0, so it can update that installed build. Signing keystores are excluded from GitHub. It is an installable APK, not a Play Store release.

## What changed

- Light interface with generous spacing, dark live-level monitor and teal controls.
- **Live**, **Voice Studio** and **Speakers** tabs; Start/Stop and Mute remain visible in every tab.
- Real microphone-level history and input clipping indication. This is level history, not a sample waveform or a latency measurement.
- Six presets: Natural, Clear voice, Deep voice, Bright voice, Robot and Room echo.
- Effect bypass, approximate pitch shift from −6 to +6 semitones, warm/bright tone, robot modulation, echo blend and 100–600 ms echo timing.
- Low-frequency rumble reduction, output limiter and microphone gain.
- Settings retained locally; no microphone audio is stored.
- Searchable speaker guide covering 12 brands, including Xiaomi Sound Pocket, Sound Outdoor and Xiaomi Bluetooth Speaker.
- USB and wired microphone input, plus explicitly selectable phone microphone.
- Bluetooth A2DP and Android LE Audio media outputs, plus wired and USB audio outputs. Phone-speaker output and hands-free Bluetooth SCO input are excluded.

## First test

1. Install the APK on the Poco. Allow **Microphone** and **Nearby devices** permissions.
2. Connect the ONSMO USB-C receiver and power on its microphone.
3. Pair the Tribit or Xiaomi speaker in Android Bluetooth settings; enable media audio. Confirm normal music plays through it, then stop the music.
4. Open **Live**, tap **Refresh connected devices**, and select the receiver and speaker. USB audio names may be generic.
5. Turn speaker volume low and keep it away from the microphone. Leave gain at 1.0× and select **Natural** in Voice Studio.
6. Tap **Start session**. The app sends silence while verifying the actual microphone and output routes.
7. When it reports **Ready · muted**, check the input meter, then tap **Unmute**.
8. Try a preset. Start with subtle effects. Check delay, distortion and stability for at least one minute.
9. Tap **Mute now** or **Stop session** when finished. Phone volume buttons control media volume.

The app must remain visible. It keeps the display awake during a session and stops when it leaves the screen, the phone locks, a selected device disconnects, an actual route changes, or another app/call takes audio focus. Switching between the three in-app tabs does not stop audio. Opening settings or a manufacturer link does.

## Speaker compatibility

The guide is informational, not an exhaustive list of every portable speaker or a list of physically tested devices. The discovery list is not restricted to the guide's brands. Any connected output exposed through a supported Android audio route is eligible.

Classic Bluetooth media playback uses A2DP. LE Audio playback is supported when Android exposes a BLE speaker/headset audio route. A Bluetooth Low Energy connection used only for app control is not an audio route. Wi-Fi-only speakers and proprietary wireless systems require a suitable audio receiver or another supported connection. Stereo/party linking is handled by the speaker manufacturer, not by MicBridge.

Manufacturer examples and research links are in [SPEAKER_GUIDE.md](SPEAKER_GUIDE.md). Models researched on 1 October 2026. Availability in Malaysia varies; this app is not a retailer and does not show prices or stock.

## Sound modulator

Audio is processed locally at 48 kHz where supported, with a 44.1 kHz fallback:

`Selected microphone → rumble filter / tone → pitch → robot modulation → echo → gain / limiter → selected output`

Natural mode bypasses creative stages with neutral settings. Effects can also be bypassed with the switch. Pitch uses two interpolated, crossfaded delay taps with a 40 ms window. It is an approximate creative effect and can produce grain or warble; it is not a professional formant-preserving pitch processor. Robot texture uses 70 Hz ring modulation. Echo uses a feedback delay with selectable spacing and blend. Tone is a simple crossover-based warm/bright tilt, not a graphic equaliser. The final block limiter bounds digital output to roughly 90% full scale. Mute clears delayed effect tails inside the app.

Bluetooth buffering cannot be removed by this app. Pitch processing and echo alter timing further. Mute/Stop prevent new audio from being submitted, but already buffered Bluetooth audio may continue briefly. If acoustic feedback occurs, lower the speaker's physical volume or power it off. The limiter cannot prevent feedback or repair clipping at the microphone transmitter.

## Build

Android 8.0+ (API 26); compile/target SDK 35; JDK 17; Android Gradle Plugin 8.7.3 and Gradle 8.9. No third-party app runtime dependencies.

Open the project in Android Studio, use an installed Gradle 8.9 distribution, and build `:app:assembleDebug`. No Gradle wrapper is included. Android Studio's debug certificate differs from the supplied APK's local test certificate, so independently signed APKs may require uninstalling the previous build.

Alternatively run the included SDK-only script:

```bash
MICBRIDGE_PLATFORM="$ANDROID_HOME/platforms/android-35" \
MICBRIDGE_TOOLS="$ANDROID_HOME/build-tools/35.0.0" \
./build-apk.sh
```

The script writes `dist/MicBridge-0.2.0-beta.apk`. Keep the locally generated signing key separately for consistent updates. If no JDK compiler is available, `MICBRIDGE_ECJ` may point to an Eclipse ECJ compiler JAR. Keystores, local SDK paths and build directories are ignored by git.

## Validation and limits

- Compiled against Android SDK 35; DEX conversion and APK signature checks completed.
- Digital tests cover unity gain, gain scaling, limiter ceiling, stereo equality, mute, echo timing, decay, clearing muted tails, effect bypass and buffer bounds.
- Overload tests exercise effects at 44.1 and 48 kHz. Spectral tests check pitch direction and robot modulation sidebands.
- Same signing certificate as v0.1.0, verified for upgrade consistency.
- No real-device or emulator UI validation was available. The Poco/ONSMO/Tribit/Xiaomi combination and end-to-end delay require physical testing. The manufacturer's Bluetooth specification is not proof of this exact live input/output combination.

No internet permission, advertising, analytics, cloud processing or audio recording files. Opening an optional source link uses the phone's external browser.

Created for Jerry Chieng Chin Tung. Beta 0.2.0, 1 October 2026.
