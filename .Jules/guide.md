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
## 2026-05-09 - Talkgroup ID (TGID)
**Simplified:**
- Talkgroup ID (TGID). Identifies a specific group of users sharing a channel on the network.

## 2026-05-09 - Radio ID (RID)
**Simplified:**
- Radio ID (RID). A unique identifier for a specific radio on the network.
