# Stream to Icecast, Shoutcast, OpenMHz, and more platforms

> Configuration guides for Icecast, Shoutcast, Rdio Scanner, OpenMHz, ThinLine Radio, and IAmResponding streaming in SDRTrunk Kennebec.

Beyond Broadcastify and Zello, SDRTrunk Kennebec supports six additional broadcast destinations. Two are live streaming servers (Icecast and Shoutcast), three are completed-call upload APIs (Rdio Scanner, OpenMHz, and ThinLine Radio), and one is a local UDP stream for the IAmResponding Two Tone Detect integration. This page covers each in turn.

***

## Icecast

SDRTrunk Kennebec supports two Icecast connection modes that match different Icecast server versions. Both stream MP3 audio from a mono, 8000 Hz source.

  ### Icecast 2 (v2.4+)
    **Icecast 2** uses HTTP PUT-based source authentication, introduced in Icecast 2.4. Choose this type when your server runs Icecast 2.4 or later.

    **Required fields**

    | Field       | Description                                                           |
    | ----------- | --------------------------------------------------------------------- |
    | Name        | A label for this stream configuration                                 |
    | Host        | Hostname or IP address of the Icecast server                          |
    | Port        | TCP port (commonly 8000)                                              |
    | Mount Point | Stream path starting with `/`, e.g. `/radio.mp3`                      |
    | Password    | The source password configured in your Icecast server's `icecast.xml` |

    **Optional fields**

    | Field       | Description                                              |
    | ----------- | -------------------------------------------------------- |
    | Username    | Source username (defaults to `source`)                   |
    | Description | Human-readable stream description sent to the server     |
    | Genre       | Genre tag visible to stream directory listeners          |
    | Public      | Whether to advertise the stream in the Icecast directory |
    | URL         | A website URL associated with the stream                 |

    > **Note:**
      The **Inline** metadata flag is enabled by default. It embeds talkgroup metadata into the MP3 stream at an interval calculated from the bit rate (bit rate × 125 bytes). Leave this enabled unless your player has compatibility issues with inline ICY metadata.



  ### Icecast (v2.3)
    **Icecast TCP** uses the older TCP source handshake protocol compatible with Icecast 2.3.x servers (and the Icecast 2.3.2 variant used by Broadcastify). Choose this type when your self-hosted server runs an older Icecast build.

    **Required fields**

    | Field       | Description                                  |
    | ----------- | -------------------------------------------- |
    | Name        | A label for this stream configuration        |
    | Host        | Hostname or IP address of the Icecast server |
    | Port        | TCP port (commonly 8000)                     |
    | Mount Point | Stream path starting with `/`                |
    | Password    | The source password                          |

    The same optional fields available for Icecast 2 (Username, Description, Genre, Public, URL) apply here as well.


### Adding an Icecast stream

  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select either **Icecast 2 (v2.4+)** or **Icecast (v2.3)** depending on your server version.


  ### Fill in server details
    Enter the host, port, mount point, and password that match your Icecast server's `icecast.xml` source password configuration.


  ### Enable and save
    Check **Enabled**, then click **Save**. SDRTrunk Kennebec connects to the server and begins streaming audio as decoded calls arrive.


***

## Shoutcast

SDRTrunk Kennebec supports both Shoutcast v1 and v2 source connections. Both send MP3 audio over a TCP source handshake.

  ### Shoutcast v1.x
    Shoutcast v1 uses a simple password-only source authentication. It does not require a username or stream ID.

    **Required fields**

    | Field    | Description                                    |
    | -------- | ---------------------------------------------- |
    | Name     | A label for this configuration                 |
    | Host     | Hostname or IP address of the Shoutcast server |
    | Port     | TCP port of the Shoutcast server               |
    | Password | The source password                            |

    **Optional fields**

    | Field       | Description                                  |
    | ----------- | -------------------------------------------- |
    | Genre       | Genre tag for the stream                     |
    | Description | A description sent to the server             |
    | Public      | Whether the stream should be publicly listed |


  ### Shoutcast v2.x
    Shoutcast v2 introduces stream IDs and optional user ID authentication to support multiple streams on a single server port.

    **Required fields**

    | Field     | Description                                    |
    | --------- | ---------------------------------------------- |
    | Name      | A label for this configuration                 |
    | Host      | Hostname or IP address of the Shoutcast server |
    | Port      | TCP port of the Shoutcast server               |
    | Password  | The source password                            |
    | Stream ID | Numeric stream identifier (1 to 2,147,483,647) |

    **Optional fields**

    | Field   | Description                                                                 |
    | ------- | --------------------------------------------------------------------------- |
    | User ID | Optional username for servers that require authenticated source connections |
    | Genre   | Genre tag                                                                   |
    | URL     | Website URL associated with the stream                                      |
    | Public  | Public directory listing flag                                               |


