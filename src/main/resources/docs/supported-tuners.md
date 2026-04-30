# Supported SDR tuners and hardware in SDRTrunk Kennebec

> Overview of all SDR hardware compatible with SDRTrunk Kennebec, including RTL-SDR, Airspy, HackRF, SDRPlay RSP devices, and Funcube Dongles.

SDRTrunk Kennebec supports a range of Software Defined Radio (SDR) hardware through two main connection pathways: USB (using libusb) and the SDRplay API. When you start the application, the tuner manager automatically scans for connected devices and brings each recognized tuner online. You can view and configure all discovered tuners from the **Tuners** panel.

## Connection types

  ### USB tuners

    Devices communicated with directly over USB using the libusb library. Includes RTL-SDR dongles, Airspy Mini/R2, and HackRF variants. Hot-plug is supported on platforms where libusb provides that capability.


  ### SDRplay RSP tuners

    Devices communicated with through the proprietary SDRplay API (`sdrplay_api`). Includes the full RSP product line. The API must be installed separately before SDRTrunk Kennebec can detect these devices.


> **Note:**
  SDRTrunk Kennebec also supports a **Recording** tuner type that replays previously captured IQ recordings. This is useful for testing and offline decoding.

## Supported tuner types

The following table lists every tuner type recognized by the `TunerType` enum, the connection method used, and the enum value referenced in configuration files and logs.

| Tuner                    | Chip / Model | Connection  | `TunerType` enum value    |
| ------------------------ | ------------ | ----------- | ------------------------- |
| Airspy Mini / R2         | R820T        | USB         | `AIRSPY_R820T`            |
| Airspy HF+               | HF+          | USB         | `AIRSPY_HF_PLUS`          |
| RTL-SDR (Elonics)        | E4000        | USB         | `ELONICS_E4000`           |
| RTL-SDR (Rafael Micro)   | R820T        | USB         | `RAFAELMICRO_R820T`       |
| RTL-SDR (Rafael Micro)   | R828D        | USB         | `RAFAELMICRO_R828D`       |
| RTL-SDR (Fitipower)      | FC0012       | USB         | `FITIPOWER_FC0012`        |
| RTL-SDR (Fitipower)      | FC0013       | USB         | `FITIPOWER_FC0013`        |
| RTL-SDR (FCI)            | FC2580       | USB         | `FCI_FC2580`              |
| HackRF One               | ONE          | USB         | `HACKRF_ONE`              |
| HackRF Jawbreaker        | Jawbreaker   | USB         | `HACKRF_JAWBREAKER`       |
| HackRF RAD1O             | RAD1O        | USB         | `HACKRF_RAD1O`            |
| HydraSDR                 | R828D        | USB         | `HYDRASDR_R828D`          |
| Funcube Dongle Pro       | —            | Sound card  | `FUNCUBE_DONGLE_PRO`      |
| Funcube Dongle Pro Plus  | —            | Sound card  | `FUNCUBE_DONGLE_PRO_PLUS` |
| SDRplay RSP1             | —            | SDRplay API | `RSP_1`                   |
| SDRplay RSP1A            | —            | SDRplay API | `RSP_1A`                  |
| SDRplay RSP1B            | —            | SDRplay API | `RSP_1B`                  |
| SDRplay RSP2             | —            | SDRplay API | `RSP_2`                   |
| SDRplay RSPduo (tuner 1) | —            | SDRplay API | `RSP_DUO_1`               |
| SDRplay RSPduo (tuner 2) | —            | SDRplay API | `RSP_DUO_2`               |
| SDRplay RSPdx            | —            | SDRplay API | `RSP_DX`                  |
| IQ Recording             | —            | File        | `RECORDING`               |

> **Info:**
  Tuner types labeled `UNKNOWN` appear in logs when the RTL2832 chipset is detected but the embedded tuner IC cannot be identified. The device is treated as unusable until a recognized embedded tuner is found.

## Tuner status values

Each discovered tuner carries a `TunerStatus` value that describes its current state. You will see these labels in the tuner panel and in log output.

| Status       | Meaning                                                                                   |
| ------------ | ----------------------------------------------------------------------------------------- |
| `Enabled`    | The tuner is running and available to receive channel assignments.                        |
| `Disabled`   | You have manually disabled (blacklisted) this tuner. It will not be used.                 |
| `Recovering` | The tuner encountered a recoverable USB error and is attempting to restart automatically. |
| `Error`      | The tuner failed and could not be recovered. Check the application log for details.       |
| `Removed`    | The USB device was physically unplugged.                                                  |

## Per-device setup guides

  ### RTL-SDR

    Driver setup, sample rate, gain, and PPM correction for RTL2832-based dongles.


  ### Airspy and HackRF

    Driver installation and configuration options for Airspy Mini/R2 and HackRF variants.


  ### Tuner self-healing

    How SDRTrunk Kennebec automatically recovers locked or failed tuners, including the Windows PowerShell reset.