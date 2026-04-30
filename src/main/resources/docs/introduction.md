# SDRTrunk Kennebec: Multi-Protocol Radio Scanner

> SDRTrunk Kennebec is a cross-platform Java radio scanner for decoding P25, DMR, and analog systems — with live streaming, AI monitoring, and a modernized GUI.

SDRTrunk Kennebec is a cross-platform desktop Java application that turns a compatible Software Defined Radio (SDR) into a multi-channel trunked radio scanner. It monitors, decodes, records, and streams radio traffic from public safety and commercial radio systems in real time. The Kennebec version is a fork of the upstream sdrtrunk project, adding a modernized interface, in-app documentation, streaming-focused workflow improvements, and advanced operational features not found in the original.

> **Note:**
  SDRTrunk Kennebec is currently in **prerelease** pending the completion of unit and system testing. Functionality is stable for most use cases, but you may encounter minor issues.

## What SDRTrunk does

Traditional hardware scanners monitor one frequency at a time. SDRTrunk captures a wide swath of the radio spectrum simultaneously, allowing it to track entire trunked radio systems where conversations dynamically jump across multiple frequencies. The application automatically follows control channels, decodes digital and analog voice traffic, and reassembles conversations in real time.

You can route the resulting audio to external internet streaming platforms, record it locally, and organize it by talkgroup — all from a single application running on your desktop.

## Supported protocols

SDRTrunk Kennebec supports a range of digital and analog radio protocols used by public safety agencies and commercial operators:

* **Project 25 (P25) Phase 1 and Phase 2** — the dominant digital standard for North American public safety
* **DMR (Digital Mobile Radio)** — widely used in commercial and some public safety systems
* **LTR (Logic Trunked Radio)** — an older commercial trunking standard
* **Analog FM** — conventional analog voice channels

> **Tip:**
  Not sure which protocol your local radio system uses? RadioReference.com maintains a database of systems organized by county and state, and SDRTrunk includes integration with the RadioReference API.

## What the Kennebec version adds

The Kennebec version builds on the upstream sdrtrunk codebase with a focused set of improvements for operators who need reliable, unattended streaming and monitoring.

### Modernized interface and workflow

* Refreshed GUI with updated icons and an improved Now Playing view
* Consolidated settings in a single user preference area, eliminating the need to hunt across multiple menus
* New interface for reviewing logs and browsing recorded audio files

### In-app knowledge base

An embedded, searchable help viewer brings documentation directly into the application. You no longer need to switch to a browser to look up configuration details or protocol explanations.

### Streaming and audio reliability

* Automated audio recording, streaming, and metadata tagging
* SDR tuner width auto-calculation to reduce manual configuration
* New stream type for IamResponding (local UDP) using Two Tone Detect
* Tuner self-healing logic that automatically attempts to recover from hardware errors
* Automated tuner reset on Windows 10 and higher using PowerShell scripts for hard-reset of locked or failed SDR devices

### Monitoring and alerts

* Two Tone Detect functionality for paging and dispatch monitoring
* Inactivity monitoring: alerts via Telegram or Email when a channel remains silent for a configurable duration
* Configurable error notifications via Telegram or Email for application or tuner faults

### Optional AI integration

When enabled, Gemini AI can automatically set channel filters, review logs, monitor application performance, assess audio quality, and notify you if a channel becomes unintelligible.

### OS and Java integration

Deep OS integration via modern Java and JNA provides native backdrop effects, theme syncing with the system appearance, and DPI-aware rendering on Windows.

***

## Where to go next

  ### Quick start

    Download or build SDRTrunk Kennebec and get your first channel decoding.


  ### System requirements

    Check OS, hardware, CPU, and RAM requirements before you install.


  ### Hardware setup

    Connect and configure your RTL-SDR, Airspy, HackRF, or SDRPlay tuner.


  ### Radio protocols

    Understand the protocols SDRTrunk decodes and how each one works.


  ### Streaming

    Route live audio to Broadcastify, Zello, Icecast, and other platforms.


  ### Alerts and AI

    Configure Telegram or Email alerts and optional Gemini AI monitoring.