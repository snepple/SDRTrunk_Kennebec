# ThinLine Radio Integration

> Send your completed radio calls to ThinLine Radio to build a beautiful, searchable archive of your local radio system!

ThinLine Radio is an awesome website that acts like a massive library for radio calls. Instead of missing a call while you step away from the computer, SDRTrunk Kennebec can send every single call to ThinLine Radio where you can play it back later!

## What is Uploaded?

When a call finishes, Kennebec sends:
* The actual audio (as a tiny MP3 file)
* Who was talking (the Talkgroup and Radio ID)
* ⏱ When the call happened

## Setup Instructions

Before you start, you will need to log into your ThinLine Radio account and get your **API Key** and your **System ID**.

1. Go to **View** > **Streaming** in Kennebec.
2. Click the **+** button and select **ThinLine Radio**.
3. Fill in the blanks:

| Field | What to type |
|---|---|
| **Name** | A label for you, like `My ThinLine Stream` |
| **ThinLine Radio URL** | The web address of your server (e.g., `http://thinline.myorg.com`) |
| **API Key** | Your secret ThinLine Radio password (API key) |
| **System ID** | The ID number for your radio system |

4. Toggle **Enabled** to on, and click **Save**.

> **Important:**
> Be sure to change the ThinLine Radio URL! If you leave it blank, Kennebec will try to send the calls to your own computer by mistake.

## Troubleshooting

Kennebec will automatically test the connection when it starts. 
* If you see an error in the logs about "Authentication", double check that your API Key was typed in perfectly!
* If your internet cuts out, don't worry! Kennebec will keep trying to reconnect every 5 seconds until it succeeds.
