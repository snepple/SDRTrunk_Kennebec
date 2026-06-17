# Decode DMR conventional and trunked radio channels

Digital Mobile Radio (DMR) is an ETSI open standard widely used by commercial fleets, utilities, and some public safety agencies. SDRTrunk Kennebec decodes both conventional DMR channels and DMR Tier III trunked systems, extracting talkgroup IDs, radio IDs, and call metadata at 9,600 bps over a 12.5 kHz carrier. This page covers the key differences between conventional and trunked DMR and walks through configuration in the **Playlist Editor**.

## DMR fundamentals

DMR uses TDMA (Time Division Multiple Access) to place **two independent voice timeslots** on a single 12.5 kHz carrier. SDRTrunk creates a separate decoder instance for each timeslot, so you see Timeslot 1 and Timeslot 2 activity independently in the event log.

| Property | Value |
| --- | --- |
| Modulation | 4FSK |
| Bit rate | 9,600 bps |
| Channel spacing | 12.5 kHz |
| Timeslots per carrier | 2 |
| Talkgroup support | Yes |
| Radio ID support | Yes |

## Audio decoding and AMBE+2

DMR uses the AMBE+2 voice codec. As with P25, audio decoding requires the **JMBE** library, which is **not bundled** with SDRTrunk Kennebec due to patent considerations. Install JMBE separately before expecting audio output. Without it, SDRTrunk still decodes all DMR signaling and displays activity in the event log.

## DMR tiers explained

### Tier I and Tier II — Conventional

On a conventional DMR channel (Tier I simplex or Tier II repeater-based), radios transmit on a fixed frequency with no control channel. SDRTrunk monitors the carrier and decodes any DMR activity it detects on either timeslot.

Use the **DMR** decoder type in the **Playlist Editor** and set **Frequency** to the channel's fixed frequency.

### Tier III — Trunked

DMR Tier III is the trunked variant, commonly deployed on Motorola MOTOTRBO Capacity Plus, Linked Capacity Plus, and Connect Plus systems. The system broadcasts a control channel that grants calls to specific traffic channel frequencies. SDRTrunk decodes the control channel and automatically follows traffic channel grants.

Use the **DMR** decoder type and point it at the control channel frequency. SDRTrunk detects Tier III operation automatically.

On MotoTRBO systems that use fixed timeslot-to-frequency mapping (MOTOTRBO conventional IP Site Connect), you can configure a **Timeslot Map** in the decoder settings to tell SDRTrunk which frequencies correspond to which timeslots.

## Set up a DMR channel

**1. Open the Playlist Editor**

Select **View → Playlist Editor** from the main menu. Choose or create a playlist.

**2. Add a new channel**

Click **New** in the Channels toolbar and select **DMR**. A new channel row appears and the detail editor opens.

**3. Enter the frequency**

Type the channel frequency in Hz in the **Frequency** field. For a trunked Tier III system, enter the control channel frequency.

**4. Configure decoder options**

Expand the **Decoder Options** section and set the options appropriate for your system:

| Option | Description |
| --- | --- |
| **Traffic Channel Pool Size** | Maximum simultaneous calls to decode. Reduce on constrained hardware. |
| **Ignore Data Calls** | Enabled by default. Disable only if you need to decode data traffic grants. |
| **Ignore Unaliased Talkgroups** | When enabled, only talkgroups with an alias entry are decoded. Useful for filtering busy systems. |
| **Ignore CRC Checksums** | Disables CRC validation. Enable only on poor-signal sites where valid calls are being dropped due to checksum errors. |
| **Use Compressed Talkgroups** | Enable for Hytera Tier III systems that use the compressed talkgroup format from ETSI 102 361-2 Annex C. |

**5. Assign system and site names**

Fill in the **System** and **Site** fields for clear labeling in the **Now Playing** view and in recording metadata.

**6. Save and start**

Click **Save**, then select the channel and click **Play**, or enable **Auto-Start**.

## Talkgroup aliases

DMR talkgroup IDs are integers decoded from the air interface. Assign human-readable names so you can identify calls at a glance.

**1. Open the Aliases tab**

In the **Playlist Editor**, click **Aliases** in the sidebar.

**2. Create an alias group**

Click **Add Group** and name it to match the system or agency, for example `Fleet` or `Operations`.

**3. Add a talkgroup alias**

Click **Add Alias** inside the group. Set **Protocol** to `DMR`, set the identifier type to **Talkgroup**, and enter the numeric talkgroup ID and a display name.

**4. Add radio ID aliases (optional)**

Set the identifier type to **Radio ID** to tag transmissions from specific subscriber units.

## Filtering by talkgroup

Use the **Ignore Unaliased Talkgroups** option to limit decoding to only the talkgroups you have added as aliases. This is the primary filtering mechanism for DMR: SDRTrunk will not allocate a traffic channel for any call whose talkgroup has no alias entry.

> **Warning:**
> Enabling **Ignore Unaliased Talkgroups** silently drops any talkgroup without an alias entry — including calls you might not have known to add. Disable this option during initial monitoring to discover all active talkgroups before narrowing the list.

> **Tip:**
> DMR talkgroup IDs on trunked systems can differ from the IDs shown in RadioReference listings if the system uses compressed talkgroups. If aliases are not matching, enable **Use Compressed Talkgroups** and verify the IDs in the SDRTrunk event log.

## Common issues

**Timeslot 2 shows no activity on a Tier III system**

On many MotoTRBO systems, Timeslot 2 of the control channel carries data only. Voice traffic appears on dedicated traffic channel frequencies. Verify that your tuner's bandwidth includes the traffic channel frequencies and that the traffic channel pool size is greater than zero.

**Talkgroup IDs do not match RadioReference listings**

Enable **Use Compressed Talkgroups** in the decoder options. This converts the air interface value to the compressed format used by Hytera Tier III and some other vendors.

**Decoding works but audio quality is poor**

DMR uses AMBE+2 vocoder compression, and artifacts on weak signals are normal. Improving antenna placement or adding a low-noise amplifier (LNA) is the most effective remedy. SDRTrunk does not provide a per-channel equalizer for DMR, so hardware improvements are the primary path to better audio.

**CRC errors in the log and calls cutting out**

On sites with multipath or interference, CRC errors are common. Enable **Ignore CRC Checksums** to pass messages with CRC failures through to the decoder. This can recover audio on marginal signals but may also produce occasional garbled messages.
