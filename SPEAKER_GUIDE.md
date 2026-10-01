# Portable speaker connection guide

The app supports Android audio routes, not a fixed model whitelist. The examples below were researched from manufacturer pages on 1 October 2026. A Bluetooth playback connection is the expected route; no listed speaker has been physically certified with this app. The list is deliberately presented as examples rather than every speaker ever manufactured. Other brands/models can appear automatically after pairing. Malaysian stock and distribution vary, especially where a global manufacturer page is linked.

| Brand | Portable model examples | Manufacturer source |
|---|---|---|
| Xiaomi | Sound Pocket, Sound Outdoor, Xiaomi Bluetooth Speaker | [Malaysia Sound Pocket](https://www.mi.com/my/product/xiaomi-sound-pocket/), [Sound Outdoor](https://www.mi.com/my/product/xiaomi-sound-outdoor/), [Bluetooth Speaker](https://www.mi.com/my/product/xiaomi-bluetooth-speaker/) |
| Tribit | StormBox Micro 2, StormBox 2, StormBox Flow, StormBox Blast, XSound Go, XSound Plus 2 | [Tribit model guide](https://tribit.com/blogs/news/tribit-speaker-lineup-comparison-guide), [StormBox collection](https://tribit.com/collections/stormbox) |
| JBL | Go 4, Clip 5, Charge 6, Xtreme | [Malaysia portable speakers](https://www.jbl.com.my/bluetooth-portables), [Clip 5](https://www.jbl.com.my/bluetooth-portables/CLIP-5.html) |
| Sony | SRS-XB100, ULT FIELD 1, 3, 5, 7 | [Malaysia XB100](https://www.sony.com.my/wireless-speakers/products/srs-xb100), [ULT FIELD 1](https://www.sony.com.my/wireless-speakers/products/ult-field-1) |
| Soundcore / Anker | Motion 100, Motion 300, Motion X600 | [Motion 300](https://www.soundcore.com/products/motion-300-speaker-a3135011), [Motion 100 family page](https://beta.soundcore.com/products/motion-100-a3133011) |
| Edifier | MP85 | [Manufacturer MP85](https://www.edifier.com/global/product-detail?id=157) |
| Marshall | Emberton III | [Malaysia Emberton III](https://www.marshall.com/my/en/product/emberton-iii-lunar-new-year-edition-2026) |
| Bose | SoundLink Flex (2nd Gen) | [Manufacturer product page](https://www.bose.com/p/speakers/bose-soundlink-flex-portable-speaker-2nd-gen/SLFLXII-SPEAKERWIRELESS.html) |
| Ultimate Ears | WONDERBOOM 4, BOOM 3 | [Malaysia speaker catalogue](https://www.ultimateears.com/en-my/shop/c/wireless-speakers) |
| LG | xboom Bounce, xboom Grab | [Malaysia Bounce](https://www.lg.com/my/speakers/xboom/bounce/), [Grab](https://www.lg.com/my/speakers/xboom/grab/) |
| Creative | MUVO Go, MUVO Flex | [Malaysia MUVO Go](https://my.creative.com/p/audio-enthusiasts/creative-muvo-go), [MUVO Flex](https://my.creative.com/p/speakers/creative-muvo-flex) |
| Harman Kardon | Luna, Luna 2 | [Malaysia portable wireless speakers](https://my.harmankardon.com/bluetooth-docks) |

## Connection procedure

1. Put the speaker into Bluetooth pairing mode using its own controls.
2. Pair through Android settings and enable **Media audio**.
3. If both normal and `LE-` names appear, choose the normal media entry unless the manufacturer explicitly supports an Android LE Audio playback route. BLE control connectivity does not imply audio playback.
4. Play ordinary music to confirm the route. Stop that music, open MicBridge, Refresh and select the output.
5. Start muted, verify the microphone input, then unmute at low volume.

Manufacturer pairing, party linking, stereo modes and proprietary control apps do not need to be reimplemented by MicBridge. The app selects one Android output route. Multiple physical speakers may play if the manufacturer manages linking behind that route; timing and compatibility remain device-specific.

For low-delay speaking or singing, a wired audio interface or direct microphone-to-PA connection is generally the more suitable architecture. Bluetooth live monitoring can be noticeably delayed even when ordinary music playback is excellent.

## Android routing references

- [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
- [AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)
- [AudioRouting: preferred devices versus actual routes](https://developer.android.com/reference/android/media/AudioRouting)
- [Audio latency](https://developer.android.com/ndk/guides/audio/audio-latency)
