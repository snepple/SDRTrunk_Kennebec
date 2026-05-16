# Decode analog radio: NBFM, AM, and tone squelch

Not all radio traffic is digital. SDRTrunk Kennebec decodes conventional analog channels alongside its digital protocol support, making it straightforward to monitor NBFM repeaters, AM aircraft frequencies, and analog systems that embed sub-audible signaling tones. The Kennebec release adds a full post-demodulation audio filter chain to the NBFM path — including hiss reduction, a noise gate, de-emphasis, and configurable low-pass filtering — so analog audio can sound as clean as possible before it reaches your speakers or streaming destination.

## When to use analog vs. digital protocols

Choose an analog decoder type when:

- The system transmits on a fixed frequency with no trunking control channel.
- The radio traffic is FM or AM modulated (listen-only scanners or hobbyist monitoring).
- The system uses sub-audible tones (CTCSS or DCS) to gate audio to specific groups.
- You want to capture MDC-1200 unit ID metadata from a conventional analog fleet.

Choose a digital decoder type (**P25 Phase 1**, **DMR**, etc.) when the system uses digital voice encoding, even if it is non-trunked.

## Narrowband FM (NBFM)

NBFM is the most common analog mode for land mobile systems. SDRTrunk demodulates the FM carrier and passes the resulting audio through a configurable post-demod filter chain.

