# Enable Diagnostics and Debug Logging in SDRTrunk

SDRTrunk Kennebec's **Diagnostics** panel gives you fine-grained control over debug logging for individual subsystems without touching configuration files or restarting the application. Changes take effect the moment you toggle a checkbox, making it straightforward to capture detailed logs for a specific component while a problem is actively occurring.

## Open the Diagnostics panel

**1. Open User Preferences**

In the main application window, open the **View** menu and select **User Preferences**.

**2. Navigate to Diagnostics**

In the left sidebar, under the **Application** section header, click **Diagnostics (Logging)**.

The panel displays a description, a log volume warning, a master toggle, and a grid of per-category checkboxes.

---

## Per-category toggles

Each row in the panel represents a named subsystem. Checking the box sets that subsystem's logger to `DEBUG`. Unchecking it returns the logger to `INFO`.

| Category | Default | What it captures |
| --- | --- | --- |
| Zello streaming | Enabled | WebSocket session activity, Opus encoder state, stream start/stop events |
| ThinLine Radio streaming | Enabled | Upload requests, HTTP responses, retry attempts |
| Rdio Scanner streaming | Disabled | Upload activity and API responses |
| SDRPlay / RSP tuners | Disabled | SDRPlay API events, gain changes, device errors |
| RTL-SDR tuners | Disabled | USB transfers, gain settings, frequency corrections |
| Channelizer | Disabled | Digital down-conversion and polyphase filter activity |
| Tuner manager | Disabled | Device discovery, assignment, and removal events |
| P25 decoder | Disabled | Message decoding, NAC filtering, control channel events |
| NBFM / audio output | Disabled | Squelch state, audio filter chain, output events |

> **Note:**
> **Zello streaming** and **ThinLine Radio streaming** are enabled by default so that live streaming sessions capture full diagnostic output without any additional configuration.

When you enable a category, debug output is captured for all activity in that subsystem.

---

## Master "Enable ALL diagnostics" checkbox

The **Enable ALL diagnostics categories** checkbox at the top of the panel is a convenience shortcut. Checking it enables every category simultaneously; unchecking it disables every category simultaneously. The master checkbox reflects the current state automatically: it appears checked only when every individual category is enabled, and unchecked the moment any single category is turned off.

> **Warning:**
> Enabling all categories at once generates extremely large log files — potentially hundreds of megabytes per day when P25 traffic is active. Enable only the categories you are actively debugging, and disable them when you are done.

---

## How runtime log level control works

SDRTrunk Kennebec applies diagnostics preference changes directly to the running logging system. This means:

- Changes take effect **immediately** — no application restart is needed.
- No configuration files on disk are modified.
- Your selections are persisted and reapplied automatically on the next startup, before the first log line is written.

> **Note:**
> Because diagnostics preferences are restored on startup, the categories you had enabled in your last session are active from the very first log entry after relaunch.

---

## View diagnostic output in the Logs panel

To see the debug output in real time, open **View → Logs** from the main menu bar. The Logs panel streams all log output and filters by level and logger namespace. Enable a diagnostics category, then switch to the Logs panel to watch the output immediately.

---

## Collecting logs for a bug report

**1. Enable the relevant category**

Open **User Preferences → Diagnostics (Logging)** and enable the category that matches the subsystem you are troubleshooting. For example, enable **Zello streaming** if you are reporting a Zello connection issue.

**2. Reproduce the problem**

Perform the action that triggers the issue. Because changes take effect immediately, logs capture from the moment you enable the toggle.

**3. Locate the log file**

Navigate to `~/SDRTrunk/logs/` and find the most recent `sdrtrunk.log` file.

**4. Attach to your report**

Attach the log file to your issue on the [GitHub repository](https://github.com/snepple/SDRTrunk_Kennebec/issues). If the file is large, you can filter it by the component name shown in the log lines.

> **Tip:**
> Disable diagnostics categories you no longer need after collecting logs. Leaving DEBUG enabled for high-volume categories like **P25 decoder** or **Channelizer** during normal operation will quickly fill your disk.
