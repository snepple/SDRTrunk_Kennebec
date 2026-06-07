# Two Tone Detect for Fire and EMS Dispatch Alerting

Two Tone Detect monitors an audio channel for sequential paging tones used in fire and EMS dispatch. When a dispatcher activates a station, they transmit two sequential audio tones before the voice message — a tone pair unique to that station. SDRTrunk Kennebec listens for these pairs and immediately fires configurable actions when a match is found, giving responders advance notice before the voice transmission plays.

## What are two-tone sequential tones?

Two-tone sequential paging is a signaling system widely used in North American fire and EMS dispatch. Each station or unit is assigned a unique pair of audio frequencies — called Tone A and Tone B — transmitted back-to-back for a fixed duration. SDRTrunk Kennebec supports tone frequencies from the Motorola QC-II and Plectron standard tables, covering approximately 282 Hz to 3,062 Hz, and also accepts custom frequency values entered directly in Hz.

> [!NOTE]
>
  Two Tone Detect works on any channel that carries paging tones — typically a conventional FM dispatch channel. The channel must already be configured and decoding in the **Channels** section before Two Tone Detect can process its audio.

## Open the Two Tone Editor

In the **Playlist Editor**, click **Two Tones** in the left sidebar. The editor opens in a split-pane layout: the left pane lists your configured detectors, and the right pane shows the configuration detail for the selected detector, with two tabs — **Configuration** and **Aliases**.

## Create a detector

  **1. Add a new detector**

    Click **New** in the left-pane toolbar and select **Detector**. A new entry named `New Detector` appears in the list.

  **2. Set the detector name**

    In the **Name** field of the **Configuration** tab, type a descriptive name such as `Station 7` or `EMS Unit 42`. This name is also used as the alias name when the detector fires.

  **3. Choose the detection type**

    Open the **Type** dropdown and select one of the following:

    | Type | When to use |
    | --- | --- |
    | **A/B Tones** | Standard two-tone paging — both Tone A and Tone B must be received in sequence |
    | **Long A Tone Only** | Some older or simplified paging systems that use a single sustained Tone A |

  **4. Set Tone A**

    Click the **Tone A** dropdown. Type a frequency value directly or select from the autocomplete list, which is populated from the combined Motorola QC-II and Plectron standard tables. Enter the value in Hz — for example, `688.3`.

  **5. Set Tone B (A/B Tones mode only)**

    Click the **Tone B** dropdown and enter the second tone frequency. The **Tone B** field is disabled automatically when **Long A Tone Only** is selected.

  **6. Save the configuration**

    Click **Save** at the bottom of the **Configuration** tab. The detector is saved to the current playlist.


> [!TIP]
>
  Use **Clone** in the left-pane toolbar to duplicate an existing detector and adjust only the tone values. This is faster than creating detectors from scratch when you have many similar entries.

## Link a detector to an alias

The **Aliases** tab in the right pane lets you associate the detector with an alias from your alias lists. When the detector fires, SDRTrunk Kennebec attributes the event to that alias and applies any actions configured on it — such as beep, clip, record, or stream.

To link from the detector side, select the detector, open the **Aliases** tab, and use the alias picker to choose the alias that corresponds to this station or unit.

To link from the alias side, open the alias in the **Alias Editor**, add a **Two Tone Paging** identifier, and enter the detector name exactly as it appears in the Two Tone Editor.

## Configure Zello integration

If you have a Zello broadcast stream configured in the **Playlist Editor's Streaming** section, you can route Two Tone detection events to a Zello channel.

  **7. Select a Zello channel**

    In the **Configuration** tab, open the **Zello Channel** dropdown. It is populated from your existing Zello broadcast configurations. Select the channel you want to receive the alert.

  **8. Enable a text message (optional)**

    Check **Enable Text Message** to send a text message to the Zello channel when the detector fires. Enter your message in the **Message Template** field using any of the following placeholders:

    | Placeholder | Replaced with |
    | --- | --- |
    | `{Alias}` | The detector name |
    | `{Channel Name}` | The Zello channel name |
    | `{Frequency}` | The channel frequency |
    | `{Timestamp}` | The time of detection (Unix milliseconds) |

    A live preview of the resolved message appears below the template field.

  **9. Enable an alert tone (optional)**

    Check **Enable Zello Alert Tone** and select a tone file from the **Alert Tone File** dropdown (`alert1.wav` or `alert2.wav`). Click **Preview** to audition the tone before saving.

  **10. Save**

    Click **Save** in the **Configuration** tab.


## Configure MQTT integration

  **11. Enable MQTT publish**

    Check **Enable MQTT Publish** in the MQTT Integration section of the **Configuration** tab. The **MQTT Topic** and **MQTT Payload** fields become active.

  **12. Set the topic and payload**

    Enter the MQTT topic string in **MQTT Topic** — for example, `sdrtrunk/dispatch/station7`. Enter the message payload in **MQTT Payload**. The payload can be any text, including JSON.

  **13. Save**

    Click **Save**. Each time the detector fires, SDRTrunk Kennebec publishes to the configured MQTT broker using the global MQTT settings in **User Preferences**.


> [!NOTE]
>
  MQTT broker connection settings — host, port, and credentials — are configured in **View → User Preferences**, not in the Two Tone Editor.

## IAmResponding integration

SDRTrunk Kennebec can send Two Tone detection events to **IAmResponding** via a local UDP stream. IAmResponding is a response management system used by volunteer fire and EMS agencies.

To use this integration, configure a UDP broadcast stream in the **Streaming** section of the **Playlist Editor** and point it at the IAmResponding local receiver port on the same computer. Then assign that stream to the alias linked to the Two Tone detector.

> [!WARNING]
>
  UDP streaming to IAmResponding is a local-only integration. The IAmResponding receiver software must be running on the same machine as SDRTrunk Kennebec.

## Manage detectors

Use the toolbar actions in the left pane to maintain your detector list:

| Action | Description |
| --- | --- |
| **Clone** | Duplicates the selected detector. Useful for creating multiple similar detectors quickly without rebuilding the configuration from scratch. |
| **Delete** | Permanently removes the selected detector from the playlist. This action cannot be undone. |
| **Refresh** | Reloads the detector list from the current playlist, discarding any unsaved changes. |
