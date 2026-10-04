# Testing and hardware reports

[← Project overview](../README.md)

## Verified for beta 0.5.1

The APK was compiled against Android SDK 35, converted to DEX and verified with Android APK signature tools. Version code 8 and the signing certificate match the intended upgrade path from earlier supplied betas.

The standalone Java suites passed:

- Natural / bypass unity, manual gain, buffer bounds, stereo output equality and limiter ceiling.
- Mute silence and cleared pitch, echo, EQ and voice-care state.
- Echo arrival time and decaying feedback; pitch direction and robot sidebands.
- EQ boost / cut, speech compression, grit and overload at 44.1 / 48 kHz.
- Soft noise expansion, calibrated floor, bounded level assist and no floor-only amplification.
- Selective de-essing and all 16 presets at 8 / 16 / 32 / 44.1 / 48 / 96 kHz.
- Both approximately 5 ms and 10 ms processing-block paths.
- Sample-rate negotiation, stereo downmix without overflow and buffer-growth capacity limits.

- Device selector identity across reordered snapshots, repeated-ID removal, distinct same-name ports, stable labels, explicit empty selections, disconnect handling and immutable rows.
- Native sample-rate/burst preference with fixed-format and missing-property fallbacks.
- 20,000 randomized bounded capture-queue operations, oversized bursts, discarded-frame accounting and recovery smoothing.
- Buffer growth, ten-second stable reductions, failed-probe hysteresis and capacity bounds.
- Causal limiter block invariance, late-peak behaviour and output ceiling at 8 / 44.1 / 48 / 96 / 192 kHz.

Run [the test script](../tests/run-tests.sh) using the [build guide](BUILDING.md).

## 0.4.1 support-link patch

This patch adds the creator-provided Ko-fi link to About and the repository support sections. APK compilation and signature verification were repeated. The audio engine is the 0.4.0 engine covered by the digital tests above; no new physical-device or UI validation is claimed.

## 0.4.2 routing and interface review

The selector tests and both existing audio suites passed. APK compilation, manifest version/permissions, alignment and signature checks passed. The supplied APK uses the earlier beta certificate. Source review covered selector event handling, fresh start preflight, hotplug handling, supported media-route categories, permission loss, worker start rejection and calibration/unmute guards. UI changes were reviewed in source only; no rendered UI or Android runtime routing test is claimed.

## 0.5.0 interface revision

The native UI compiles against SDK 35. The existing selector and digital audio regression suites were rerun. The APK upgrade version, alignment and signatures were checked. Source review covered the featured-preset picker, null-safe featured buttons, expandable creative controls, preset restoration, dropdown affordances, calibration state and per-tab scrolling. The SVG design reference was rendered and visually reviewed; it is not a rendered Android UI or device screenshot.

## Physical validation still needed

No phone or emulator UI session has been validated in this environment. No physical microphone / speaker pair or total acoustic delay is certified. The speaker guide is a list of examples, not a tested-device whitelist.

**Everyone is invited to test their own microphone and speaker, whatever the brand or price.** Useful setups include a phone mic → wired output, phone mic → generic Bluetooth speaker, USB mic or compatible wireless receiver → Bluetooth output, wired headset mic → external output, and a stereo USB interface. Android must recognise the connected input and output. Please report both successful and unsuccessful setups.

For each setup, record:

1. Phone model, Android version and MicBridge version.
2. Microphone model or generic label, adapter, and selected input type.
3. Speaker model or generic label, connection type and selected output.
4. Actual route, sample rate, processing mode, app buffer, underruns and capture-backlog trimming shown on Live.
5. Preset, manual gain, voice-care sliders and whether quiet calibration was completed.
6. Audible delay, clipping, crackles, feedback and session stability.
7. If measuring latency, the measurement method and microphone-to-speaker result. An app buffer value alone is not a latency measurement.
8. Disconnect, focus-loss and app-leave behaviour. Start every test muted and at low volume.

Email **[jerrychiengchintung@gmail.com](mailto:jerrychiengchintung@gmail.com?subject=MicBridge%20beta%20feedback%20and%20requests)** with your experience and feature requests, or use the public [hardware report form](https://github.com/jerrychiengct/micbridge-android/issues/new/choose). The app's About page can prepare a feedback email; you review and send it yourself.

## 0.5.1 latency and quality tests on hardware

Compare Fast and Balanced modes with **Lecture**, pitch and echo off, starting muted at low speaker volume. Test wired/USB and Bluetooth output separately. Record the same short speech passage and your measurement method if you measure acoustic delay. Run at least a 30-minute lecture-length session; observe both diagnostics and audible speech, including consonants, pauses, quiet speech and loud transients.

After a brief workload spike, check that delay recovers. Backlog trimming means old capture was discarded: occasional recovery may be useful, but continuously rising trimming or missing syllables is a failed quality result. Try Balanced mode and report the setup. Watch for repeated buffer changes, crackles, interruptions, feedback and mute/stop/disconnect behaviour. These tests require actual hardware and have not been completed in this environment.

## Capture interface screenshots

Take screenshots from the installed app, using the phone's normal screenshot shortcut. Capture these four views in portrait:

- **Live:** connection selectors and a muted session. Hide identifying Bluetooth device names if needed.
- **Studio:** the preset collection and a second capture showing the EQ / voice-care controls.
- **Speakers:** the generic guide and named examples.
- **About:** creator, support and collaboration information.

Crop only unnecessary system areas; preserve the actual UI. Note the app version, phone model and Android version. Do not include notifications, private email conversations, patient information or other personal content. Place reviewed captures under `docs/screenshots/`, give them clear filenames and add a labelled gallery to the README. Device captures should be labelled by version; design illustrations should be labelled as illustrations.
