# Contributing to MicBridge

Thank you for helping improve live speech and creative voice tools. Android developers, audio engineers, designers and beta testers are welcome.

## Start a conversation

This is an independent private beta project by Jerry Chieng Chin Tung. If you have repository access, use the issue forms for bugs, ideas or hardware reports. Otherwise email **[jerrychiengchintung@gmail.com](mailto:jerrychiengchintung@gmail.com?subject=MicBridge%20collaboration)** with your proposed contribution. Repository access is not granted automatically.

Describe the problem or user benefit, your proposed approach and the kind of help you can provide. Discuss large architectural changes before implementing them.

## Development workflow

1. Read [Build and test](docs/BUILDING.md) and the [Audio engine](docs/AUDIO_ENGINE.md) reference.
2. Work on a focused branch. Make a small, reviewable change.
3. For DSP or format changes, run `./tests/run-tests.sh` and add meaningful coverage for the changed behaviour.
4. For Android behaviour, report the actual phone / Android / microphone / speaker setup and test result. Do not claim hardware validation from digital tests alone.
5. Include genuine screenshots for interface changes where a phone or emulator is available.
6. Open a pull request describing the problem, resulting behaviour and validation. Flag device tests still outstanding.

## Project expectations

- Keep processing on device; preserve the app's recording-free behaviour.
- Keep route checks, start-muted behaviour and device-disconnection handling intact.
- Avoid extra delay in the speech path. State any timing trade-off clearly.
- Treat generic and named devices by Android audio capabilities, not a brand whitelist.
- Use original assets and preset tunings. Discuss new third-party dependencies and their requirements with the creator first.
- Never commit signing keys, credentials, personal recordings or unrelated personal data.
- Keep discussion respectful, specific and focused on the work.

For donations or personal contact, see [Support](SUPPORT.md).
