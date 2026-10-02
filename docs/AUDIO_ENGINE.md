# Audio engine

Audio is processed locally. The app first tries rates shared by input and output, then advertised input rates and common Android conversion fallbacks (48, 44.1, 32, 24, 16, 96, 22.05 and 8 kHz). Supported rates are limited to 8–192 kHz. Mono and stereo capture configurations are attempted:

`Selected microphone → rumble filter / tone / 3-band EQ → voice care → pitch → grit → speech compressor → robot modulation → echo → gain / limiter → selected output`

Natural mode bypasses creative stages with neutral settings. Effects can also be bypassed with the switch. Pitch uses two interpolated, crossfaded delay taps with a 40 ms window. It is an approximate creative effect and can produce grain or warble; it is not a professional formant-preserving pitch processor. Robot texture uses 70 Hz ring modulation. Echo uses a feedback delay with selectable spacing and blend. Tone is a crossover-based warm/bright tilt. Separate EQ bands use a 180 Hz low shelf, a 1.5 kHz peaking mid filter (Q 0.8), and a 4 kHz high shelf. Live EQ changes ease across blocks. Grit blends a soft tanh saturation stage with the original signal. Speech leveling uses a −18 dBFS envelope threshold, roughly 2.5:1 compression, 10 ms attack and 180 ms release, without automatic makeup gain. Vigilante combines −5 semitones, a warmer tone, bass boost and 35% grit; it is an approximation, not an exact actor or character imitation. The final block limiter bounds digital output to roughly 90% full scale. Mute clears delayed effect tails and filter / voice-care state inside the app.

Voice care uses a calibrated or conservative default noise floor for a soft downward expander. This reduces quiet background between phrases, not noise mixed into speech. Level assist avoids floor-only input and bounds gain to 0.5–2.0×. De-essing reduces the upper band when its envelope dominates; it is best-effort software processing, not AI restoration. It adds no intentional look-ahead buffer. These controls are optional, and Natural or Effects bypass preserves neutral processing. Heavy hiss, clipping, poor mic placement or a defective adapter cannot be fixed regardless of hardware quality.

Bluetooth buffering cannot be removed by this app. Pitch processing and echo alter timing further. Mute/Stop prevent new audio from being submitted, but already buffered Bluetooth audio may continue briefly. If acoustic feedback occurs, lower the speaker's physical volume or power it off. The limiter cannot prevent feedback or repair clipping at the microphone transmitter.


See [Testing](TESTING.md) for validation scope and [Installation](INSTALLATION.md) for first use.

## Connected-device identity

Both selectors are built from a single `AudioManager.getDevices(GET_DEVICES_ALL)` snapshot. Source/sink flags and supported route categories determine each list. Repeated Android IDs are removed; product names are never used to merge distinct endpoints. Same-name ports are numbered, and media versus LE Audio profiles are labelled separately. Hands-free SCO, phone earpiece, safe-speaker aliases, unknown and virtual system endpoints are excluded. This engine does not establish a Bluetooth communication session.

Rows carry their own device ID. Selection restoration uses that ID; a missing ID returns to an explicit prompt. Before starting, the app takes a fresh snapshot and resolves both selections again. AudioRecord/AudioTrack preferences must be accepted, then their actual routed device IDs must match before voice is enabled. A changed route stops the session. Addresses and device choices are not persisted.

Android references: [AudioManager.getDevices](https://developer.android.com/reference/android/media/AudioManager#getDevices(int)), [AudioDeviceInfo](https://developer.android.com/reference/android/media/AudioDeviceInfo), [preferred versus actual routing](https://developer.android.com/reference/android/media/AudioRouting). A preferred device does not guarantee the actual route; hardware validation is still necessary.
