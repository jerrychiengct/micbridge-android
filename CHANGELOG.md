# Changelog

## 0.5.1 beta — 4 October 2026

- Public testing invitation for Android-compatible microphones and speakers across brands, including generic devices and wireless microphones with compatible USB receivers.
- About feedback button prepares an email to jerrychiengchintung@gmail.com with hardware, audio-quality, latency and feature-request fields; public repository access wording corrected.
- Shared native sample-rate preference and suitable native burst sizes in Fast mode, with fixed-format and common-rate fallbacks.
- Bounded capture queue and nonblocking backlog drain after speaker stalls; retain recent speech, count discarded frames and ease recovery over 2 ms. Overload can skip audio and must be tested on hardware.
- Output buffer capacity reserved for recovery; Fast mode grows on underruns and cautiously reduces after ten stable seconds, retaining a higher floor after a failed probe. Android 12+ start threshold follows the effective buffer.
- Causal sample-based peak limiting with 80 ms release, independent of processing-block boundaries; a late peak no longer attenuates earlier speech in its block.
- Sample-rate-aware noise expansion with 1 ms opening / 20 ms closing, DC-offset-aware noise calibration, and cached settled EQ coefficients.
- Native block duration, input/output buffer sizes, underruns and cumulative backlog trimming exposed in Live diagnostics.
- Four regression suites passed, including randomized queue overload, tuning hysteresis and limiter invariance at up to 192 kHz. SDK 35 build and signatures verified; version code 8, same supplied beta certificate.
- End-to-end delay, acoustic quality, Android runtime routing and rendered UI still require physical-device testing. No universal-hardware or measured latency guarantee.

## 0.5.0 beta — 2 October 2026

- Refined native Android interface with neutral grouped surfaces, charcoal primary actions, mint accents and clearer type hierarchy.
- Original vector outline navigation and action icons, press feedback and inset device selectors with dropdown affordances.
- Compact featured presets plus the complete 16-preset collection in a picker.
- Separate sound, equaliser, expandable creative and voice-care cards; creative controls reveal for relevant presets.
- Less crowded Live page with detailed routing/latency explanations available on demand.
- Real session-state badge, clearer calibration state and per-tab scroll restoration.
- Accessible slider value labels and text-sized controls.
- Signed beta upgrade APK, version code 7. Design references are clearly labelled; physical UI and audio validation remain required.

## 0.4.2 beta — 2 October 2026

- One snapshot for input/output discovery, repeated-ID removal and stable device-ID selection.
- Explicit selection prompts; disconnecting a choice never silently selects another device.
- Fresh availability check before starting, coalesced connection callbacks and cleared stale lists after permission loss.
- Filtered hands-free, earpiece and virtual routes; retained generic Bluetooth media, LE Audio, built-in, USB and wired support. Distinct same-name ports are numbered.
- Wrapping device labels, larger-text button sizing, side/cutout insets, accessibility announcements and selected navigation state.
- Explicit audio-worker start failure and calibration/unmute state guards.
- New selector regression suite; existing audio suites rerun. Signed upgrade APK uses version code 6.
- Hardware and rendered UI validation remain open; this is a beta, not a production certification.

## 0.4.1 beta — 2 October 2026

- Support on Ko-fi button in About: https://ko-fi.com/jerrychiengct.
- Ko-fi links in the README and support guide, with GitHub funding configuration.
- External-service privacy information; donation / collaboration email remains available.

## 0.4.0 beta — 2 October 2026

- Voice care with soft noise expansion, de-essing, bounded level assist and muted noise-floor calibration.
- Broader sample-rate negotiation and mono / stereo capture support.
- Fast 5 ms processing blocks, smaller requested buffers, underrun recovery and live diagnostics; Balanced mode retained.
- 16 original presets, including Studio Speech, Broadcast, Warm Narrator, Crisp Presenter, Soft Spoken, Small Room, Cyber Pilot and Cinematic.
- Generic Bluetooth-speaker guidance.
- About with creator credit, GitHub links and donation / collaboration email buttons.

## 0.3.0 beta — 1 October 2026

- Built-in and connected microphone selection, expanded physical outputs and brand-neutral wording.
- Independent bass, mid and treble controls.
- Lecture and Vigilante presets, grit and speech dynamics controls.
- Material-style adaptive and themed app icons.

## 0.2.0 beta — 1 October 2026

- Redesigned Live / Voice Studio / Speakers interface and live level history.
- Six initial presets with pitch, tone, robot and echo controls.
- Offline guide with portable-speaker examples from 12 brands.

## 0.1.0 beta — 1 October 2026

- Initial microphone-to-speaker test build with actual-route verification, start-muted playback, gain and limiting.

## Repository presentation — 2 October 2026

- Branded project overview, concise download / setup instructions and organised documentation.
- Contribution guidance, bug / feature / hardware-report forms and a pull-request template.
- Standalone test runner for contributors. Application version remains 0.4.0.
