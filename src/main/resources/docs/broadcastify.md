# Stream live radio audio to Broadcastify feeds and calls

> How to configure SDRTrunk Kennebec to stream live audio to Broadcastify Feeds and upload completed recordings to Broadcastify Calls.

SDRTrunk Kennebec supports two distinct Broadcastify integrations: **Broadcastify Feed**, which streams live audio to the Broadcastify network in real time, and **Broadcastify Call**, which uploads each completed call recording to the Broadcastify Calls API. You can configure one or both depending on what your feed account supports.

## Broadcastify Feed vs. Broadcastify Call

  ### Broadcastify Feed
    Broadcastify Feed delivers a continuous live audio stream to the Broadcastify network using an Icecast-compatible TCP source connection (compatible with Icecast 2.3.2). Listeners hear the audio in near real time through the Broadcastify website or mobile app.

    Use this when you want a traditional live "radio feed" experience where your listeners can tune in at any time and hear what is happening on the radio right now.

    **Required credentials**

    * Server hostname (provided by Broadcastify)
    * Port number
    * Mount point (the stream path, e.g. `/stream`)
    * Password
    * Feed ID (a numeric identifier assigned to your feed)

    **Audio format:** MP3 at 16 kbps, mono, 8000 Hz sample rate. These values are set automatically and do not require manual configuration.


  ### Broadcastify Call
    Broadcastify Call is a completed-call push service rather than a live stream. When each trunked radio call ends, SDRTrunk Kennebec encodes the recording as an MP3 file and uploads it to the Broadcastify Calls API (`https://api.broadcastify.com/call-upload`). The call then appears in the Broadcastify Calls interface with metadata such as talkgroup and timestamp.

    Use this when your Broadcastify account is provisioned for the Calls API and you want per-call recordings with metadata rather than a continuous live stream.

    **Required credentials**

    * API key (issued by Broadcastify)
    * System ID (your numeric Broadcastify system identifier)

    **Endpoint:** The production endpoint (`https://api.broadcastify.com/call-upload`) is pre-filled. A development endpoint (`https://api.broadcastify.com/call-upload-dev`) is available for testing.


## Setting up Broadcastify Feed

  ### Gather your feed credentials
    Log in to your Broadcastify account and locate the feed you want to connect. You need the following values from the feed's configuration page:

    * Hostname
    * Port
    * Mount point (e.g. `/stream` or a feed-specific path)
    * Password
    * Feed ID


  ### Add a Broadcastify Feed broadcaster
    In SDRTrunk Kennebec, go to **View** > **Streaming**, then click **+** and select **Broadcastify Feed**.


  ### Enter the connection details
    Fill in the following fields in the configuration panel:

    | Field       | Description                                                    |
    | ----------- | -------------------------------------------------------------- |
    | Name        | A label for this configuration—for example, "County Fire Main" |
    | Host        | The Broadcastify server hostname                               |
    | Port        | The server port number                                         |
    | Mount Point | The stream path, starting with `/`                             |
    | Password    | The source password for your feed                              |
    | Feed ID     | Your numeric Broadcastify feed ID                              |


  ### Enable verbose logging (optional)
    Toggle **Verbose Logging** if you need detailed diagnostic output for this feed in the application log. This is useful for troubleshooting connection or audio quality issues.


  ### Enable and save
    Check **Enabled**, then click **Save**. SDRTrunk Kennebec connects to the Broadcastify server immediately and begins streaming decoded audio.


> **Note:**
  SDRTrunk Kennebec sets the audio parameters for Broadcastify Feed automatically (16 kbps MP3, mono, 8000 Hz). You do not need to configure bit rate or sample rate manually.

## Setting up Broadcastify Call

  ### Gather your API credentials
    Log in to your Broadcastify account and locate the Calls API key and system ID for your system. These are separate from feed credentials.


  ### Add a Broadcastify Call broadcaster
    In SDRTrunk Kennebec, go to **View** > **Streaming**, click **+**, and select **Broadcastify Call**.


  ### Enter the configuration
    Fill in the following fields:

    | Field     | Description                                                        |
    | --------- | ------------------------------------------------------------------ |
    | Name      | A label for this configuration                                     |
    | API Key   | Your Broadcastify Calls API key                                    |
    | System ID | Your numeric Broadcastify system ID                                |
    | Host      | Leave at the default production endpoint unless directed otherwise |


  ### Enable and save
    Check **Enabled**, then click **Save**. SDRTrunk Kennebec will upload completed call recordings to the Broadcastify Calls API as they arrive.


## Testing the connection

  ### Broadcastify Feed — verifying the stream is live
    After enabling the broadcaster, check the **Broadcaster Status** panel (**View** > **Broadcaster Status**). The Feed entry should show **Connected** within a few seconds.

    You can also visit your feed's Broadcastify page while audio is being received to confirm the stream is active.


  ### Broadcastify Call — verifying uploads
    After enabling the broadcaster and receiving a trunked call, the status panel shows **Connected** once the first upload attempt succeeds. You can also check your Broadcastify Calls interface to confirm new call entries are appearing.


  ### Troubleshooting a Disconnected status
    If the status panel shows **Disconnected** or **Error**, verify the following:

    * The hostname, port, and mount point are exactly as shown in your Broadcastify feed settings.
    * The password is copied without leading or trailing spaces.
    * Port is open outbound on your network (common ports: 80, 8000).
    * Your Broadcastify account is active and the feed is provisioned.

    Enable **Verbose Logging** on the Broadcastify Feed configuration and review the application log for detailed error messages.


> **Warning:**
  The **delay** and **maximum recording age** fields are available in the configuration but should be left at their defaults unless you have been specifically advised to change them by Broadcastify support. Setting maximum recording age too low can cause call recordings to be discarded before they are uploaded.