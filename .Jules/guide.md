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
