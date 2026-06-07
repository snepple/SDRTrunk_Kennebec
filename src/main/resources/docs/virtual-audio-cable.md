# Route Decoded Audio to External Apps via Virtual Cable

SDRTrunk Kennebec can isolate audio from specific talkgroups or radio IDs and route it to any audio output device visible to the operating system — including virtual audio cable (VAC) devices. By installing a third-party VAC driver, you can pass pristine decoded audio directly into other programs running on the same computer, such as streaming encoders, transcription software, paging detection applications, or call recorders.

## Supported VAC software

SDRTrunk Kennebec interacts with virtual audio cables as standard output devices. You need to install a third-party driver for your operating system before assigning one in the application.

<Tabs>
  <Tab title="Windows">
    **VB-Audio Virtual Cable (VB-Cable)** — Free, single cable. Install from the VB-Audio website. Appears as `CABLE Input (VB-Audio Virtual Cable)` in Windows sound settings.

    **Virtual Audio Cable (VAC)** — Commercial software supporting multiple simultaneous cables. Useful when you need to route several aliases to different applications at the same time.
  </Tab>
  <Tab title="macOS">
    **BlackHole** — Free, open-source virtual audio driver supporting 2, 16, or 64 channels. Available via Homebrew or direct download.

    **Loopback** — Commercial application from Rogue Amoeba. Provides a graphical interface for creating and managing virtual cables with routing rules.
  </Tab>
  <Tab title="Linux">
    **PulseAudio null sinks** — Create a virtual output device with the `pactl load-module module-null-sink` command. Other applications can then record from the associated monitor source.

    **PipeWire virtual devices** — On systems running PipeWire, use `pw-loopback` or the PipeWire configuration API to create loopback nodes that function as virtual cables.
  </Tab>
</Tabs>

---

## How audio routing works

Audio routing in SDRTrunk Kennebec is configured at the alias level. When a decoded call matches an alias, the audio is sent to the output device assigned to that alias. Calls that match an alias with no device assigned play through your default system speakers.

```
SDRTrunk Kennebec
  └── Decoded call matches alias
        ├── Alias has no output assigned  →  Default system speakers
        └── Alias has VAC assigned        →  Virtual Audio Cable device
                                                └── External application listens
                                                      on VAC input/monitor
```

> [!NOTE]
>
  When a specific audio output device is assigned to an alias, the audio for that alias will **no longer play** through your default computer speakers. It is routed entirely to the VAC.

---

## Assign a VAC to an alias

  **1. Open the Alias Editor**

    In the Playlist Editor, click **Aliases** in the sidebar.

  **2. Select the target alias**

    Find and select the alias you want to route — for example, "Fire Dispatch" or a specific pager tone group.

  **3. Configure the audio output device**

    In the alias detail editor panel below the table, locate the **Audio output device** property. Click the dropdown and select the virtual audio cable you installed — for example, `CABLE Input (VB-Audio Virtual Cable)`.

  **4. Save the alias**

    Click **Save**. Any future calls matching this alias are now routed exclusively to the selected virtual audio cable.


---

## Route multiple aliases

You can assign the same virtual audio cable to multiple aliases if you want an external application to receive several talkgroups at once. The audio streams are mixed into the single VAC device.

If you have multiple VAC devices installed — for example, VB-Cable A and VB-Cable B — you can route different aliases to different applications simultaneously:

- Route "Fire Dispatch" to VB-Cable A for a Two Tone Detect paging application.
- Route "Police Dispatch" to VB-Cable B for a dedicated call recorder.

---

## Use cases

  **Live streaming**

    Route talkgroup audio into OBS Studio or another encoder to stream scanner traffic to a YouTube or Broadcastify channel.

  **Transcription**

    Feed decoded audio into Whisper, Google Speech-to-Text, or any speech recognition tool that accepts a microphone or line input source.

  **Paging detection**

    Send two-tone or DTMF traffic to TwoToneDetect or a similar application for automated paging alerts and dispatch logging.

  **Secondary recording**

    Capture audio in a separate recording application alongside SDRTrunk's built-in recorder for redundancy or alternative formats.


---

## Troubleshoot missing audio

If your external application is not receiving audio, work through the following checks:

1. **Verify the alias is matching.** The alias name should appear in the Now Playing panel when the relevant calls are active. If it does not appear, the alias is not matching the talkgroup or radio ID.
2. **Check the external application's input.** Make sure the application is configured to listen on the **Input** (or **Microphone**) side of the virtual cable — not the output side.
3. **Check your OS sound mixer.** Open your operating system's volume mixer and confirm the VAC device is not muted and its volume is above zero.
4. **Confirm the VAC driver is installed.** In SDRTrunk Kennebec, re-open the alias and verify the dropdown still shows your VAC device. If it reverted to a default, the driver may not have loaded correctly at system startup.