### Adding a Shoutcast stream

  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select **Shoutcast v1.x** or **Shoutcast v2.x**.


  ### Fill in server details
    Enter the host, port, and password. For Shoutcast v2, also enter the stream ID.


  ### Enable and save
    Check **Enabled**, then click **Save**.


***

## Rdio Scanner

Rdio Scanner is a self-hosted web application for browsing and replaying trunked radio calls. SDRTrunk Kennebec uploads completed call recordings to a Rdio Scanner instance via its HTTP API.

**Required fields**

| Field     | Description                                                                                              |
| --------- | -------------------------------------------------------------------------------------------------------- |
| Name      | A label for this configuration                                                                           |
| Host      | The base URL of your Rdio Scanner instance (e.g. `http://192.168.1.50` or `https://scanner.example.com`) |
| API Key   | The API key configured in your Rdio Scanner instance for call uploads                                    |
| System ID | The numeric system ID assigned in Rdio Scanner to the radio system you are uploading                     |

> **Note:**
  The host field defaults to `http://localhost` if you leave it blank. Update it to the actual URL of your Rdio Scanner server before enabling the broadcaster.

### Adding a Rdio Scanner broadcaster

  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select **Rdio Scanner**.


  ### Enter the API details
    Fill in the host URL, API key, and system ID. The API key and system ID are configured inside the Rdio Scanner administration interface.


  ### Enable and save
    Check **Enabled**, then click **Save**. Completed calls upload to Rdio Scanner automatically.


> **Tip:**
  Enable the **Rdio Scanner** diagnostic category under **Application** > **Diagnostics (Logging)** in User Preferences to get detailed upload activity in the application log.

***

## OpenMHz

OpenMHz is an online radio archive platform at `openmhz.com`. SDRTrunk Kennebec uploads completed call recordings to the OpenMHz API at `https://api.openmhz.com`.

**Required fields**

| Field       | Description                                                              |
| ----------- | ------------------------------------------------------------------------ |
| Name        | A label for this configuration                                           |
| System Name | Your system's short name as registered on OpenMHz (e.g. `countysheriff`) |
| API Key     | The API key issued by OpenMHz for your system                            |

The production endpoint is pre-filled and does not need to be changed.

### Adding an OpenMHz broadcaster

  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select **OpenMHz**.


  ### Enter the API details
    Fill in the system name and API key from your OpenMHz account.


  ### Enable and save
    Check **Enabled**, then click **Save**.


***

## ThinLine Radio

ThinLine Radio is a call archive platform that shares the same call upload API structure as Rdio Scanner. SDRTrunk Kennebec uploads completed call recordings via HTTPS.

**Required fields**

| Field     | Description                                      |
| --------- | ------------------------------------------------ |
| Name      | A label for this configuration                   |
| Host      | The ThinLine Radio server URL                    |
| API Key   | Your ThinLine Radio API key                      |
| System ID | The numeric system ID assigned by ThinLine Radio |

> **Note:**
  The host field defaults to `http://localhost`. Update it to the ThinLine Radio server address before enabling.

### Adding a ThinLine Radio broadcaster

  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select **ThinLine Radio**.


  ### Enter the API details
    Fill in the host URL, API key, and system ID provided by your ThinLine Radio account.


  ### Enable and save
    Check **Enabled**, then click **Save**.


> **Tip:**
  ThinLine Radio debug logging is enabled by default in SDRTrunk Kennebec, so live streaming sessions produce full diagnostic output in the application log without any additional configuration.

***

## IAmResponding

IAmResponding is a first-responder alerting platform that includes a **Two Tone Detect** feature. SDRTrunk Kennebec streams raw PCM audio to the IAmResponding Two Tone Detect listener over UDP on the local machine.

> **Warning:**
  IAmResponding is a **local, Windows-only** integration. SDRTrunk Kennebec sends audio to a UDP socket on the same computer; it does not transmit audio over a network. This integration requires that the IAmResponding Two Tone Detect client software is running on the same Windows machine as SDRTrunk Kennebec.

**Required fields**

| Field | Description                                                             |
| ----- | ----------------------------------------------------------------------- |
| Name  | A label for this configuration                                          |
| Host  | The UDP destination host—typically `127.0.0.1` for local delivery       |
| Port  | The UDP port the IAmResponding Two Tone Detect software is listening on |

**Audio format:** Raw 16-bit PCM audio is sent over UDP. No encoding or compression is applied—IAmResponding Two Tone Detect expects raw PCM input.

### Adding an IAmResponding broadcaster

  ### Confirm IAmResponding is running
    Make sure the IAmResponding Two Tone Detect client is installed, configured, and running on the same Windows machine as SDRTrunk Kennebec.


  ### Add a new broadcaster
    Go to **View** > **Streaming**, click **+**, and select **IAmResponding**.


  ### Enter the connection details
    Set the host to `127.0.0.1` (or whichever loopback/local address IAmResponding is bound to) and enter the UDP port IAmResponding is listening on.


  ### Enable and save
    Check **Enabled**, then click **Save**.