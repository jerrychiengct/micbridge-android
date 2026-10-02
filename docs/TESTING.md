# Testing and hardware reports

[← Project overview](../README.md)

## Verified for beta 0.4.0

The APK was compiled against Android SDK 35, converted to DEX and verified with Android APK signature tools. Version code 4 and the signing certificate match the intended upgrade path from earlier supplied betas.

The standalone Java suites passed:

- Natural / bypass unity, manual gain, buffer bounds, stereo output equality and limiter ceiling.
- Mute silence and cleared pitch, echo, EQ and voice-care state.
- Echo arrival time and decaying feedback; pitch direction and robot sidebands.
- EQ boost / cut, speech compression, grit and overload at 44.1 / 48 kHz.
- Soft noise expansion, calibrated floor, bounded level assist and no floor-only amplification.
- Selective de-essing and all 16 presets at 8 / 16 / 32 / 44.1 / 48 / 96 kHz.
- Both approximately 5 ms and 10 ms processing-block paths.
- Sample-rate negotiation, stereo downmix without overflow and buffer-growth capacity limits.

Run [the test script](../tests/run-tests.sh) using the [build guide](BUILDING.md).

## 0.4.1 support-link patch

This patch adds the creator-provided Ko-fi link to About and the repository support sections. APK compilation and signature verification were repeated. The audio engine is the 0.4.0 engine covered by the digital tests above; no new physical-device or UI validation is claimed.

## Physical validation still needed

No phone or emulator UI session has been validated in this environment. No physical microphone / speaker pair or total acoustic delay is certified. The speaker guide is a list of examples, not a tested-device whitelist.

Useful setups to report include a phone mic → wired output, phone mic → generic Bluetooth speaker, USB mic → Bluetooth output, wired headset mic → external output, and a stereo USB interface. The requested Poco X7 Pro / Tribit / Xiaomi combinations need actual testing too.

For each setup, record:

1. Phone model, Android version and MicBridge version.
2. Microphone model or generic label, adapter, and selected input type.
3. Speaker model or generic label, connection type and selected output.
4. Actual route, sample rate, processing mode, app buffer and underruns shown on Live.
5. Preset, manual gain, voice-care sliders and whether quiet calibration was completed.
6. Audible delay, clipping, crackles, feedback and session stability.
7. If measuring latency, the measurement method and microphone-to-speaker result. An app buffer value alone is not a latency measurement.
8. Disconnect, focus-loss and app-leave behaviour. Start every test muted and at low volume.

Use the [hardware report form](https://github.com/jerrychiengct/micbridge-android/issues/new/choose) if you have repository access, or email [Jerry](mailto:jerrychiengchintung@gmail.com?subject=MicBridge%20hardware%20test).

## Capture interface screenshots

Take screenshots from the installed app, using the phone's normal screenshot shortcut. Capture these four views in portrait:

- **Live:** connection selectors and a muted session. Hide identifying Bluetooth device names if needed.
- **Studio:** the preset collection and a second capture showing the EQ / voice-care controls.
- **Speakers:** the generic guide and named examples.
- **About:** creator, support and collaboration information.

Crop only unnecessary system areas; preserve the actual UI. Note the app version, phone model and Android version. Do not include notifications, private email conversations, patient information or other personal content. Place reviewed captures under `docs/screenshots/`, give them clear filenames and add a labelled gallery to the README. Device captures should be labelled by version; design illustrations should be labelled as illustrations.
