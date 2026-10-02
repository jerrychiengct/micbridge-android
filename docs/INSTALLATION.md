# Install and test MicBridge

[← Project overview](../README.md)

## Download

Download [MicBridge 0.4.1 beta](../dist/MicBridge-0.4.1-beta.apk), open the GitHub file page and choose **Download raw file**. Sign in with an account that has access to this private repository. Android 8.0+ is required. The APK is a test build, not a Play Store release.

Open the downloaded APK on your phone. If Android asks, allow installation from the app you used to open the file. The supplied 0.4.1 APK uses the same test certificate as previous supplied betas and version code 5. An independently built APK may use a different key and require uninstalling the earlier build.

## First session

1. Grant **Microphone** and, on Android 12+, **Nearby devices**. Pairing is managed in Android settings.
2. Plug in an Android-compatible USB / wired microphone if desired. Otherwise choose **Phone microphone**. USB names can be generic. Analogue microphones require a suitable Android-compatible adapter.
3. Pair the Bluetooth speaker and enable **Media audio**. Test ordinary music through it, then stop the music. Wired / USB outputs can also be selected.
4. Open **Live**, tap **Refresh connected devices**, then select the microphone and audio output.
5. Start at low physical speaker volume and microphone gain around **1.0×**. Keep the speaker away from the microphone.
6. Select **Lecture** or **Studio Speech** in **Studio**. Tap **Start session**. MicBridge sends silence while checking the actual audio route.
7. When **Ready · muted** appears, tap **Calibrate background noise**. Stay quiet for 1.5 seconds. You remain muted afterward.
8. Check the microphone level, then tap **Unmute**. Raise speaker volume gradually. Recalibrate after changing microphone or room.
9. Assess speech for at least one minute. Tap **Mute now** or **Stop session** when finished.

Phone volume buttons adjust media volume. The app keeps the display awake while active. Playback stops on leaving the app, locking the phone, selected-device removal, an actual-route change or audio-focus loss. Moving between the four tabs keeps the session running. Opening a settings page, website or email app stops it.

## Troubleshooting

| Symptom | First checks |
| --- | --- |
| Microphone absent | Reconnect it, check the adapter supports microphone input, grant permissions and refresh. Confirm Android recognises it in another audio app. |
| Speaker absent | Pair in Android settings, enable Media audio, stop other media and refresh. BLE control alone is not LE Audio playback. |
| Route unavailable | Recheck both selections. A Bluetooth headset mic and a separate Bluetooth media speaker may be an unsupported combination. |
| Delay while speaking | Use Lecture / Studio Speech with pitch and echo off. Try Fast mode. Compare with a wired or USB output. Bluetooth buffering cannot be eliminated here. |
| Crackles or interruptions | Stop the session, turn **Prefer shorter latency buffers** off, then restart. Check diagnostic underruns and test again. |
| Harsh or muffled speech | Reduce de-essing / noise reduction if consonants soften. Adjust mid / treble gently; avoid extreme boosts. |
| Input clipping | Reduce transmitter or microphone input level. Software cannot repair already-clipped input. |
| Squeal / feedback | Mute or stop, lower physical speaker volume and increase microphone / speaker separation. |
| Calibration reports a loud background | Stay quiet and retry. If the room is noisy, reduce noise-reduction strength; it does not remove other voices. |
| Session stopped after opening a link | Expected: the beta requires the app to remain visible. Return and start a new muted session. |

## Verify the downloaded APK

SHA-256 for the supplied 0.4.1 beta:

```text
11bce5542c988bdafe38fe6297556cf2425744d4984ceb0f634e8b05634bd071
```

On Windows PowerShell:

```powershell
Get-FileHash .\MicBridge-0.4.1-beta.apk -Algorithm SHA256
```

On Linux:

```bash
sha256sum MicBridge-0.4.1-beta.apk
```
