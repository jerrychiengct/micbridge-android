# Privacy

MicBridge 0.7.0 Preview processes microphone audio locally for live output. It does not create microphone recordings, send audio to a server, include advertising or analytics, or request internet access. Theme, workspace, effect settings and gain preferences are stored locally on the phone. Noise calibration measures a background level for the current session; it does not save an audio file.

Android microphone permission is used for capture. Nearby devices permission on Android 12+ is used to access connected Bluetooth audio-device information. Bluetooth pairing is performed in Android settings. Foreground-service microphone/media-playback permissions support the live session; notification permission enables notification controls. No accessibility, overlay, contacts, location or internet permission is requested.

GitHub / manufacturer / Ko-fi links and email buttons open another app. The external app or service's own privacy practices apply. Ko-fi handles any support payment externally; MicBridge receives no payment details. An explicitly started session can continue when the screen locks or another app opens. An ongoing foreground-service notification identifies microphone activity and offers Mute/Stop controls when notifications are allowed. A partial CPU wake lock is held only during an active session and is released on shutdown. MicBridge does not auto-start the microphone at boot or restore a killed audio session. Calls, selected-route loss, Stop and dismissing the app end the session. Donation / collaboration email buttons open a draft for the user to send; MicBridge sends no email automatically.

Questions: [jerrychiengchintung@gmail.com](mailto:jerrychiengchintung@gmail.com).
