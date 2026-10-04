# Release readiness

## 0.5.1 beta: completed in this environment

- [x] Reviewed selector discovery, identity, hotplug and pre-start availability handling.
- [x] Passed all four selector, DSP, voice-care and latency/quality regression suites.
- [x] Compiled the Android SDK 35 APK; checked application ID, version code 8, Android 8+ minimum and permissions.
- [x] Verified APK alignment and v2/v3 signatures using the earlier beta certificate.
- [x] Preserved creator credit, GitHub, collaboration email and Ko-fi support.
- [x] Updated install instructions, checksum and compatibility limits.

- [x] Reviewed native UI source and the labelled design reference; built the revised interface.

## Required before a production release

- [ ] On a range of Android phones, verify built-in microphones, wired microphones, USB microphones and compatible wireless USB receivers with generic Bluetooth, wired and USB outputs. Record Android/firmware versions and actual hardware.
- [ ] On at least one other Android phone, verify a wired mic and USB mono/stereo interface; list any unsupported adapters.
- [ ] Connect/disconnect repeatedly, reorder routes, toggle media audio, switch Bluetooth profiles and revoke permissions. No mismatched label, duplicate ID or silent replacement may occur.
- [ ] Exercise muted start, calibration, mute/unmute, stop/start, call/focus interruption, app leave/lock and actual route change. Confirm no continuing fallback playback.
- [ ] Validate portrait/landscape, system cutouts, small displays, large fonts and TalkBack on an installed app. Capture genuine screenshots.
- [ ] Measure acoustic delay, crackling and stability for at least a 30-minute lecture, including Fast and Balanced modes. App buffer duration alone is insufficient.
- [ ] Set up an owner-controlled production signing key and secure backup; the supplied test certificate is for beta upgrades. Never commit signing keys. Decide the production package/update path before distribution.
- [ ] Review the chosen distribution platform's current target SDK, privacy and release requirements before submitting.

No phone/emulator or acoustic setup is attached to this build environment. These open checks are release gates, not completed results. See [Testing](TESTING.md) for the report format.
