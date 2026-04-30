# Decode analog radio: NBFM, AM, and tone squelch modes

> Monitor conventional analog radio in SDRTrunk Kennebec including NBFM, AM, CTCSS tone squelch, DCS coded squelch, and MDC-1200 metadata.

Not all radio traffic is digital. SDRTrunk Kennebec decodes conventional analog channels alongside its digital protocol support, making it straightforward to monitor NBFM repeaters, AM aircraft frequencies, and analog systems that embed signaling tones or subaudible codes. The Kennebec release adds post-demodulation audio filters to the NBFM path — including hiss reduction, a noise gate, de-emphasis, and configurable low-pass filtering — so analog audio can sound as clean as possible before it reaches your speakers or streaming destination.

## NBFM

Narrowband FM (NBFM) is the most common analog radio mode for land mobile systems. SDRTrunk demodulates the FM carrier and passes the resulting audio through a configurable post-demod filter chain.

### Set up an NBFM channel

  ### Add a channel in the Playlist Editor
    Open **View → Playlist Editor**, select a playlist, and click **Add Channel**.


  ### Set the decoder to NBFM
    In the channel configuration panel, set the **Decoder** drop-down to **NBFM**.


  ### Enter the frequency
    Type the channel frequency in Hz in the **Frequency** field.


  ### Select the channel bandwidth
    Choose the appropriate bandwidth for the system:

    | Bandwidth         | Channel spec             | Typical use                   |
    | ----------------- | ------------------------ | ----------------------------- |
    | 7.5 kHz (default) | 12.5 kHz channel spacing | Modern narrowband land mobile |
    | 12.5 kHz          | 25 kHz channel spacing   | Transitional narrowband       |
    | 25 kHz            | 50 kHz channel spacing   | Wideband / legacy VHF         |


  ### Adjust audio filter settings (optional)
    Expand **Audio Filters** to configure the post-demod filter chain. See [audio filter options](#audio-filter-options) below.


  ### Save and start
    Click **Save**, then click **Play** or enable **Auto-Start**.


### Audio filter options

SDRTrunk Kennebec adds a full post-demodulation audio filter chain to the NBFM path. All filters are per-channel and persist in your playlist.

  <Accordion title="Hiss reduction" defaultOpen={true}>
    A high-shelf filter that attenuates high frequencies above a configurable corner frequency. Enabled by default with a **−6 dB** shelf cut at **2,000 Hz**.

    * **Enabled by default:** Yes
    * **Shelf gain range:** −12 dB to 0 dB
    * **Corner frequency range:** 500 Hz to 3,800 Hz

    Increase the cut (more negative dB) to reduce hiss on marginal signals. Lower the corner frequency if the filter cuts into intelligible speech.


  ### Low-pass filter
    Rolls off audio above the cutoff frequency. Enabled by default at **2,800 Hz**. Reduce the cutoff to eliminate high-frequency noise; raise it slightly if speech sounds muffled.

    * **Enabled by default:** Yes
    * **Cutoff range:** 3,000–4,000 Hz recommended


  ### De-emphasis
    Applies the standard FM de-emphasis curve to compensate for transmitter pre-emphasis. Use **75 µs** for North American systems and **50 µs** for European systems. Disabled by default.


  ### Noise gate
    Attenuates audio when the signal level drops below a threshold, reducing noise between transmissions. Disabled by default.

    * **Threshold:** 0–100% (gate opens when level exceeds threshold; default 4%)
    * **Reduction:** 0.0–1.0 (amount of attenuation when gate is closed; default 0.8 = 80%)
    * **Hold time:** Duration the gate stays open after voice stops (default 500 ms)


  ### Bass boost
    Boosts low frequencies by up to +12 dB. Disabled by default. Useful for improving the perceived warmth of thin-sounding audio.


  ### AGC / voice enhancement
    Applies automatic gain control to normalize volume across transmissions. Disabled by default.


### Squelch settings

NBFM uses a noise-based squelch that opens when the signal noise floor drops below a threshold (indicating a carrier is present) and closes when noise rises back above it.

| Setting               | Description                                                           | Default |
| --------------------- | --------------------------------------------------------------------- | ------- |
| Noise open threshold  | Signal noise level that opens the squelch                             | 0.1     |
| Noise close threshold | Signal noise level that closes the squelch                            | 0.2     |
| Hysteresis open       | Number of consecutive samples below open threshold required to open   | 4       |
| Hysteresis close      | Number of consecutive samples above close threshold required to close | 4       |

> **Tip:**
  If the squelch opens frequently on a noisy frequency, raise the **noise open threshold** in small increments until the false triggers stop. If weak signals are not opening the squelch, lower it.

**Squelch tail removal** trims the noise burst that occurs at the end of a transmission when the transmitter drops carrier. It is enabled by default and removes the last 100 ms of each transmission. You can also configure **squelch head removal** (default 0 ms) to trim CTCSS tone ramp-up noise from the start of each transmission.

## AM

AM (Amplitude Modulation) is used primarily on aviation and some HF bands. Configuration follows the same steps as NBFM.

  ### Add a channel
    Open the Playlist Editor, click **Add Channel**.


  ### Set the decoder to AM
    In the **Decoder** drop-down, select **AM**.


  ### Enter the frequency
    Type the frequency in Hz. Aviation VHF frequencies are typically in the 118–137 MHz range.


  ### Save and start
    Click **Save**, then **Play** or enable **Auto-Start**.


> **Note:**
  AM does not support sub-audible tone squelch (CTCSS/DCS). The squelch on AM channels is carrier-based.

## CTCSS tone squelch

Continuous Tone-Coded Squelch System (CTCSS) uses a sub-audible tone (67–254.1 Hz) transmitted simultaneously with voice. SDRTrunk can detect and filter on CTCSS tones on NBFM channels.

### Enable CTCSS squelch on an NBFM channel

  ### Open the channel's Decoder tab
    In the Playlist Editor, select the NBFM channel and go to the **Decoder** configuration section.


  ### Enable tone filtering
    Toggle **Tone Filter Enabled** to on.


  ### Add a CTCSS tone filter
    Click **Add Tone Filter**, set the type to **CTCSS**, and select the tone frequency from the drop-down (for example, 100.0 Hz or 127.3 Hz).


  ### Save the channel
    Click **Save**. SDRTrunk will now pass audio only when the configured CTCSS tone is detected on the carrier.


> **Warning:**
  When tone filtering is enabled and the tone filter list is empty, **no audio will pass**. Always add at least one tone filter entry before enabling the feature.

You can add multiple CTCSS entries to a single channel. Audio passes when **any** of the configured tones is detected — useful if a repeater is used by multiple agencies on different tones.

## DCS digital coded squelch

Digital Coded Squelch (DCS) uses a 134 bps continuous digital code transmitted below voice audio. It functions identically to CTCSS from a configuration perspective.

  ### Enable tone filtering on the NBFM channel
    Toggle **Tone Filter Enabled** to on in the channel's Decoder settings.


  ### Add a DCS tone filter
    Click **Add Tone Filter**, set the type to **DCS**, and select the DCS code (for example, `023` or `445`).


  ### Save
    Click **Save**. Audio will only pass when the configured DCS code is received.


> **Tip:**
  CTCSS and DCS tone filters can be mixed on the same channel. This lets you monitor a shared frequency where different agencies use different squelch codes — SDRTrunk opens the squelch when any configured code is detected.

## MDC-1200 metadata decoding

MDC-1200 is a 1,200 bps FSK signaling protocol that many analog FM radios transmit as a burst at the start of each transmission. It carries unit IDs, emergency alerts, paging, and status messages. SDRTrunk decodes MDC-1200 automatically on any NBFM channel without additional configuration.

The following MDC-1200 message types are decoded and appear in the event log:

| Message type | Description                                                        |
| ------------ | ------------------------------------------------------------------ |
| ANI          | Automatic Number Identification — the transmitting radio's unit ID |
| Emergency    | Emergency button press from a radio                                |
| Acknowledge  | Acknowledgment from the system or dispatcher                       |
| Paging       | Selective call to a specific unit or group                         |
| Status       | Status message from a radio                                        |

To assign friendly names to MDC-1200 unit IDs, add alias entries with the **Protocol** set to `MDC-1200` and the identifier type set to **Talkgroup**, entering the numeric unit ID in the **Identifier** field.

> **Note:**
  MDC-1200 and Fleetsync both layer over NBFM carriers. You do not create a separate channel entry for them — they are automatically decoded alongside the voice audio on any NBFM channel where they appear.