# Original voice presets — MicBridge 0.6.0 Preview

This collection uses MicBridge's own DSP code and tuning values. No paid preset pack, commercial engine, AI voice model or proprietary plug-in is included. The professional guidance below informed the choice of processors; these settings are our original implementation, not extracted iZotope presets or claims of equivalent studio quality.

| Studio preset | Intent | Pitch | Bass / mid / treble | Speech compression | Noise / de-ess | Level assist |
|---|---|---:|---|---|---|---|
| Studio Speech | Balanced spoken dialogue | 0 | −1 / +2 / +1 dB | On | 35% / 40% | On |
| Broadcast | Warm radio presence, 8% grit | 0 | +2 / +2 / 0 dB | On | 40% / 45% | On |
| Warm Narrator | Warm storytelling | 0 | +2 / +1 / −1 dB | On | 30% / 35% | On |
| Crisp Presenter | Speech presence | 0 | −3 / +3 / +2 dB | On | 40% / 55% | On |
| Soft Spoken | Assist a quiet voice | 0 | −1 / +2 / +1 dB | On | 30% / 25% | On |
| Small Room | Reduce boom and harshness | 0 | −4 / +2 / −2 dB | On | 55% / 50% | Off |
| Cyber Pilot | Sci-fi texture, 45% robot, 15% grit | −2 | −2 / +2 / +1 dB | On | 35% / 30% | Off |
| Cinematic | Deep trailer texture, 20% grit | −3 | +3 / +1 / −1 dB | On | 35% / 35% | Off |

Cyber Pilot has 12% echo at 150 ms; Cinematic has 10% at 200 ms. The first six studio presets preserve natural pitch and leave echo off. Lecture uses −2 / +3 / +1 dB EQ, 45% noise, 35% de-ess and level assist. Vigilante remains the deep, gritty Batman-inspired effect. Natural is neutral. All controls can be adjusted or bypassed.

For speech use Lecture or Studio Speech first, and calibrate while muted and quiet. Settings are starting points: voices, rooms, adapters and microphones differ. De-essing and noise expansion can soften consonants if excessive; reduce their sliders if speech sounds dull or clipped between words. Level assist is capped to avoid unlimited noise amplification, but microphone / speaker separation and low physical starting volume remain necessary.

## Processing references

- iZotope, *Crafting a basic vocal chain* (21 September 2023): EQ, compression, de-essing and tasteful delay; specifically supports the choice of processors, not our numeric tunings: https://www.izotope.com/community/blog/crafting-a-basic-vocal-chain
- iZotope, *How to Edit Podcasts*: dialogue clarity, correcting boom / harshness and dynamics: https://www.izotope.com/community/blog/how-to-edit-podcasts
- Android AudioTrack: smaller write buffers trade latency for underrun risk; requested sizes may be adjusted: https://developer.android.com/reference/android/media/AudioTrack#setBufferSizeInFrames(int)
- Android AudioRecord: minimum capture size is not a guarantee of stable or low-latency hardware capture: https://developer.android.com/reference/android/media/AudioRecord#getMinBufferSize(int,int,int)

Researched 2 October 2026. Hardware / emulator validation is still required.

## Synthetic Overlord — new preset 17

Ultron-inspired metallic character processing, applied to the user's own voice. Select **Studio → Try Synthetic Overlord**, or choose it in the full preset collection. This is an original sound effect, not an exact actor/character voice, a trained voice model or a soundboard of the reference recording.

| Control | Starting value |
| --- | --- |
| Pitch / tone | −2 semitones / −1 dB |
| Bass / mid / treble | +1 / +2 / +1 dB |
| Grit / robot | 18% / 18% |
| Metallic layers | 65%; adjustable from 0 to 100% |
| Echo | 5% at 100 ms |
| Noise / de-essing | 20% / 20% |
| Speech compression / quiet-voice assist | On / off |

Start at low volume. For a lighter effect, reduce Metallic layers and Robot texture; set Pitch to 0 to remove the pitch shifter's delay. Set Echo blend to 0 to remove repeats. Dry speech remains within the metallic processor, but the preset as a whole still uses the existing approximate pitch shifter. Prefer Lecture or Studio Speech when natural lecture delivery matters more than a character effect. [Reference analysis and implementation](docs/SYNTHETIC_OVERLORD.md)
