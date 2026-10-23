# Synthetic Overlord — reference analysis and implementation

## What was analysed

A creator-supplied MP3 excerpt, approximately 13.94 seconds, stereo at 44.1 kHz. The file was used locally as a sound reference; it is not embedded in the app or published in this repository. The analysis does not establish the original production signal chain or the actor's isolated pitch.

A mono-downmix spectral analysis at 16 kHz showed strong low-frequency energy: approximately 90% below 120 Hz, 5.5% at 120–400 Hz, 2.9% at 400 Hz–2 kHz and less than 1% above 2 kHz. Stereo correlation was about 0.67. These measurements describe the complete supplied mix, including any score, effects, mastering and channel ambience. They are not measurements of clean speech alone. Low periodicity around 77–107 Hz in selected frames is especially vulnerable to soundtrack contamination and was not treated as an isolated vocal fundamental or a target for automatic pitch matching.

The design interpretation is a dark mechanical character with a retained intelligible vocal core. Rather than copying the whole mix's heavy bass, the preset uses restrained low EQ, a modest pitch reduction and controllable parallel texture. A short tail suggests space without turning every word into a long repeat. Exact perceptual matching remains subjective and requires the creator's listening feedback with their own voice.

## Original DSP design

1. Existing rumble filtering, tone, EQ and light voice care.
2. Existing approximate −2-semitone dual-delay pitch shift and 18% grit.
3. New **MetallicVoice** stage: three parallel damped feedback combs at approximately 6.7, 10.9 and 16.3 ms. Mixed feedback signs and bounded feedback below 0.4 produce multiple resonances rather than one dominant ringing pitch.
4. A dual 47/89 Hz ring-modulated layer, combined with a retained dry component. The metallic blend eases over 5 ms; its mix contains 64% direct signal, 12% of the summed comb taps and 30% of the filtered modulation layer before the user blend.
5. Existing speech compression, light 70 Hz robot modulation, 5% echo at 100 ms and causal peak limiting.

The metallic stage uses fixed buffers, per-sample causal processing and no FFT, look-ahead or per-block array allocation. Its direct component adds no deliberate buffering, while its wet reflections are intentionally delayed. The full preset inherits the existing pitch shifter's 40 ms modulation window; this is not a guarantee of 40 ms total latency. Echo repeats and Bluetooth/hardware delay are additional.

Metallic layers is a separate Studio control, stored with custom settings and restored after reopening the app. Synthetic Overlord is appended at index 16 so indices 0–15 retain their meaning. Natural, Lecture, Vigilante and other existing presets retain their prior tuning and use zero metallic blend. Mute and bypass clear effect state; the uploaded MP3 is not required at runtime.

## Verification and limits

All five Java regression suites passed. New coverage checks preset metadata, bounded/NaN parameters, zero-blend and bypass unity, dry impulse onset, expected modulation sidebands using a windowed spectral measurement, processing-block invariance, damped impulse-tail stability, stereo equality, output limits and mute clearing at 8, 16, 32, 44.1, 48, 96 and 192 kHz. Existing selector, audio, voice-care and buffer-tuning suites also pass.

The Android SDK 35 build, application identity, Android 8+ minimum, launcher class, alignment and APK signatures were checked. This is **MicBridge Preview 0.6.0**, a separate package with a separate test certificate. It can coexist with the earlier beta; it is not an in-place upgrade. No phone/emulator session, subjective hardware audition or acoustic latency measurement was performed here.

Try the same phrase with Natural, Robot, Vigilante and Synthetic Overlord at matched playback volume. Compare a wired/USB output against Bluetooth. Listen for consonant clarity, harshness, excessive bass, feedback and distracting delay. Email your observations to [jerrychiengchintung@gmail.com](mailto:jerrychiengchintung@gmail.com?subject=MicBridge%20Synthetic%20Overlord%20feedback).
