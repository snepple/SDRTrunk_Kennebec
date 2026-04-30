# Monitor DMR Tier III and conventional radio channels

> Configure DMR conventional and Tier III trunked channels in SDRTrunk Kennebec with talkgroup aliases, radio ID filtering, and CRC options.

Digital Mobile Radio (DMR) is an ETSI open standard widely used by commercial fleets, utilities, and some public safety agencies. SDRTrunk Kennebec decodes both conventional DMR channels and DMR Tier III trunked systems, extracting talkgroup IDs, radio IDs, and call metadata at 9,600 bps over a 12.5 kHz carrier. This guide covers the key differences between conventional and trunked DMR and walks through configuration in the Playlist Editor.

## DMR fundamentals

DMR uses TDMA (Time Division Multiple Access) to place **two independent voice timeslots** on a single 12.5 kHz channel. SDRTrunk creates a separate decoder instance for each timeslot, so you see Timeslot 1 and Timeslot 2 activity independently in the event log.

| Property              | Value     |
| --------------------- | --------- |
| Modulation            | 4FSK      |
| Bit rate              | 9,600 bps |
| Channel spacing       | 12.5 kHz  |
| Timeslots per carrier | 2         |
| Talkgroup support     | Yes       |
| Radio ID support      | Yes       |

## DMR conventional vs. DMR Tier III

  ### Conventional (Tier I / Tier II)
    On a conventional DMR channel, radios transmit directly on a fixed frequency. There is no control channel. SDRTrunk monitors the carrier and decodes any DMR activity it detects on either timeslot.

    Use the **DMR** decoder type in the Playlist Editor for conventional channels.


  ### Trunked (Tier III)
    DMR Tier III is the trunked variant, commonly deployed on Motorola MOTOTRBO Capacity Plus, Linked Capacity Plus, and Connect Plus systems. The system broadcasts a control channel that grants calls to specific traffic channel frequencies. SDRTrunk decodes the control channel and automatically follows traffic channel grants.

    Use the **DMR** decoder type and point it at the control channel frequency. SDRTrunk detects Tier III operation automatically.

    > **Note:**
      On MotoTRBO systems that use fixed timeslot-to-frequency mapping (MOTOTRBO conventional IP Site Connect), you can configure a **Timeslot Map** in the decoder settings to tell SDRTrunk which frequencies correspond to which timeslots.



## Set up a DMR channel

  ### Open the Playlist Editor
    From the main menu, select **View → Playlist Editor**. Choose or create a playlist.


  ### Add a new channel
    Click **Add Channel**. A new entry appears in the Channels table.


  ### Select the DMR decoder
    In the channel configuration panel, set the **Decoder** drop-down to **DMR**.


  ### Enter the frequency
    Type the channel frequency in the **Frequency** field in Hz. For a trunked system, use the control channel frequency.


  ### Configure decoder options
    Expand the **Decoder Options** section:

    * **Traffic Channel Pool Size** — maximum simultaneous calls to decode. Default is suitable for most systems. Reduce on constrained hardware.
    * **Ignore Data Calls** — enabled by default. Disabling this decodes data traffic channel grants, which increases CPU load.
    * **Ignore Unaliased Talkgroups** — when enabled, only talkgroups with an alias entry are decoded. Useful for filtering busy systems down to what you care about.
    * **Ignore CRC Checksums** — disables CRC validation on decoded messages. Enable only if you see valid-looking calls being dropped due to checksum errors on poor-signal sites.
    * **Use Compressed Talkgroups** — enable for Hytera Tier III systems that use the compressed talkgroup format described in ETSI 102 361-2 Annex C.


  ### Assign system and site names
    Fill in the **System** and **Site** fields for clear labeling in the Now Playing view and recording metadata.


  ### Save and start
    Click **Save**, then select the channel and click **Play**, or enable **Auto-Start**.


## Talkgroup aliases

DMR talkgroup IDs are integers decoded from the air interface. Assign human-readable names so you can identify calls at a glance.

  ### Open the Aliases tab
    In the Playlist Editor, select **Aliases**.


  ### Create an alias group
    Click **Add Group** and name it to match the system or agency (for example, `Fleet` or `Operations`).


  ### Add a talkgroup alias
    Click **Add Alias** inside the group. Set **Protocol** to `DMR`, set the identifier type to **Talkgroup**, and enter the numeric talkgroup ID and a display name.


  ### Add radio ID aliases (optional)
    Set the identifier type to **Radio ID** to tag transmissions from specific subscriber units.


> **Tip:**
  DMR talkgroup IDs on trunked systems can differ from the IDs shown in RadioReference listings if the system uses compressed talkgroups. If aliases are not matching, try enabling the **Use Compressed Talkgroups** option and verifying the IDs in the SDRTrunk event log.

## Filtering by talkgroup

Use the **Ignore Unaliased Talkgroups** option to limit decoding to only the talkgroups you have added as aliases. This is the primary filtering mechanism for DMR: SDRTrunk will not allocate a traffic channel for any call whose talkgroup has no alias entry.

> **Warning:**
  Enabling **Ignore Unaliased Talkgroups** means any talkgroup without an alias entry is silently dropped, including calls you might not have known to add. Disable this option during initial monitoring to discover all active talkgroups before narrowing the list.

## Common issues

  ### Timeslot 2 shows no activity on a Tier III system
    On many MotoTRBO systems, Timeslot 2 of the control channel carries data only. Voice traffic appears on dedicated traffic channel frequencies. Verify that your tuner's bandwidth includes the traffic channel frequencies and that the traffic channel pool size is greater than zero.


  ### Talkgroup IDs do not match RadioReference listings
    Try enabling **Use Compressed Talkgroups** in the decoder options. This converts the air interface value to the compressed format used by Hytera Tier III and some other vendors.


  ### Decoding works but audio quality is poor
    DMR uses AMBE+2 vocoder compression. Audio artifacts on weak signals are normal. Improving antenna placement or adding a low-noise amplifier (LNA) is the most effective remedy. SDRTrunk does not currently provide a per-channel equalizer for DMR (unlike P25), so hardware improvements are the primary path to better audio.


  ### CRC errors appear in the log and calls cut out
    On sites with multipath or interference, CRC errors are common. Enable **Ignore CRC Checksums** to pass messages with CRC failures through to the decoder. This can recover audio on marginal signals but may also produce occasional garbled messages.