# Tuner self-healing and automatic USB reset in SDRTrunk

> How SDRTrunk Kennebec automatically recovers locked or failed SDR tuners from USB errors, including the Windows 10+ PowerShell scheduled task USB reset.

SDRTrunk Kennebec includes automatic tuner recovery logic designed to minimize disruption when a USB tuner encounters a software-level error. Instead of marking the device as permanently failed and waiting for you to intervene, the application attempts to stop the tuner, clear the error state, and restart it — all without any action on your part. On Windows 10 and Windows 11, a supplementary PowerShell-based USB monitor provides a deeper hardware-level reset capability for tuners that become physically locked in the operating system.

## What triggers self-healing

Self-healing is triggered by two distinct USB error conditions:

  ### USB transfer buffer exhaustion

    All USB bulk transfer buffers fail, leaving no buffers available for continued streaming. This typically indicates a transient USB communication breakdown.


  ### USB device disconnection

    The application detects that the device is no longer responding on the USB bus — for example, after a USB reset, cable issue, or device power fault.


Any other error message moves the tuner directly to the `Error` status and does not trigger automatic recovery.

## Recovery behavior

The recovery logic differs depending on which error was detected.

### Transfer buffer exhaustion recovery

  ### Tuner stops
    The application stops the tuner immediately and sets its status to `Recovering`.


  ### Scheduled retry
    A scheduled task runs every **3 minutes**, up to a maximum of **5 attempts**.


  ### Restart attempt
    Each attempt clears the error state and reinitializes the tuner from scratch, including USB device setup and hardware configuration.


  ### Success or permanent failure
    If the tuner reaches `Enabled` status, channels automatically resume on it. If all 5 attempts fail, the tuner is permanently set to `Error` with the message "Permanent USB Error - Transfer Buffers Exhausted".


### Device disconnection recovery

  ### Tuner stops
    The application stops the tuner and sets its status to `Recovering`. The USB device handle is released to avoid resource lockups.


  ### Rapid retry phase (first 15 minutes)
    The application retries the restart every **5 seconds** during the first 15 minutes after the disconnect is detected.


  ### Slow retry phase (15 to 45 minutes)
    If the device has not reconnected after 15 minutes, the retry interval increases to **5 minutes**.


  ### Success or timeout
    A successful reconnect restores the tuner to `Enabled` and channels resume automatically. If the device is still unreachable after **45 minutes**, the tuner is permanently set to `Error` with the message "Permanent USB Error - Device Disconnected".


> **Note:**
  The disconnect recovery schedule is designed to handle brief USB resets or cable reconnections quickly, while still accommodating longer hardware failures without consuming excessive system resources.

## Windows PowerShell USB monitor

On Windows 10 and Windows 11, SDRTrunk Kennebec starts an additional background process — the **USB Monitor** — that can perform a hardware-level reset of the USB device through the Windows operating system. This complements the software-level self-healing described above and is intended for situations where libusb has lost control of the device and a software restart alone is insufficient.

### How it works

  ### Script extraction
    At startup, SDRTrunk Kennebec extracts a PowerShell script (`usb_monitor.ps1`) from the application resources and writes it to the `scripts/` subdirectory inside the application root folder.


  ### Configuration file
    A JSON configuration file (`usb_monitor_config.json`) is written alongside the script. It contains the application's process ID and the path to the USB monitor log file.


  ### Scheduled task creation
    The application checks whether a Windows Scheduled Task named `SDRTrunk_UsbMonitor_<username>` already exists and points to the current script path. If it does not, SDRTrunk Kennebec prompts for a **UAC elevation dialog** (one time) and creates the task to run under the `NT AUTHORITY\SYSTEM` account, which has the privileges needed to reset USB devices.


  ### Task execution
    The scheduled task is started silently. The monitor script watches for tuner failures reported by the application and issues OS-level USB device reset commands when needed.


> **Warning:**
  Creating the scheduled task requires administrator (UAC) approval. SDRTrunk Kennebec will display a UAC prompt the first time this task needs to be registered. You must approve the prompt for the USB monitor to function. If you deny the prompt, the software-level self-healing still operates normally, but the OS-level reset will not be available.

### OS requirement

The USB monitor is **only active on Windows 10 and Windows 11**. On Windows versions below 10, on Linux, and on macOS, the USB monitor is skipped and the software-level recovery operates alone. A log message records this when the application starts.

## How to tell if a reset occurred

Look for the following entries in the SDRTrunk Kennebec application log:

| Log message                                                      | Meaning                                                                       |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `Tuner Error - Initiating Recovery - <id>`                       | Self-healing has started for a transfer buffer exhaustion error.              |
| `Tuner Error - Device Disconnected - Initiating Recovery - <id>` | Self-healing has started after a USB disconnect.                              |
| `Attempting tuner recovery for <id> - Attempt N of 5`            | A software restart is in progress.                                            |
| `Successfully recovered tuner <id>`                              | The tuner came back online and channels resumed automatically.                |
| `Failed to recover tuner <id> after 5 attempts`                  | The tuner could not be recovered; it is now permanently in the `Error` state. |
| `USB Monitor is only supported on Windows 10 or newer`           | The current OS does not support the PowerShell USB monitor.                   |
| `Successfully created scheduled task '<name>'`                   | The Windows USB monitor scheduled task was registered.                        |

The USB monitor also writes its own log to `sdrtrunk_usb_monitor.log` inside the application log directory.

## Limitations

* Self-healing restarts the tuner from scratch. Any channels that were assigned to the tuner at the time of the error will need to be reassigned or will resume automatically when the tuner comes back online, depending on your channel configuration.
* The Windows USB monitor requires a one-time UAC approval and a `NT AUTHORITY\SYSTEM` scheduled task. Environments with strict Group Policy or endpoint security tools that block scheduled tasks may prevent the monitor from running.
* Recovery is not attempted for errors other than transfer buffer exhaustion and device disconnection. Configuration errors, unknown tuner types, and other startup failures move the tuner directly to the `Error` status.
* On macOS Tahoe (OS version 26), there is a known libusb compatibility issue that can prevent USB tuner detection. See the [RTL-SDR setup guide](/hardware/rtl-sdr) for the Homebrew workaround.