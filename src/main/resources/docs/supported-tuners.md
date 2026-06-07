# Supported SDR Tuners and Hardware in SDRTrunk Kennebec

SDRTrunk Kennebec supports a wide range of Software Defined Radio (SDR) hardware through three connection pathways: USB via libusb, the proprietary SDRplay API, and sound card input for Funcube Dongles. When you launch the application, the tuner manager scans for connected devices automatically and brings each recognized tuner online. You can view and configure all discovered tuners from the **Tuners** panel.

## Connection types

SDRTrunk Kennebec uses a different communication path depending on the hardware family you connect.

**USB (libusb)** — Devices communicate directly over USB using the libusb library. This includes RTL-SDR dongles, Airspy Mini/R2, Airspy HF+, HackRF variants, and HydraSDR. Hot-plug is supported on platforms where libusb provides that capability, so you can plug in a device while the application is running.

**SDRplay API** — SDRplay RSP devices communicate through the proprietary `sdrplay_api` library rather than raw libusb. You must install the SDRplay API separately before SDRTrunk Kennebec can detect any RSP device.

**Sound card** — Funcube Dongle Pro and Pro Plus present audio output to the operating system as a USB sound card. SDRTrunk Kennebec reads IQ samples from the sound card interface rather than a USB bulk transfer endpoint.

> [!NOTE]
>
SDRTrunk Kennebec also includes a **Recording** tuner type that replays previously captured IQ files. Use it for offline decoding and testing without physical hardware.

## Supported tuner types

The table below lists every tuner recognized by the `TunerType` enum, the chip or model identifier embedded in the firmware, the connection method SDRTrunk Kennebec uses, and the exact enum value you will see in configuration files and log output.

| Tuner | Chip / Model | Connection | `TunerType` enum value |
|---|---|---|---|
| Airspy Mini / R2 | R820T | USB | `AIRSPY_R820T` |
| Airspy HF+ | HF+ | USB | `AIRSPY_HF_PLUS` |
| RTL-SDR (Elonics) | E4000 | USB | `ELONICS_E4000` |
| RTL-SDR (Rafael Micro) | R820T | USB | `RAFAELMICRO_R820T` |
| RTL-SDR (Rafael Micro) | R828D | USB | `RAFAELMICRO_R828D` |
| RTL-SDR (Fitipower) | FC0012 | USB | `FITIPOWER_FC0012` |
| RTL-SDR (Fitipower) | FC0013 | USB | `FITIPOWER_FC0013` |
| RTL-SDR (FCI) | FC2580 | USB | `FCI_FC2580` |
| HackRF One | ONE | USB | `HACKRF_ONE` |
| HackRF Jawbreaker | Jawbreaker | USB | `HACKRF_JAWBREAKER` |
| HackRF RAD1O | RAD1O | USB | `HACKRF_RAD1O` |
| HydraSDR | R828D | USB | `HYDRASDR_R828D` |
| Funcube Dongle Pro | — | Sound card | `FUNCUBE_DONGLE_PRO` |
| Funcube Dongle Pro Plus | — | Sound card | `FUNCUBE_DONGLE_PRO_PLUS` |
| SDRplay RSP1 | — | SDRplay API | `RSP_1` |
| SDRplay RSP1A | — | SDRplay API | `RSP_1A` |
| SDRplay RSP1B | — | SDRplay API | `RSP_1B` |
| SDRplay RSP2 | — | SDRplay API | `RSP_2` |
| SDRplay RSPduo (tuner 1) | — | SDRplay API | `RSP_DUO_1` |
| SDRplay RSPduo (tuner 2) | — | SDRplay API | `RSP_DUO_2` |
| SDRplay RSPdx | — | SDRplay API | `RSP_DX` |
| IQ Recording | — | File | `RECORDING` |

> [!NOTE]
>
A tuner type of `UNKNOWN` appears in logs when SDRTrunk Kennebec detects an RTL2832 chip but cannot identify the embedded tuner IC. The device is treated as unusable until a recognized tuner IC is found. Re-plug the dongle or check for a driver conflict if you see this value.

## Tuner status values

Each tuner in the **Tuners** panel displays a status label that reflects its current state. These same values appear in the application log.

| Status | Meaning |
|---|---|
| `Enabled` | The tuner is running and available to receive channel assignments. |
| `Disabled` | You have manually disabled (blacklisted) this tuner. It will not be used until you re-enable it. |
| `Recovering` | The tuner encountered a recoverable USB error and is attempting to restart automatically. |
| `Error` | The tuner failed and could not be recovered. Check the application log for the specific error message. |
| `Removed` | The USB device was physically unplugged from the system. |

## Per-device setup guides

  **RTL-SDR**

    Driver setup, sample rate, gain, and PPM correction for RTL2832-based dongles on Windows, Linux, and macOS.

  **Airspy & HackRF**

    Driver installation and configuration options for Airspy Mini/R2, Airspy HF+, and all HackRF variants.

  **Tuner Self-Healing**

    How SDRTrunk Kennebec automatically recovers locked or failed tuners, including the Windows PowerShell USB reset.
