# Gemini AI: Automated Monitoring and Log Analysis

SDRTrunk Kennebec includes an optional integration with Google Gemini AI that adds automated monitoring on top of the core radio decoding engine. When enabled, the AI can tune channel filters automatically, translate cryptic log output into plain English, watch CPU and memory usage, assess decoded audio quality, and alert you when audio becomes unintelligible — all running in the background while you monitor.

## What AI features are available

  **Auto Channel Filter Configuration**

    Gemini AI analyzes your active channels and automatically adjusts filter settings to optimize decode quality, removing the need to manually tune squelch, pass-band, and other filter parameters.

  **Intelligent Log Analysis**

    The AI reviews SDRTrunk's application logs and translates stack traces and warning messages into plain-English explanations with suggested fixes, displayed directly in the **Logs** viewer.

  **System Health Advisor**

    A background agent polls CPU and memory usage at regular intervals and displays optimization suggestions in the application when resource pressure exceeds 80%.

  **Audio Quality Monitoring**

    The AI monitors the last five decoded audio files from each active channel. If a channel's audio becomes unintelligible, Gemini flags it so you can investigate whether the cause is a decoding problem, weak signal, or hardware fault.


> **Info**
>
  When AI features are enabled, SDRTrunk Kennebec saves the last five audio files from each active channel to your local disk to allow for audio quality review. These files are managed automatically and cleared as new audio arrives.

## Prerequisites

Before enabling Gemini AI, confirm you have the following:

- A Google account with access to [Google AI Studio](https://aistudio.google.com/app/apikey)
- An active internet connection from the machine running SDRTrunk Kennebec
- A valid Gemini API key (the free tier is sufficient for most use cases)

## Get a Gemini API key

  **1. Open Google AI Studio**

    In a browser, go to [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) and sign in with your Google account.

  **2. Create an API key**

    Click **Create API key** and follow the prompts. Copy the generated key — you will need it in the next section.


> **Warning**
>
  Treat your Gemini API key like a password. Do not share it or store it in a public location. SDRTrunk Kennebec stores the key in your local user preferences using a masked password field.

## Enable AI in SDRTrunk Kennebec

  **3. Open User Preferences**

    Go to **View** → **User Preferences** in the menu bar.

  **4. Navigate to AI Preferences**

    Select **AI** in the left sidebar of the **User Preferences** panel.

  **5. Enable AI Features**

    Turn on the **Enable AI Features** toggle. The remaining AI settings become visible once this toggle is on.

  **6. Enter your Gemini API key**

    Paste your API key into the **Gemini API Key** field. The field is masked for security.

  **7. Test the API key**

    Click **Test**. SDRTrunk Kennebec contacts the Gemini API to verify the key. If the test passes, the **Gemini Model** dropdown populates automatically with all models available to your account.

  **8. Select a Gemini model**

    Choose a model from the **Gemini Model** dropdown. The default is `models/gemini-1.5-flash`, which balances speed and capability for the tasks SDRTrunk Kennebec performs. You can also type a model name directly into the field if you prefer a specific version.


> **Tip**
>
  `models/gemini-1.5-flash` is a good default for most users — it is fast, cost-efficient, and well-suited for short-form analysis tasks. If you need higher accuracy for log analysis or audio quality review, consider `models/gemini-1.5-pro`.

## Enable or disable individual AI features

Each AI capability has its own toggle so you can enable only what you need:

| Toggle | What it controls |
| --- | --- |
| **Enable AI Features** | Master switch. Disabling this turns off all AI functionality regardless of individual toggles. |
| **Intelligent Log Analysis** | Enables AI-powered translation of application logs and stack traces into plain-English explanations with actionable fixes. |
| **Enable System Health Advisor** | Starts the background agent that monitors CPU and memory usage and displays optimization suggestions in the application. |

Audio quality monitoring and automatic channel filter configuration activate alongside the master **Enable AI Features** toggle and do not have separate controls.

## System health advisor

The system health advisor runs on a timer in the background and reads CPU load and total/free memory from the operating system. It displays a live status panel in the application showing:

- **Status** — whether the advisor is active
- **CPU Usage** — current processor load as a percentage
- **Memory Usage** — used memory as a percentage of total available RAM
- **Optimization Suggestions** — plain-text guidance when thresholds are exceeded

When CPU usage exceeds 80%, the advisor recommends reducing sample rates or disabling waterfall displays to prevent audio dropouts. When memory usage exceeds 80%, it suggests closing unused features or tuners. These suggestions appear highlighted in red directly in the application window.

> **Note**
>
  The system health advisor displays its output within the SDRTrunk Kennebec application window. It does not send suggestions via Telegram or Email. For remote alerting on system health, use the standard [error notifications](/docs/alerts/notifications) feature.

## How log analysis works

When **Intelligent Log Analysis** is enabled, SDRTrunk Kennebec sends relevant log entries to the Gemini API for interpretation. The AI returns a plain-English summary explaining what each warning or error means and what action — if any — you should take. Results appear in the **Logs** viewer alongside the original log output.

If the Gemini API quota for your selected model is exhausted, SDRTrunk Kennebec automatically downgrades to the next available model for the following request and notifies you in the log viewer.

> **Warning**
>
  Log entries sent to the Gemini API may contain application state information such as channel names, protocol details, and error messages. Review Google's data usage policy for the Gemini API before enabling log analysis if you are operating in a sensitive environment.

## Troubleshooting

  **Test button returns a failure**

    Confirm the API key was copied correctly with no leading or trailing spaces. Check that your machine has an active internet connection. If the key was just created, wait a minute and try again — new keys can take a moment to become active.

  **Model dropdown is empty after a successful test**

    If the test passes but no models appear, your API key may have restricted permissions. Visit Google AI Studio and verify the key has access to the Gemini model list endpoint. You can also type a model name such as `models/gemini-1.5-flash` directly into the **Gemini Model** field.

  **System health advisor shows no data**

    Both the master **Enable AI Features** toggle and the **Enable System Health Advisor** toggle must be on. If the status panel is blank, verify both are enabled and that a valid API key is entered.

  **AI analysis seems inactive despite being enabled**

    Make sure the specific sub-feature toggle is enabled — for example, **Intelligent Log Analysis**. Also confirm that your Gemini API key has not been revoked or exhausted its quota in Google AI Studio.
