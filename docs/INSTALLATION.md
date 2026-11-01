# Install and test MicBridge

[← Project overview](../README.md)

## Current 0.7.0 Studio preview

Download [MicBridge Preview 0.7.0](../dist/MicBridge-0.7.0-preview.apk), choose **Download raw file** and open the APK on Android 8+. This updates 0.6.0 Preview using the same package and signing certificate. It remains separate from the older 0.5.x app.

- **Effects → Presets:** search all 17 voices or filter Speech/Character; select Synthetic Overlord for the mechanical character effect.
- **Effects → Sound controls:** EQ, pitch, tone, grit, metallic layers, robot, echo and voice care.
- **Theme** in the header: Follow system, Studio Light, Graphite Dark or Midnight OLED.
- **Live → Your workspace:** Simple or Studio. The switch changes control visibility without changing the sound.
- **Screen lock:** start the session while the app is visible, then lock or switch apps. Allow Notifications for Mute/Unmute and Stop controls. [Complete screen-lock setup and test guide](BACKGROUND_AUDIO.md).

This preview's lifecycle and themes have digital regression coverage and SDK compilation checks. Physical screen-lock playback and installed UI validation remain required.

Current 0.7.0 APK SHA-256: `96997f2dd77f968dc834b90a76760a2f424e02692d5f19d19cc3b629422d097f`.

## Earlier 0.5.1 beta download

Download [MicBridge 0.5.1 beta](../dist/MicBridge-0.5.1-beta.apk), open the GitHub file page and choose **Download raw file**. The repository is public; a GitHub account is not required to download the APK. Android 8.0+ is required. The APK is a test build, not a Play Store release.

Open the downloaded APK on your phone. If Android asks, allow installation from the app you used to open the file. The supplied 0.5.1 APK uses the same test certificate as previous supplied betas and version code 8. An independently built APK may use a different key and require uninstalling the earlier build.

## First session

1. Grant **Microphone** and, on Android 12+, **Nearby devices**. Pairing is managed in Android settings.
2. Plug in an Android-compatible USB / wired microphone if desired. Otherwise choose **Phone microphone**. USB names can be generic. Analogue microphones require a suitable Android-compatible adapter.
3. Pair the Bluetooth speaker and enable **Media audio**. Test ordinary music through it, then stop the music. Wired / USB outputs can also be selected.
4. Open **Live**, tap **Refresh connected devices**, then select the microphone and audio output.
5. Start at low physical speaker volume and microphone gain around **1.0×**. Keep the speaker away from the microphone.
6. Select **Lecture** or **Studio Speech** in **Effects**. Tap **Start session**. MicBridge sends silence while checking the actual audio route.
7. When **Ready · muted** appears, tap **Calibrate background noise**. Stay quiet for 1.5 seconds. You remain muted afterward.
8. Check the microphone level, then tap **Unmute**. Raise speaker volume gradually. Recalibrate after changing microphone or room.
9. Assess speech for at least one minute. Tap **Mute now** or **Stop session** when finished.

For 0.7.0 Preview, the screen can turn off and the session can continue in the background. Stop, app dismissal, selected-device removal, an actual-route change or audio-focus loss ends it. Earlier 0.5.x/0.6.0 builds require the app to stay visible.

## Troubleshooting

| Symptom | First checks |
| --- | --- |
| Microphone absent | Reconnect it, check the adapter supports microphone input, grant permissions and refresh. Confirm Android recognises it in another audio app. |
| Speaker absent | Pair in Android settings, enable Media audio, stop other media and refresh. BLE control alone is not LE Audio playback. |
| Route unavailable | Recheck both selections. Use a built-in, USB or wired microphone; hands-free Bluetooth input is excluded. Android must report the selected media output as the actual route. |
| Delay while speaking | Use Lecture / Studio Speech with pitch and echo off. Try **Low latency mode**. Compare with a wired or USB output. Bluetooth buffering cannot be eliminated here. |
| Crackles or interruptions | Stop the session, turn **Low latency mode** off, then restart. Check diagnostic underruns and capture-backlog trimming and test again. Continuous trimming can skip syllables; report it. |
| Harsh or muffled speech | Reduce de-essing / noise reduction if consonants soften. Adjust mid / treble gently; avoid extreme boosts. |
| Input clipping | Reduce transmitter or microphone input level. Software cannot repair already-clipped input. |
| Squeal / feedback | Mute or stop, lower physical speaker volume and increase microphone / speaker separation. |
| Calibration reports a loud background | Stay quiet and retry. If the room is noisy, reduce noise-reduction strength; it does not remove other voices. |
| Screen-lock playback stopped | Verify you installed 0.7.0 Preview, started while visible, and have microphone access. Check for a call, route change or device battery restriction; report the actual setup. |

## Verify the downloaded APK

SHA-256 for the supplied 0.5.1 beta:

```text
531ce7c3b7dba40b638c57e326038068756eb08c08381d0524fc9989cdb654ea
```

On Windows PowerShell:

```powershell
Get-FileHash .\MicBridge-0.5.1-beta.apk -Algorithm SHA256
```

On Linux:

```bash
sha256sum MicBridge-0.5.1-beta.apk
```

## Device selection in 0.5.1

Choose both devices explicitly. Refresh preserves choices by Android device ID, not row position. A disconnected device resets its selector to the prompt; select a replacement yourself. Same-name ports are numbered because identical product names do not prove that two routes are the same hardware. Bluetooth media and LE Audio profiles are different routes. Hands-free, earpiece and virtual system routes are filtered from this app.

## Share your experience

Everyone is welcome to test their own Android-compatible microphone and speaker, across brands and prices. Email **[jerrychiengchintung@gmail.com](mailto:jerrychiengchintung@gmail.com?subject=MicBridge%20beta%20feedback%20and%20requests)** with your experience, issues and feature requests, or use About → **Email feedback or a request**. Include your setup, settings and Live diagnostics. [Testing guide](TESTING.md)
