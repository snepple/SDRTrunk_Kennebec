## 2026-05-05 - NAC Filter & Max Traffic Channels
**Simplified:**
- NAC Filter: A unique code identifying a specific radio system. This is usually provided by RadioReference and tells the software which network to follow.
- Max Traffic Channels: Limits how many audio conversations can be processed at the same time. Higher numbers decode more calls simultaneously but require more CPU.

## 2026-05-05 - WACN
**Simplified:**
- WACN: A unique code identifying a large regional radio system. This is usually provided by RadioReference and tells the software which network to follow.
## 2026-05-06 - WACN (Wide Area Communication Network)
**Simplified:**
- WACN: Wide Area Communication Network (WACN) identifier. Required for cross-system P25 calls where the raw ID alone is not unique.

## 2026-05-07 - Tuner Settings (Channelizer Type & SDRPlay RSPduo Selection Mode)
**Simplified:**
- Channelizer Type: Determines how the SDR hardware processes incoming radio signals across its tuned bandwidth. Polyphase is more efficient for decoding 3 or more channels, while Heterodyne processes each channel on-demand.
- SDRPlay RSPduo Selection Mode: Configures the dual-tuner behavior of the SDRPlay RSPduo hardware (e.g., Single Tuner vs. Dual Tuner mode).

## 2026-05-08 - PPM (Parts Per Million)
**Simplified:**
- PPM (Parts Per Million): Adjusts your tuner to match the exact frequency. If your hardware gets warm and signals shift, adjust this until the signal is centered.

## 2026-05-08 - LNA Gain
**Simplified:**
- LNA Gain: The power of the signal amplifier. Increase this for distant signals, but lower it if you see a lot of static/noise.
## 2024-05-24 - [Ignore Data Calls, Ignore Unaliased TGs, Talkgroup To Assign]
**Simplified:**
- Ignore Data Calls: Skips processing data packets, focusing only on voice traffic.
- Ignore Unaliased TGs: Skips processing calls from talkgroups that have not been explicitly defined and named in your alias list.
- Talkgroup To Assign: Forces all decoded audio from this channel to use a specific talkgroup ID.
## 2024-05-14 - LNA Gain
**Simplified:**
- The power of the signal amplifier. Increase this for distant signals, but lower it if you see a lot of static/noise.
## 2024-05-23 - FFT Window Type Help
**Learning:** Adding a help icon with clear explanation for "FFT Window Type" helps beginners understand that different window types affect frequency resolution and amplitude accuracy, and recommends Hanning or Hamming as good defaults.
**Action:** Added a `createHelpIcon` to `DisplayPreferenceEditor.java` and applied it to the "FFT Window Type" `SettingsRow`.

## 2026-06-19 - P25 Talkgroup & Radio ID System Identifiers
**Simplified:**
- System: System Identifier. Combined with the WACN, uniquely identifies a P25 system.
- Radio ID: A unique number assigned to an individual radio on the network.
- Talkgroup: A unique number representing a group or channel on the network.

## 2026-06-19 - Auto-Adjust PPM
**Simplified:**
- Auto-Adjust PPM: Allow decoders to automatically measure channel frequency error and adjust the tuner PPM setting above.

## 2026-06-21 - Jargon Bridge: LNA, WACN, FFT Size
**Learning:** SDR users struggle with terminology like LNA, WACN, and FFT Size. Converting these into "User Outcomes" greatly improves the onboarding experience.
**Action:** Replaced existing tooltips with Apple HIG compliant user-friendly translations:
- **LNA Gain:** Translated from "Low Noise Amplifier power coefficient" to "The power of the signal amplifier. Increase this for distant signals, but lower it if you see a lot of static/noise."
- **WACN:** Translated from "Wide Area Communication Network ID" to "A unique code identifying a large regional radio system. This is usually provided by RadioReference and tells the software which network to follow."
- **FFT Size:** Translated from "Fast Fourier Transform bin count" to "How much detail you see in the waterfall display. Higher numbers show more detail but use more computer power."
