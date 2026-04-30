# Configure Two Tone Detect for fire and EMS dispatch alerting

> Use SDRTrunk Kennebec's Two Tone Detect to identify sequential fire and EMS dispatch tones and trigger Zello, MQTT, or alert sound actions.

Two Tone Detect monitors an audio channel for sequential paging tones used in fire and EMS dispatch. When a dispatcher activates a station or unit, they transmit two sequential audio tones before the voice message — a tone pair unique to that station. SDRTrunk Kennebec's Two Tone Detect feature listens for these pairs and fires configurable actions the moment a match is detected, giving responders advance notice before the voice message plays.

## What are two-tone sequential tones?

Two-tone sequential (or "two-tone paging") is a signaling system widely used in North American fire and EMS dispatch. Each station or unit is assigned a unique pair of audio frequencies, called Tone A and Tone B, transmitted back-to-back for a fixed duration. SDRTrunk Kennebec supports tone frequencies from the Motorola QC-II and Plectron standard tone tables, covering the range from approximately 282 Hz to 3,062 Hz, and also accepts custom frequency values.

> **Note:**
  Two Tone Detect works on any channel that carries paging tones — typically a conventional FM dispatch channel. The channel must already be configured and decoding in the Channels section before Two Tone Detect can process its audio.

## Open the Two Tone Editor

In the Playlist Editor, click **Two Tones** in the left sidebar. The editor opens in a split-pane layout. The left pane lists your configured detectors. The right pane shows the configuration detail for the selected detector, with two tabs: **Configuration** and **Aliases**.

## Create a detector

  ### Add a new detector
    Click **New** in the left-pane toolbar and select **Detector**. A new entry named `New Detector` appears in the list.


  ### Set the detector name
    In the **Name** field of the Configuration tab, type a descriptive name for this detector, such as `Station 7` or `EMS Unit 42`. This name is also used as the alias name when the detector fires.


  ### Choose the detection type
    Open the **Type** dropdown and select one of:

    * **A/B Tones** — requires both Tone A and Tone B to be received in sequence (standard two-tone paging).
    * **Long A Tone Only** — triggers on a single sustained Tone A, used by some older or simplified paging systems.


  ### Set Tone A
    Click the **Tone A** dropdown. You can type a frequency value directly or select from the autocomplete list, which is populated from the combined Motorola QC-II and Plectron standard tables. Enter the value in Hz, for example `688.3`.


  ### Set Tone B (A/B Tones mode only)
    Click the **Tone B** dropdown and enter the second tone frequency. Tone B is disabled automatically when **Long A Tone Only** is selected.


  ### Save the configuration
    Click **Save** at the bottom of the Configuration tab. The detector is saved to the current playlist.


> **Tip:**
  Use **Clone** in the left-pane toolbar to duplicate an existing detector and adjust only the tone values. This is faster than creating detectors from scratch when you have many similar entries.

## Link a detector to an alias

The **Aliases** tab in the right pane lets you associate the detector with an alias from your alias lists. When the detector fires, SDRTrunk Kennebec attributes the event to the linked alias and applies any actions configured on that alias (beep, clip, record, stream).

To link an alias, select the detector, open the **Aliases** tab, and use the alias picker to select the alias that corresponds to this station or unit.

Alternatively, you can add a **Two Tone Paging** identifier to an existing alias from the Alias Editor. Enter the detector name exactly as it appears in the Two Tone Editor.

## Configure Zello integration

If you have a Zello stream configured in the Playlist Editor's Streaming section, you can route Two Tone detection events to a Zello channel.

  ### Select a Zello channel
    In the Configuration tab, open the **Zello Channel** dropdown. The dropdown is populated from your existing Zello broadcast configurations. Select the channel you want to receive the alert.


  ### Enable a text message (optional)
    Check **Enable Text Message** to send a text message to the Zello channel when the detector fires. Enter your message in the **Message Template** field. The following placeholders are available:

    * `{Alias}` — the detector name
    * `{Channel Name}` — the Zello channel name
    * `{Frequency}` — the channel frequency
    * `{Timestamp}` — the time of detection (Unix milliseconds)

    A live preview of the resolved message appears below the template field.


  ### Enable an alert tone (optional)
    Check **Enable Zello Alert Tone** and select a tone file from the **Alert Tone File** dropdown (`alert1.wav` or `alert2.wav`). Click **Preview** to audition the tone before saving.


## Configure MQTT integration

  ### Enable MQTT publish
    Check **Enable MQTT Publish** in the MQTT Integration section of the Configuration tab. The **MQTT Topic** and **MQTT Payload** fields become active.


  ### Set the topic and payload
    Enter the MQTT topic string in **MQTT Topic** (for example, `sdrtrunk/dispatch/station7`). Enter the message payload in **MQTT Payload**. The payload can be any text, including JSON.


  ### Save
    Click **Save**. Each time the detector fires, SDRTrunk Kennebec publishes to the configured MQTT broker using the global MQTT settings in User Preferences.


> **Note:**
  MQTT broker connection settings (host, port, credentials) are configured in **View → User Preferences**, not in the Two Tone Editor.

## IAmResponding integration

SDRTrunk Kennebec supports sending Two Tone detection events to **IAmResponding** via a local UDP stream. IAmResponding is a response management system used by volunteer fire and EMS agencies.

To use this integration, configure a UDP broadcast stream in the Streaming section of the Playlist Editor and point it at the IAmResponding local receiver port on the same computer. Then assign that stream to the alias linked to the Two Tone detector. The UDP stream type is available under the local computer streaming options.

> **Warning:**
  UDP streaming to IAmResponding is a local-only integration. The receiving IAmResponding software must be running on the same machine as SDRTrunk Kennebec.

## Manage detectors

| Action      | Description                                                                               |
| ----------- | ----------------------------------------------------------------------------------------- |
| **Clone**   | Duplicates the selected detector. Useful for creating multiple similar detectors quickly. |
| **Delete**  | Removes the selected detector permanently from the playlist.                              |
| **Refresh** | Reloads the detector list from the current playlist, discarding any unsaved changes.      |