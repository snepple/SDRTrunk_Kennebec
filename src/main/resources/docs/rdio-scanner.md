# Upload Trunked Call Recordings to Rdio Scanner

Rdio Scanner is a self-hosted web application that lets you browse, search, and replay trunked radio calls from any browser or mobile device. Rather than streaming live audio, SDRTrunk Kennebec pushes each completed call recording to your Rdio Scanner instance over HTTP as an MP3 file along with metadata such as talkgroup ID, timestamp, frequency, and radio ID. The call then appears immediately in the Rdio Scanner interface for playback.

## Prerequisites

Before adding a Rdio Scanner broadcaster in SDRTrunk Kennebec, you need:

- A running Rdio Scanner instance accessible from the machine running SDRTrunk Kennebec
- An API key configured in the Rdio Scanner administration interface
- The numeric system ID assigned to your radio system inside Rdio Scanner

## Add the Broadcaster

**1. Open the Streaming Editor**

In SDRTrunk Kennebec, go to **View** > **Streaming**.

**2. Add an Rdio Scanner broadcaster**

Click **+** and select **Rdio Scanner**.

**3. Enter the configuration**

Fill in the following fields in the configuration panel:

| Field | Description |
|---|---|
| **Name** | A label for this configuration, for example `My Rdio Scanner Stream` |
| **Rdio Scanner URL** | The base URL of your Rdio Scanner server, for example `http://192.168.1.50` or `https://scanner.example.com`. The path `/api/call-upload` is appended automatically |
| **API Key** | The API key configured in your Rdio Scanner administration interface |
| **System ID** | The numeric system ID assigned in Rdio Scanner for the radio system you are uploading |
| **Max Recording Age (seconds)** | The maximum age of a recording that will be uploaded. Recordings older than this value are discarded rather than uploaded |

**4. Enable and save**

Toggle **Enabled** on, then click **Save**. SDRTrunk Kennebec begins uploading completed call recordings to your Rdio Scanner instance.

> **Note:**
> The **Rdio Scanner URL** field defaults to `http://localhost` if you leave it blank. Update it to the actual address of your Rdio Scanner server before enabling the broadcaster — uploads to `localhost` will fail unless Rdio Scanner is running on the same machine.

## How Call Data Is Sent

Each time a monitored trunked call ends, SDRTrunk Kennebec encodes the recording as MP3 and sends an HTTP POST request to your Rdio Scanner server. The upload includes the following metadata alongside the audio file:

| Metadata field | Description |
|---|---|
| `key` | Your Rdio Scanner API key |
| `system` | The configured system ID |
| `dateTime` | Call start time as a Unix timestamp (seconds) |
| `talkgroupId` | Talkgroup identifier, converted to Radio Reference format |
| `source` | Radio ID of the transmitting unit |
| `frequency` | Channel frequency in Hz |
| `talkerAlias` | Talker alias if broadcast by the radio system |
| `talkgroupLabel` | Alias name from your SDRTrunk alias list |
| `talkgroupGroup` | Alias group from your SDRTrunk alias list |
| `systemLabel` | System name from your SDRTrunk channel configuration |
| `patches` | Patch group members, if the call was on a patch group |

## Diagnostic Logging

Enable the **Rdio Scanner** diagnostic category under **Application** > **Diagnostics (Logging)** in User Preferences to write detailed upload activity — including HTTP response codes and error messages — to the application log. This is the first step when troubleshooting failed uploads.
