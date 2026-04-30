# Stream decoded radio audio to online streaming platforms

> An overview of SDRTrunk Kennebec's audio streaming system, all supported broadcast destinations, and how to add and monitor live radio feeds.

SDRTrunk Kennebec can route decoded radio audio from your SDR hardware to a wide range of online platforms simultaneously. Whether you want a live feed on Broadcastify, a private Zello channel for your team, or a self-hosted Icecast server, the streaming subsystem handles the encoding, queuing, and delivery so you can focus on monitoring.

## How streaming works

When a trunked radio call ends, SDRTrunk Kennebec encodes the completed audio to MP3, places it in a broadcaster queue, and each enabled broadcaster delivers it to its destination. Live Icecast-compatible streams push audio in near real time, while call-upload services (Broadcastify Call, Rdio Scanner, OpenMHz, ThinLine Radio) post the completed file via an HTTP API.

For Zello, the flow is slightly different: audio is re-encoded from MP3 to Opus on the fly and pushed as a voice message to the configured Zello channel over a WebSocket connection.

> **Note:**
  Each broadcaster runs independently. A failure on one stream—such as a network interruption—does not affect the others.

## Supported platforms

| Platform          | Type        | Protocol                                    |
| ----------------- | ----------- | ------------------------------------------- |
| Broadcastify Feed | Live stream | Icecast TCP (compatible with Icecast 2.3.2) |
| Broadcastify Call | Call upload | HTTPS POST                                  |
| Icecast 2 (v2.4+) | Live stream | HTTP PUT                                    |
| Icecast (v2.3)    | Live stream | TCP source connection                       |
| Shoutcast v1.x    | Live stream | TCP source connection                       |
| Shoutcast v2.x    | Live stream | TCP source connection                       |
| Rdio Scanner      | Call upload | HTTPS POST                                  |
| OpenMHz           | Call upload | HTTPS POST                                  |
| ThinLine Radio    | Call upload | HTTPS POST                                  |
| Zello Work        | Live stream | WebSocket + Opus                            |
| Zello Consumer    | Live stream | WebSocket + Opus                            |
| IAmResponding     | Live stream | UDP raw PCM (Windows only)                  |

## Adding a stream

  ### Open the Streaming editor
    From the main menu, select **View** > **Streaming**. The Streaming editor opens on the right side of the application window.


  ### Add a new broadcaster
    Click the **+** (Add) button in the toolbar. A dropdown lets you choose the broadcaster type. Select the platform you want to configure.


  ### Fill in the required fields
    Enter the credentials and connection details for your chosen platform. Required fields vary by type—see the platform-specific pages linked below for exact field definitions.


  ### Enable the broadcaster
    Check the **Enabled** box in the configuration panel. SDRTrunk Kennebec will connect to the streaming server on startup and reconnect automatically if the connection drops.


  ### Save your configuration
    Click **Save**. The broadcaster appears in the Streaming list with a status indicator.


> **Tip:**
  You can add multiple configurations for the same platform type—for example, two separate Broadcastify Feed streams pointed at different mount points.

## Broadcaster Status panel

The Broadcaster Status panel (accessible from **View** > **Broadcaster Status**) shows the real-time connection state of every configured stream. Each entry displays the broadcaster name, platform icon, and one of the following states:

| State        | Meaning                                                                        |
| ------------ | ------------------------------------------------------------------------------ |
| Connected    | The broadcaster is connected and actively streaming or ready to stream.        |
| Disconnected | The broadcaster is not connected. Check your credentials and network settings. |
| Error        | A non-recoverable error occurred. Review the application log for details.      |
| Disabled     | The broadcaster exists in your configuration but is not enabled.               |

## Reconnect button (Zello)

Zello streams include a dedicated **Reconnect** button in the Broadcaster Status panel. Use this button to manually trigger a reconnection attempt after a recoverable error—for example, if the Zello server dropped the WebSocket session and the automatic retry did not restore it within a reasonable time.

> **Note:**
  For most connection problems, SDRTrunk Kennebec reconnects automatically. The reconnect button is a manual override for situations where the automatic recovery has not yet triggered.

## Platform guides

* [Broadcastify Feed and Broadcastify Call](/streaming/broadcastify)
* [Zello Work and Zello Consumer](/streaming/zello)
* [Icecast, Shoutcast, Rdio Scanner, OpenMHz, ThinLine Radio, and IAmResponding](/streaming/other-platforms)