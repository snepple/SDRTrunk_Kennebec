# Upload Trunked Call Recordings to ThinLine Radio

ThinLine Radio is a call archive platform that accepts trunked radio call recordings via an HTTP upload API. SDRTrunk Kennebec pushes each completed call recording — encoded as MP3 — to your ThinLine Radio server along with metadata including talkgroup, timestamp, frequency, and radio ID. The integration uses the same multipart form-data upload structure as Rdio Scanner, so the setup process will be familiar if you have configured either platform before.

## Prerequisites

Before adding a ThinLine Radio broadcaster in SDRTrunk Kennebec, you need:

- A ThinLine Radio server accessible from the machine running SDRTrunk Kennebec
- An API key issued by ThinLine Radio for your account
- The numeric system ID assigned by ThinLine Radio for the radio system you are uploading

## Add the Broadcaster

  **1. Open the Streaming Editor**

    In SDRTrunk Kennebec, go to **View** > **Streaming**.

  **2. Add a ThinLine Radio broadcaster**

    Click **+** and select **ThinLine Radio**.

  **3. Enter the configuration**

    Fill in the following fields in the configuration panel:

    | Field | Description |
    |---|---|
    | **Name** | A label for this configuration, for example `My ThinLine Stream` |
    | **ThinLine Radio URL** | The base URL of your ThinLine Radio server, for example `http://thinline.myorg.com`. The path `/api/call-upload` is appended automatically |
    | **API Key** | Your ThinLine Radio API key |
    | **System ID** | The numeric system ID assigned by ThinLine Radio |
    | **Max Recording Age** | The maximum age (in seconds) of a recording that will be uploaded. Recordings older than this value are discarded rather than uploaded |

  **4. Enable and save**

    Toggle **Enabled** on, then click **Save**. SDRTrunk Kennebec begins uploading completed call recordings to your ThinLine Radio server.


> **Note**
>
  The **ThinLine Radio URL** field defaults to `http://localhost` if you leave it blank. Update it to the actual address of your ThinLine Radio server before enabling the broadcaster.

## Connection Testing

When SDRTrunk Kennebec first connects to your ThinLine Radio server on startup, it sends a test request to verify the API key and system ID are valid. A successful test receives the response `incomplete call data: no talkgroup`, which confirms the server accepted the credentials. If the response differs, the broadcaster enters an error state and logs the server response.

SDRTrunk Kennebec retries the connection every 5 seconds when it is in an error state, so transient server unavailability recovers automatically once the server comes back online.

## Diagnostic Logging

ThinLine Radio debug logging is **enabled by default** in SDRTrunk Kennebec. Every upload attempt — successful or failed — produces diagnostic output in the application log, including HTTP status codes and response bodies. You do not need to change any settings to capture this information.

To view the log output, check the SDRTrunk Kennebec application log. Look for lines prefixed with `ThinLine Radio API` for upload results:

- `Call imported successfully.` — the call was accepted
- `duplicate call rejected` — the server already has this call; no action needed
- Any other response indicates an upload failure; the HTTP status code and full response body are included

> **Tip**
>
  If uploads are consistently failing, compare the API key and system ID in the SDRTrunk Kennebec configuration against the values shown in your ThinLine Radio account settings. A mismatch is the most common cause of authentication errors.
