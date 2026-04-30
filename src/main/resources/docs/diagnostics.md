# Enable diagnostics and debug logging in SDRTrunk Kennebec

> Use the Diagnostics preferences panel in SDRTrunk Kennebec to enable per-category DEBUG logging at runtime without restarting the application.

SDRTrunk Kennebec's Diagnostics panel gives you fine-grained control over debug logging for individual subsystems without touching configuration files or restarting the application. Changes take effect the moment you toggle a checkbox, making it straightforward to capture detailed logs for a specific component while a problem is actively occurring.

## Opening the Diagnostics panel

  ### Open User Preferences
    In the main application window, open the **View** menu and select **User Preferences**.


  ### Navigate to Diagnostics
    In the left sidebar, under the **Application** section header, click **Diagnostics (Logging)**.


The panel displays a description, a volume warning, a master toggle, and a grid of per-category checkboxes.

***

## Per-category toggles

Each row in the Diagnostics panel represents a named subsystem. Checking a box sets that subsystem's Logback logger to `DEBUG`. Unchecking it returns the logger to `INFO`.

| Category label           | Logger namespace                                   | Default  |
| ------------------------ | -------------------------------------------------- | -------- |
| Zello streaming          | `io.github.dsheirer.audio.broadcast.zello`         | Enabled  |
| ThinLine Radio streaming | `io.github.dsheirer.audio.broadcast.thinlineradio` | Enabled  |
| Rdio Scanner streaming   | `io.github.dsheirer.audio.broadcast.rdioscanner`   | Disabled |
| SDRPlay / RSP tuners     | `io.github.dsheirer.source.tuner.sdrplay`          | Disabled |
| Nooelec / RTL-SDR tuners | `io.github.dsheirer.source.tuner.rtl`              | Disabled |
| Channelizer / DDC        | `io.github.dsheirer.dsp.filter.channelizer`        | Disabled |
| Tuner manager / pool     | `io.github.dsheirer.source.tuner.manager`          | Disabled |
| P25 decoder              | `io.github.dsheirer.module.decode.p25`             | Disabled |
| NBFM / audio output      | `io.github.dsheirer.audio`                         | Disabled |

> **Info:**
  **Zello streaming** and **ThinLine Radio streaming** are enabled by default. This matches the `logback.xml` startup defaults so that live streaming sessions always have full diagnostic output available.

When a category is enabled, every logger whose name starts with the listed namespace is switched to `DEBUG`. For example, enabling **P25 decoder** captures debug output from all classes under `io.github.dsheirer.module.decode.p25`.

***

## Master "Enable ALL diagnostics" checkbox

The **Enable ALL diagnostics categories** checkbox at the top of the panel is a convenience shortcut. Checking it enables every category simultaneously; unchecking it disables every category simultaneously.

> **Warning:**
  Enabling all categories at once generates extremely large log files — potentially hundreds of megabytes per day when P25 traffic is active. Only enable the categories you are actively debugging, and disable them when you are done.

The master checkbox reflects the current state automatically: it appears checked only when every individual category is enabled, and unchecked the moment any single category is turned off.

***

## How runtime log level control works

SDRTrunk Kennebec applies diagnostics preference changes directly to the running logging system. This means:

* Changes take effect **immediately** — no application restart is needed.
* No configuration files on disk are modified.
* Your selections are persisted and reapplied automatically on the next startup, before the first log line is written.

> **Note:**
  Because diagnostics preferences are restored on startup, the categories you had enabled in your last session are active from the very first log entry after relaunch.

***

## Sharing logs for support

When filing a bug report or requesting help, attach the relevant log file from your SDRTrunk logs directory (default: `~/SDRTrunk/logs/`).

  ### Enable the relevant category
    Open **User Preferences > Diagnostics (Logging)** and enable the category that matches the subsystem you are troubleshooting. For example, enable **Zello streaming** if you are reporting a Zello connection issue.


  ### Reproduce the problem
    Perform the action that triggers the issue. Because changes take effect immediately, logs capture from the moment you enable the toggle.


  ### Locate the log file
    Navigate to `~/SDRTrunk/logs/` and find the most recent `sdrtrunk.log` file.


  ### Attach to your report
    Attach the log file to your issue on the project's GitHub repository. If the file is very large, consider filtering for lines from the relevant logger namespace (shown in the second column of the category table above).


> **Tip:**
  Disable diagnostics categories you no longer need after collecting logs. Leaving DEBUG enabled for high-volume categories like **P25 decoder** or **Channelizer / DDC** during normal operation will quickly fill your disk.