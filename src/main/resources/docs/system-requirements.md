# System requirements for SDRTrunk Kennebec

> Minimum and recommended hardware, operating system, Java version, and SDR device requirements to run SDRTrunk Kennebec on Windows, Linux, or macOS.

SDRTrunk Kennebec is a cross-platform Java application that runs on Windows, Linux, and macOS. Before installing, confirm that your system meets the requirements below. Underpowered hardware can lead to dropped audio, decoding failures, or application instability — particularly when monitoring multiple simultaneous channels.

## Operating system

SDRTrunk Kennebec supports 64-bit operating systems only. 32-bit systems are not supported.

| Platform | Supported versions                        |
| -------- | ----------------------------------------- |
| Windows  | 64-bit (any modern version)               |
| Linux    | 64-bit                                    |
| macOS    | 64-bit, version 12.x (Monterey) or higher |

> **Info:**
  Automated tuner reset via PowerShell — which hard-resets a locked or failed SDR device — is available on Windows 10 and higher only. This feature is not available on Linux or macOS.

> **Warning:**
  ARM-based systems (aarch64) are supported for Linux and macOS (Apple Silicon). Windows ARM support is available in build tooling but is not the primary tested configuration. Check the release notes for your specific version.

## CPU

| Requirement   | Value            |
| ------------- | ---------------- |
| Minimum cores | 4-core processor |

SDRTrunk performs software-based digital signal processing in real time. A 4-core CPU is the minimum for stable operation. More cores improve performance when monitoring multiple simultaneous channels or running with AI monitoring enabled.

> **Tip:**
  The application uses the JDK 23 Vector API (Project Panama) for accelerated DSP operations, which benefits from modern CPU microarchitectures. A processor released within the last five years will generally perform better than the minimum spec suggests.

## Memory (RAM)

| Configuration       | RAM          |
| ------------------- | ------------ |
| Preferred           | 8 GB or more |
| Minimum (light use) | 4 GB         |

8 GB is the recommended baseline. 4 GB may be sufficient for simple configurations with a small number of channels, but heavy use — multiple simultaneous trunking systems, streaming, and AI monitoring — benefits from more available memory.

The application defaults to a 6 GB JVM heap (`-Xmx6g`). You can adjust the allocated memory limit directly from the user preferences interface without editing configuration files.

## Java

| Requirement     | Value                                                       |
| --------------- | ----------------------------------------------------------- |
| Minimum version | Java 23                                                     |
| Provisioning    | Automatic via Gradle Toolchains (when building from source) |

Prebuilt release packages include a bundled JDK — you do not need to install Java separately. When building from source with `./gradlew`, Gradle Toolchains automatically downloads and provisions Java 23 if it is not already present.

If you are running the application outside of a bundled release or Gradle environment, install a compatible JDK 23+ distribution. The build system uses Azul Zulu JDK 23; Bellsoft Liberica JDK is used for cross-platform release packaging.

> **Note:**
  The application uses preview and incubator features from JDK 23 (specifically the Vector API from Project Panama). These are enabled automatically by the bundled launcher scripts. You do not need to configure JVM flags manually when using a release package.

## SDR hardware

SDRTrunk Kennebec requires at least one compatible Software Defined Radio device. No SDR hardware is included — you must supply your own.

Supported hardware families include:

* **RTL-SDR** — the most common and lowest-cost option; widely available USB dongles based on the RTL2832U chip
* **Airspy** — higher dynamic range receivers for demanding environments
* **HackRF** — wide-band transceiver suitable for broad spectrum capture
* **SDRPlay** — requires the SDRPlay API to be installed separately

> **Info:**
  On Windows, most RTL-SDR dongles require the WinUSB driver to be installed via [Zadig](https://zadig.akeo.ie/) before SDRTrunk can access them. See the [RTL-SDR setup guide](/hardware/rtl-sdr) for step-by-step instructions.

A single SDR device can monitor one contiguous band of spectrum at a time. For monitoring multiple separate frequency ranges simultaneously, you need multiple SDR devices. SDRTrunk supports multiple tuners running concurrently.

## Summary table

| Component     | Minimum                        | Preferred                      |
| ------------- | ------------------------------ | ------------------------------ |
| OS            | Windows / Linux / macOS 64-bit | Windows / Linux / macOS 64-bit |
| macOS version | 12.x (Monterey)                | Latest supported               |
| CPU           | 4-core                         | 8-core or more                 |
| RAM           | 4 GB                           | 8 GB or more                   |
| Java          | 23+ (auto-provisioned)         | Bundled in release package     |
| SDR hardware  | 1 compatible device            | 1 device per frequency band    |

## Next steps

  ### Quick start

    Install SDRTrunk Kennebec and get your first channel decoding.


  ### Hardware setup

    Connect and configure your RTL-SDR, Airspy, HackRF, or SDRPlay device.