### Set up an NBFM channel

  **1. Open the Playlist Editor**

    Select **View → Playlist Editor** from the main menu. Choose a playlist and go to the **Channels** section.

  **2. Add a new channel**

    Click **New** and select **NBFM** from the decoder type dropdown.

  **3. Enter the frequency**

    Type the channel frequency in Hz in the **Frequency** field.

  **4. Select the channel bandwidth**

    Choose the bandwidth that matches the channel plan for your system:

    | Bandwidth | Channel spacing | Typical use |
    | --- | --- | --- |
    | 7.5 kHz (default) | 12.5 kHz | Modern narrowband land mobile |
    | 12.5 kHz | 25 kHz | Transitional narrowband |
    | 25 kHz | 50 kHz | Wideband / legacy VHF |

  **5. Configure audio filters (optional)**

    Expand **Audio Filters** in the detail editor to tune the post-demodulation filter chain. See [Audio filter options](#audio-filter-options) below.

  **6. Save and start**

    Click **Save**, then click **Play** or enable **Auto-Start**.


### Audio filter options

SDRTrunk Kennebec provides a full post-demodulation audio filter chain for NBFM. All filters are per-channel and persist in your playlist.

| Filter | Default state | Description |
| --- | --- | --- |
| **Hiss reduction** | Enabled | High-shelf filter that attenuates frequencies above a configurable corner frequency. Default: −6 dB shelf at 2,000 Hz. Increase the cut on marginal signals; lower the corner frequency if the filter cuts into speech. |
| **Low-pass filter** | Enabled | Rolls off audio above the cutoff frequency. Default cutoff: 2,800 Hz. Reduce to eliminate high-frequency noise; raise slightly if speech sounds muffled. |
| **De-emphasis** | Disabled | Applies the standard FM de-emphasis curve. Use **75 µs** for North American systems and **50 µs** for European systems. |
| **Noise gate** | Disabled | Attenuates audio when signal level drops below a threshold, reducing noise between transmissions. Default threshold: 4%, reduction: 0.8, hold time: 500 ms. |
| **Bass boost** | Disabled | Boosts low frequencies by up to +12 dB. Useful for thin-sounding audio. |
| **AGC / voice enhancement** | Disabled | Applies automatic gain control to normalize volume across transmissions. |

### Squelch settings

NBFM squelch opens when the signal noise floor drops below a threshold (indicating a carrier is present) and closes when noise rises back above it.

| Setting | Description | Default |
| --- | --- | --- |
| **Noise open threshold** | Signal noise level that opens the squelch | 0.1 |
| **Noise close threshold** | Signal noise level that closes the squelch | 0.2 |
| **Hysteresis open** | Consecutive samples below open threshold required to open | 4 |
| **Hysteresis close** | Consecutive samples above close threshold required to close | 4 |

> **Tip**
>
If the squelch opens frequently on a noisy frequency, raise the **noise open threshold** in small increments until false triggers stop. If weak signals are not opening the squelch, lower it.

**Squelch tail removal** trims the noise burst at the end of a transmission when the transmitter drops carrier. It is enabled by default and removes the last 100 ms of each transmission. You can also configure **squelch head removal** (default 0 ms) to trim CTCSS tone ramp-up noise from the start of each transmission.

## AM (Amplitude Modulation)

AM is used primarily on aviation and some HF bands. Configuration follows the same steps as NBFM, but with a simpler signal path.

  **7. Add a channel**

    Open the **Playlist Editor**, click **New**, and select **AM**.

  **8. Enter the frequency**

    Type the frequency in Hz. Aviation VHF frequencies are typically in the 118–137 MHz range.

  **9. Save and start**

    Click **Save**, then click **Play** or enable **Auto-Start**.


> **Note**
>
AM does not support sub-audible tone squelch (CTCSS/DCS). The squelch on AM channels is carrier-based only.

## CTCSS tone squelch

Continuous Tone-Coded Squelch System (CTCSS) uses a sub-audible tone in the range of 67–254.1 Hz transmitted simultaneously with voice. SDRTrunk can detect and filter on CTCSS tones on NBFM channels, passing audio only when the correct tone is present.

  **10. Select your NBFM channel**

    In the **Playlist Editor**, select the NBFM channel you want to configure.

  **11. Enable tone filtering**

    Toggle **Tone Filter Enabled** to on in the **Decoder** configuration section.

  **12. Add a CTCSS tone filter**

    Click **Add Tone Filter**, set the type to **CTCSS**, and select the tone frequency from the dropdown — for example, `100.0 Hz` or `127.3 Hz`.

  **13. Save the channel**

    Click **Save**. SDRTrunk will now pass audio only when the configured CTCSS tone is detected.


> **Warning**
>
When tone filtering is enabled and the tone filter list is empty, **no audio will pass**. Always add at least one tone filter entry before enabling the feature.

You can add multiple CTCSS entries to a single channel. Audio passes when **any** of the configured tones is detected — useful if a repeater is shared by multiple agencies on different tones.

## DCS digital coded squelch

Digital Coded Squelch (DCS) uses a 134 bps continuous digital code transmitted below the voice audio. It functions identically to CTCSS from a configuration perspective.

  **14. Enable tone filtering**

    Toggle **Tone Filter Enabled** to on in the channel's Decoder settings.

  **15. Add a DCS tone filter**

    Click **Add Tone Filter**, set the type to **DCS**, and select the DCS code — for example, `023` or `445`.

  **16. Save**

    Click **Save**. Audio will only pass when the configured DCS code is received.


> **Tip**
>
CTCSS and DCS tone filters can be mixed on the same channel. SDRTrunk opens the squelch when **any** configured code is detected, letting you monitor a shared frequency where different agencies use different squelch codes.

## MDC-1200 metadata decoding

MDC-1200 is a 1,200 bps FSK signaling protocol that many analog FM radios transmit as a short burst at the start of each transmission. It carries unit IDs, emergency alerts, paging, and status messages. SDRTrunk decodes MDC-1200 automatically on any NBFM channel — no additional configuration is needed.

The following MDC-1200 message types are decoded and appear in the event log:

| Message type | Description |
| --- | --- |
| ANI | Automatic Number Identification — the transmitting radio's unit ID |
| Emergency | Emergency button press from a radio |
| Acknowledge | Acknowledgment from the system or dispatcher |
| Paging | Selective call to a specific unit or group |
| Status | Status message from a radio |

To assign friendly names to MDC-1200 unit IDs, add alias entries with the **Protocol** set to `MDC-1200` and the identifier type set to **Talkgroup**, entering the numeric unit ID in the **Identifier** field.

> **Note**
>
MDC-1200 and Fleetsync II both layer over NBFM carriers. You do not create a separate channel entry for them — they are automatically decoded alongside the voice audio on any NBFM channel where they appear.
