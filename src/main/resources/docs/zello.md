# Stream Decoded Radio Audio to Zello Channels

SDRTrunk Kennebec can push decoded radio audio directly into Zello channels over a WebSocket connection, giving your team or group instant radio-to-Zello bridging. This is particularly useful for volunteer responders, EOC staff, or community monitoring groups who want to hear radio traffic inside Zello without carrying a physical scanner. Two integration types are supported: **Zello Work** for organizational or enterprise Zello networks, and **Zello Consumer** for personal Zello (Friends & Family) accounts.

## Zello Work vs. Zello Consumer

| | Zello Work | Zello Consumer |
|---|---|---|
| **Use case** | Enterprise / organizational PTT networks | Personal Zello (Friends & Family) accounts |
| **WebSocket endpoint** | `wss://zellowork.io/ws/{network_name}` | `wss://zello.io/ws` (fixed) |
| **Authentication** | Username + password | Username + password + JWT auth token |
| **Auth token required** | No | Yes — from the Zello developer portal |
| **Network Name field** | Required (your Zello Work subdomain) | Not used |

---

## Setting Up Zello Work

  **1. Confirm your credentials**

    Verify your Zello Work network name (the subdomain portion of your Zello Work URL, for example `acmefire`), the channel name you want to stream to, and the username and password of an account with transmit permission on that channel.

  **2. Open the Streaming Editor**

    In SDRTrunk Kennebec, go to **View** > **Streaming**.

  **3. Add a Zello Work broadcaster**

    Click **+** and select **Zello Work**.

  **4. Enter the configuration**

    Fill in the required fields in the configuration panel:

    | Field | Description |
    |---|---|
    | **Name** | A label for this configuration, for example `Dispatch Channel` |
    | **Network Name** | Your Zello Work network identifier (the subdomain) |
    | **Channel** | The target Zello channel name |
    | **Username** | Your Zello Work username |
    | **Password** | Your Zello Work password |

  **5. Configure timing parameters (optional)**

    Three parameters fine-tune how audio transmissions are shaped on the Zello channel:

    | Field | Default | Description |
    |---|---|---|
    | **Stream Guard (ms)** | `0` | Minimum gap in milliseconds enforced between the end of one Zello transmission and the start of the next |
    | **Pause Time (ms)** | `0` | Delay in milliseconds inserted between consecutive transmissions |
    | **Relaxation Time (ms)** | `700` | Hold-over time in milliseconds after a call ends before the Zello transmission closes |

    Leave these at their defaults unless you are experiencing clipped audio or back-to-back transmission collisions on the channel.

  **6. Enable and save**

    Check **Enabled**, then click **Save**. SDRTrunk Kennebec opens a WebSocket connection to `wss://zellowork.io/ws/{network_name}` and begins routing decoded audio.

---

## Setting Up Zello Consumer

  **1. Obtain a Zello developer auth token**

    Register your application on the Zello developer portal to receive a JWT auth token. This token is a requirement of the Zello Consumer API — you cannot connect without it.

  **2. Open the Streaming Editor**

    In SDRTrunk Kennebec, go to **View** > **Streaming**.

  **3. Add a Zello Consumer broadcaster**

    Click **+** and select **Zello Consumer**.

  **4. Enter the configuration**

    Fill in the required fields:

    | Field | Description |
    |---|---|
    | **Name** | A label for this configuration |
    | **Channel** | The Zello channel name |
    | **Username** | Your Zello account username |
    | **Password** | Your Zello account password |
    | **Auth Token** | The JWT token from the Zello developer portal |

  **5. Configure timing parameters (optional)**

    The same **Stream Guard**, **Pause Time**, and **Relaxation Time** parameters are available as for Zello Work. The defaults (`0` / `0` / `700` ms) are a good starting point.

  **6. Enable and save**

    Check **Enabled**, then click **Save**. SDRTrunk Kennebec connects to `wss://zello.io/ws` and starts streaming.

> **Note:**
>
  The auth token is a requirement of the Zello Consumer API. You must register your application on the Zello developer portal to obtain one before you can stream to a consumer channel.

---

## Audio Encoding

Both Zello integration types re-encode decoded audio to **Opus** before transmission over WebSocket, regardless of the MP3 format used internally by the streaming subsystem. This re-encoding happens automatically in real time — you do not need to configure any codec settings.

> **Note:**
>
  Because SDRTrunk Kennebec re-encodes audio to Opus for Zello, there is a brief additional processing step compared to Icecast or Shoutcast streaming. In practice this delay is imperceptible.

---

## Automatic Reconnection

SDRTrunk Kennebec automatically reconnects the Zello broadcaster when it detects transient server errors. Specific server error messages — including `failed to start sending message` and `failed to stop sending message` — trigger an automatic reconnect rather than a permanent shutdown. In most cases the connection recovers without any user action.

If the automatic reconnect does not restore the stream in a timely manner, use the **Reconnect** button in the **Broadcaster Status** panel (**View** > **Broadcaster Status**). Clicking **Reconnect** forces an immediate reconnection attempt and resets the session.

---

## Diagnostic Logging

Each Zello broadcaster configuration has a **Verbose Logging** toggle. When enabled, SDRTrunk Kennebec writes detailed session diagnostics to the application log, including:

- Session epoch and stream identifiers
- Opus encoder state changes
- WebSocket frame activity

You can also enable verbose logging for all Zello broadcasters globally from **Application** > **Diagnostics (Logging)** in User Preferences, using the **Zello** category toggle.

> **Warning:**
>
  Verbose logging generates a significant volume of log output. Enable it for troubleshooting sessions only, and disable it once you have captured the information you need.

> **Tip:**
>
  If you find yourself clicking **Reconnect** frequently, enable verbose logging to capture session-level diagnostics and share the log with support.
