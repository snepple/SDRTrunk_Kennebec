# Enable Gemini AI for automated monitoring

> Connect SDRTrunk Kennebec to Google Gemini AI for automatic channel filter tuning, log analysis, audio quality alerts, and system health monitoring.

SDRTrunk Kennebec includes an optional integration with Google Gemini AI that adds a layer of automated monitoring on top of its core radio decoding engine. When enabled, the AI can tune channel filters automatically, translate cryptic log output into plain English, watch system resource usage, assess decoded audio quality, and alert you when audio becomes unintelligible — all running in the background while you focus on monitoring.

## What AI features are available

  ### Auto channel filter configuration
    Gemini AI analyzes your active channels and automatically adjusts filter settings to optimize decode quality. This removes the need to manually tune squelch, pass-band, and other filter parameters for each channel.


  ### Intelligent log analysis
    When enabled, the AI reviews SDRTrunk's application logs and translates stack traces and warning messages into plain-English explanations with suggested fixes. You no longer need to search the internet to understand what a cryptic Java exception means.


  ### System health advisor
    A background agent polls CPU and memory usage at regular intervals. If system resources are under pressure — for example, CPU usage exceeds 80% or memory usage exceeds 80% — the advisor displays optimization suggestions directly in the application, such as reducing sample rates or closing unused tuners.


  ### Audio quality monitoring
    The AI monitors the last five decoded audio files from each active channel, which SDRTrunk stores temporarily on disk when this feature is enabled. If a channel's audio becomes unintelligible, Gemini flags it and notifies you so you can investigate whether the issue is a decoding problem, a weak signal, or a hardware fault.


> **Info:**
  When AI features are enabled, SDRTrunk Kennebec saves the last five audio files from each active channel to your local disk to allow for audio quality review. These files are managed automatically and cleared as new audio arrives.

## Prerequisites

* A Google account with access to Google AI Studio
* An internet connection from the machine running SDRTrunk Kennebec
* A valid Gemini API key (free tier is sufficient for most use cases)

## Get a Gemini API key

  ### Visit Google AI Studio
    Open a browser and go to [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey). Sign in with your Google account.


  ### Create an API key
    Click **Create API key** and follow the prompts. Copy the generated key — you will need it in the next section.


> **Warning:**
  Treat your Gemini API key like a password. Do not share it or commit it to a public repository. SDRTrunk Kennebec stores it in your local user preferences using a masked password field.

## Enable AI features in SDRTrunk Kennebec

  ### Open User Preferences
    Go to **View** → **User Preferences** in the menu bar.


  ### Navigate to AI Preferences
    Select **AI** in the left sidebar of the User Preferences panel.


  ### Enable AI features
    Turn on the **Enable AI Features** toggle. The remaining AI settings become visible once this toggle is on.


  ### Enter your Gemini API key
    Paste your API key into the **Gemini API Key** field. The field is masked for security.


  ### Test the API key
    Click **Test**. SDRTrunk Kennebec contacts the Gemini API to verify the key. If the test passes, the **Gemini Model** dropdown populates automatically with all models available to your account.


  ### Select a Gemini model
    Choose a model from the **Gemini Model** dropdown. The default is `models/gemini-1.5-flash`, which balances speed and capability for the monitoring tasks SDRTrunk performs. You can also type a model name directly into the field if you prefer a specific version.


> **Tip:**
  `models/gemini-1.5-flash` is a good default for most users. It is fast, cost-efficient, and well-suited for the short-form analysis tasks SDRTrunk Kennebec uses AI for. If you need higher accuracy for log analysis or audio quality review, consider `models/gemini-1.5-pro`.

## Enable or disable individual AI features

Each AI capability has its own toggle so you can enable only what you need:

| Toggle                           | What it controls                                                                                      |
| -------------------------------- | ----------------------------------------------------------------------------------------------------- |
| **Enable AI Features**           | Master switch. Disabling this turns off all AI functionality regardless of individual toggles.        |
| **Intelligent Log Analysis**     | Enables AI-powered translation of application logs and stack traces into plain-English explanations.  |
| **Enable System Health Advisor** | Starts the background agent that monitors CPU and memory usage and displays optimization suggestions. |

Audio quality monitoring and automatic channel filter configuration activate alongside the master **Enable AI Features** toggle and do not have separate controls.

## What the system health advisor monitors

The system health advisor runs on a timer in the background and reads CPU and total/free memory from the operating system. It displays a live status panel showing:

* **Status** — whether the advisor is active
* **CPU usage** — current processor load as a percentage
* **Memory usage** — used memory as a percentage of total available RAM
* **Optimization suggestions** — plain-text guidance when thresholds are exceeded

When CPU usage exceeds 80%, the advisor recommends reducing sample rates or disabling waterfall displays to prevent audio dropouts. When memory usage exceeds 80%, it suggests closing unused features or tuners.

> **Note:**
  The system health advisor displays its output within the SDRTrunk Kennebec application window. It does not send suggestions via Telegram or Email — for remote alerting on system health, use the standard [error notifications](/alerts/notifications) feature.

## How log analysis works

When **Intelligent Log Analysis** is enabled, SDRTrunk Kennebec sends relevant log entries to the Gemini API for interpretation. The AI returns a plain-English summary of what each warning or error means and what action — if any — you should take. Results appear in the **Logs** viewer alongside the original log output.

> **Warning:**
  Log entries sent to the Gemini API may contain application state information such as channel names, protocol details, and error messages. Review Google's data usage policy for the Gemini API before enabling log analysis if you are operating in a sensitive environment.

## Troubleshooting

  ### Test button returns a failure
    Confirm that the API key was copied correctly with no leading or trailing spaces. Check that your machine has an active internet connection. If the key was just created, wait a minute and try again — new keys occasionally take a moment to become active.


  ### Model dropdown is empty after a successful test
    If the test passes but no models appear, your API key may have restricted permissions. Visit Google AI Studio and verify that the key has access to the Gemini model list endpoint. You can also type a model name such as `models/gemini-1.5-flash` directly into the field.


  ### System health advisor shows no data
    The advisor requires the master **Enable AI Features** toggle and the **Enable System Health Advisor** toggle to both be on. If the status panel shows no data, verify both are enabled and that you have a valid API key entered.


  ### AI features are available but AI analysis seems inactive
    Make sure the specific sub-feature toggle is enabled (e.g., **Intelligent Log Analysis**). Also confirm that your Gemini API key has not been revoked or exhausted its quota in Google AI Studio.