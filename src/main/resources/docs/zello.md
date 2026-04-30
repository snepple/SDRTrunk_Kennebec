# Stream decoded radio audio to Zello Work and Zello Consumer

> Configure SDRTrunk Kennebec to broadcast decoded radio audio to Zello Work channels or Zello Consumer (Friends & Family) in real time.

SDRTrunk Kennebec can push decoded radio audio directly to Zello channels over a WebSocket connection, giving your team or group instant radio-to-Zello bridging. Two Zello integration types are supported: **Zello Work** for organizational or enterprise Zello networks, and **Zello Consumer** for personal Zello (Friends & Family) accounts.

## Zello Work vs. Zello Consumer

  ### Zello Work
    Zello Work is Zello's enterprise push-to-talk platform. It uses a private WebSocket endpoint specific to your organization's network name (`wss://zellowork.io/ws/{network_name}`).

    You need a Zello Work account with permission to transmit on the target channel.

    **Required fields**

    | Field        | Description                                                                                        |
    | ------------ | -------------------------------------------------------------------------------------------------- |
    | Network Name | Your Zello Work network identifier (the subdomain portion of your Zello Work URL, e.g. `acmefire`) |
    | Channel      | The Zello Work channel to stream to                                                                |
    | Username     | Your Zello Work account username                                                                   |
    | Password     | Your Zello Work account password                                                                   |


  ### Zello Consumer
    Zello Consumer uses the public Zello application (Friends & Family) and connects to the fixed endpoint `wss://zello.io/ws`. Authentication requires a JWT token obtained from the Zello developer portal in addition to username and password credentials.

    **Required fields**

    | Field      | Description                                      |
    | ---------- | ------------------------------------------------ |
    | Channel    | The Zello channel name to stream to              |
    | Username   | Your Zello account username                      |
    | Password   | Your Zello account password                      |
    | Auth Token | A JWT token issued by the Zello developer portal |

    > **Note:**
      The auth token is a requirement of the Zello Consumer API. You must register your application on the Zello developer portal to obtain one before you can stream to a consumer channel.



## Setting up Zello Work

  ### Prepare your Zello Work credentials
    Confirm your Zello Work network name, the channel you want to stream to, and the username and password of an account with transmit permission on that channel.


  ### Add a Zello Work broadcaster
    In SDRTrunk Kennebec, go to **View** > **Streaming**, click **+**, and select **Zello Work**.


  ### Enter the configuration
    Fill in the required fields in the configuration panel:

    | Field        | Description                                             |
    | ------------ | ------------------------------------------------------- |
    | Name         | A label for this configuration, e.g. "Dispatch Channel" |
    | Network Name | Your Zello Work network identifier                      |
    | Channel      | The target Zello channel name                           |
    | Username     | Your Zello Work username                                |
    | Password     | Your Zello Work password                                |


  ### Configure timing parameters (optional)
    Three optional timing parameters fine-tune how audio transmissions are shaped on the Zello channel:

    | Field                | Default | Description                                                                                              |
    | -------------------- | ------- | -------------------------------------------------------------------------------------------------------- |
    | Stream Guard (ms)    | 0       | Minimum gap in milliseconds enforced between the end of one Zello transmission and the start of the next |
    | Pause Time (ms)      | 0       | Delay in milliseconds inserted between consecutive transmissions                                         |
    | Relaxation Time (ms) | 700     | Hold-over time in milliseconds after a call ends before the Zello transmission closes                    |

    Leave these at their defaults unless you are experiencing clipped audio or back-to-back transmission collisions on the channel.


  ### Enable and save
    Check **Enabled**, then click **Save**. SDRTrunk Kennebec opens a WebSocket connection to `wss://zellowork.io/ws/{network_name}` and begins routing decoded audio.


## Setting up Zello Consumer

  ### Obtain a Zello developer auth token
    Register your application on the Zello developer portal to receive a JWT auth token. This token is required for all Zello Consumer API connections.


  ### Add a Zello Consumer broadcaster
    In SDRTrunk Kennebec, go to **View** > **Streaming**, click **+**, and select **Zello Consumer**.


  ### Enter the configuration
    Fill in the required fields:

    | Field      | Description                                   |
    | ---------- | --------------------------------------------- |
    | Name       | A label for this configuration                |
    | Channel    | The Zello channel name                        |
    | Username   | Your Zello account username                   |
    | Password   | Your Zello account password                   |
    | Auth Token | The JWT token from the Zello developer portal |


  ### Configure timing parameters (optional)
    The same Stream Guard, Pause Time, and Relaxation Time parameters are available as for Zello Work. The defaults (0 / 0 / 700 ms) are a good starting point.


  ### Enable and save
    Check **Enabled**, then click **Save**. SDRTrunk Kennebec connects to `wss://zello.io/ws` and starts streaming.


## Audio encoding

Both Zello integration types re-encode audio to **Opus** before transmission, regardless of the `MP3` format stored internally by the streaming subsystem. The re-encoding happens in real time inside the Zello broadcaster. You do not need to configure any codec settings—this is handled automatically.

> **Note:**
  Because SDRTrunk Kennebec re-encodes audio to Opus for Zello, there is a brief additional processing step compared to Icecast or Shoutcast streaming. In practice, this delay is imperceptible.

## Automatic reconnection

SDRTrunk Kennebec automatically reconnects the Zello broadcaster when it detects transient server errors. Specific server error messages—including `failed to start sending message` and `failed to stop sending message`—trigger an automatic reconnect rather than a permanent shutdown. In most cases the connection recovers without any user action.

## Manual reconnect button

If the automatic reconnect does not restore the stream in a timely manner, use the **Reconnect** button in the **Broadcaster Status** panel (**View** > **Broadcaster Status**). Clicking Reconnect forces an immediate reconnection attempt and resets the session.

> **Tip:**
  If you find yourself clicking Reconnect frequently, enable verbose logging (see below) to capture session-level diagnostics and share the log with support.

## Verbose diagnostic logging

Each Zello broadcaster configuration has a **Verbose Logging** toggle. When enabled, SDRTrunk Kennebec writes detailed session diagnostics to the application log, including:

* Session epoch and stream identifiers
* Opus encoder state changes
* WebSocket frame activity

You can also enable verbose logging globally for all Zello broadcasters from **Application** > **Diagnostics (Logging)** in User Preferences, using the **Zello** category toggle.

> **Warning:**
  Verbose logging generates a significant volume of log output. Enable it for troubleshooting sessions only, and disable it once you have captured the information you need.