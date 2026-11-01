# Screen-lock sessions — 0.7.0 Preview

## Use it

1. Update the installed 0.6.0 Preview with the 0.7.0 APK. It uses the same preview package and signing certificate.
2. Allow Microphone and, for connected Bluetooth information on Android 12+, Nearby devices. On Android 13+, allow Notifications for lock-screen and notification-tray controls.
3. While MicBridge is visible, choose your microphone and speaker, then Start session. The microphone listens but output stays muted until the actual selected routes are verified.
4. Calibrate in quiet if desired, then Unmute. You can lock the screen or switch apps while this explicit session is active.
5. Use the ongoing notification to Mute/Unmute or Stop, or reopen Live. On a phone-speaker output, unmuting from the notification opens the app for its feedback confirmation.
6. Stop when finished. A call/focus interruption, selected-device removal or actual-route change stops the session. It does not automatically resume after an interruption. Dismissing the task requests shutdown. A system force-stop ends the process.

If notifications are denied or hidden by device settings, the foreground service may run without lock-screen actions being visible. Reopen the app to stop or mute. Enable notifications in Android app settings if you want the controls. Test battery-management settings on your own device; no device-specific battery exemption is requested automatically.

## Architecture

`AudioSessionService` owns the audio engine, audio focus, selected-route monitoring and active-session CPU wake lock. `AudioSessionClient` binds the Activity to the service and detaches UI listeners while the Activity is stopped. Screen lock, theme recreation and opening another app no longer stop the worker merely because the Activity stopped.

The service declares microphone and mediaPlayback foreground types and their permissions, promotes itself before requesting focus, and returns START_NOT_STICKY. It starts capture only after a visible user start action. There is no boot receiver, background auto-start or automatic restart of microphone capture after process death. A background notification cannot create a fresh microphone session.

A main-thread `SessionState` separates starting, muted, live, calibrating, stopping and idle. Readiness checks are required before unmute. Calibration and stopping are muted; late readiness/calibration callbacks cannot make a stopping session live. Immutable snapshots restore current route identity and mute state when the UI reconnects. A generation guard rejects focus callbacks belonging to an earlier session.

The partial CPU wake lock has a ten-minute timeout renewed during an active session, and is released along with focus and foreground status on completion or service destruction. The screen itself is allowed to turn off. Mute keeps microphone monitoring active, as the notification states; Stop ends capture. The service and its controls are not exported to other apps.

## Test on an Android phone before relying on it

No phone/emulator is attached to this environment. The following tests are required, not claimed as completed:

- Speak to wired/USB output, lock for ten minutes, unlock, and confirm uninterrupted voice and correct mute state. Repeat with Bluetooth and a 30-minute session.
- Use notification Mute/Unmute and Stop while locked; verify the mic indicator clears after Stop and the service does not restart itself.
- Lock during startup and calibration. Startup must remain muted until routes verify, and calibration must finish muted. Rapid Stop/Start must not revive old audio.
- Change theme and rotate the phone while speaking. The existing session must remain under the same service, with a single notification and correct input/output labels.
- Disconnect USB or Bluetooth while locked; a selected-route loss must stop, with no fallback to another speaker. Test call and competing audio-focus interruptions.
- Deny notifications, then test reopening the app to mute/stop. Grant notifications and repeat lock-screen controls.
- Swipe the task away and force-stop from Android settings. Confirm capture ends and does not resume without opening the app and starting again.
- Inspect battery/wake-lock behaviour after Stop and after an error. Compare Fast/Balanced modes and report crackles, trims and measured acoustic latency separately from app-buffer durations.

## Android references

- [Foreground service types and microphone prerequisites](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Background-start and while-in-use restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Audio focus requirements](https://developer.android.com/media/optimize/audio-focus)
- [Wake-lock lifecycle guidance](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock)
