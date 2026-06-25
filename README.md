# SDRTrunk — Kennebec Version

**SDRTrunk Kennebec** is a feature-rich fork of [SDRTrunk](https://github.com/DSheirer/sdrtrunk) that turns a standard computer and a compatible Software Defined Radio (SDR) into a powerful, multi-channel trunked and conventional radio scanner — with a modern interface and a large suite of automation, streaming, and reliability features layered on top for unattended, public-safety listening.

> **Lineage at a glance:** [DSheirer/sdrtrunk](https://github.com/DSheirer/sdrtrunk) (original) → [actionpagezello/sdrtrunk](https://github.com/actionpagezello/sdrtrunk) (upstream fork) → **SDRTrunk Kennebec** (this project). The core decoding, DSP, and trunking engine is the work of those upstream projects — see [Credits, Lineage & License](#credits-lineage--license).

---

## What is SDRTrunk?

SDRTrunk is a Java application that transforms a standard computer and compatible SDR hardware (such as RTL-SDR, Airspy, HackRF, or SDRplay/RSP) into a multi-channel radio scanner. Unlike a traditional hardware scanner that listens to a single frequency at a time, SDRTrunk captures a wide swath of the radio spectrum at once.

That wideband capture lets the software monitor entire trunked radio systems, where conversations dynamically hop across many frequencies. SDRTrunk tracks system control channels, decodes the digital or analog voice traffic, and reassembles conversations in real time. By doing the signal processing in software, it provides an accessible and highly configurable way to monitor local radio traffic, label talkgroups, record calls, and route audio to internet streaming platforms.

**Supported decoders** (provided by the upstream SDRTrunk engine): P25 Phase 1 & Phase 2, DMR (including Tier III), LTR, LTR-Net, MPT-1327, Passport, conventional NBFM and AM, plus auxiliary decoders for MDC-1200, Fleetsync II, and Tait 1200.

### How does trunked radio work?

Trunking systems let a large number of user groups share a limited number of radio frequencies by dynamically assigning a frequency to a talkgroup only for the duration of a transmission. Most trunked systems (such as P25 and Motorola SmartNet) dedicate one frequency as a **control channel** that broadcasts these frequency assignments. When someone keys up, the system grants a voice frequency and announces it on the control channel so the right radios know where to listen.

To follow the traffic, SDRTrunk continuously decodes the control channel. When a frequency is granted to a talkgroup, it spins up a monitoring process that decodes that portion of the spectrum from the SDR data it is already receiving. When activity stops for a configured period, the call ends.

---

## What the Kennebec Version Adds

The Kennebec build is aimed at operators who need reliable, unattended streaming and monitoring. It builds on the upstream [actionpagezello/sdrtrunk](https://github.com/actionpagezello/sdrtrunk) fork and adds (or significantly overhauls) the following:

### Modernized interface & workflow
- **Unified User Preferences** — application settings, memory allocation, and tuner preferences consolidated into a single area instead of being spread across multiple menus.
- **In-app Logs & Recordings** — browse, filter, and play back recordings and review live application/tuner logs from a GUI instead of digging through the file system.
- **Refreshed UI** — updated icons, a collapsible left-hand navigation sidebar, and a polished "Now Playing" view.
- **Windows native installer** — a standard `.exe` installer plus a silent, Windows-friendly launcher (no background terminal window).

### Advanced monitoring & integrations
- **Two-Tone paging detection** — integrated detection of two-tone (and long-tone) pages for fire/EMS dispatch alerting, with per-detector alerting to one or more Zello channels, local audio, visual notifications, and MQTT.
- **IAmResponding streaming** — native UDP streaming designed to route audio dispatches into the IAmResponding platform.
- **Streaming integrations** — works with Broadcastify (feeds & Calls), OpenMHz, RdioScanner, Icecast, and Zello, alongside the existing SDRTrunk streaming options.
- **Inactivity & error alerts** — optional Telegram or email alerts when a channel goes silent for too long or when the application/a tuner faults.

### Reliability & health
- **Tuner self-healing** — automatic recovery from transient USB errors instead of dropping all channels on a momentary glitch.
- **Smart Bandwidth** — automatically lowers SDR sample rates when narrower bandwidth is sufficient, conserving CPU.
- **System Health monitoring** — startup self-tests, playlist linting that surfaces configuration problems, and in-app health notifications.

### Performance
- **Reduced CPU & memory utilization** — engine-level work to reduce heap allocations and improve thread usage.
- **Optional OpenCL GPU acceleration (Aparapi)** — offloads some DSP/visualization math to a compatible GPU when available.

### Artificial intelligence (optional)
- **Audio transcription** — real-time transcription of radio audio via OpenAI Whisper or Google Speech-to-Text.
- **NBFM audio optimization** — AI-assisted noise reduction for analog NBFM audio.
- **System Health Advisor** — an optional assistant that can review logs and offer remediation guidance.

### In-app knowledge base
- An embedded, searchable help viewer brings the documentation directly into the application.

> See [`CHANGELOG.md`](CHANGELOG.md) for the detailed, version-by-version history.

---

## Core SDRTrunk features

- Comprehensive digital and analog trunking support (P25 Phase 1 & 2, DMR, LTR, and more)
- Multi-channel tracking from a single wideband capture
- Automated control-channel following and voice-traffic decoding
- Talkgroup/alias management and audio routing to streaming platforms
- Call recording and event logging

---

## Documentation & guides

**[Read the SDRTrunk Kennebec documentation](https://sam-64221fcd.mintlify.app/)** for setup guides, tutorials, and troubleshooting — from initial hardware setup to advanced streaming and AI integration. The same content is also available in the in-app knowledge base.

---

## Building & running from source

SDRTrunk Kennebec is built with Gradle and requires **Java 23+** (provisioned automatically via Gradle toolchains).

```bash
# Run the application directly
./gradlew run

# Create a native OS installer/app-image (uses the JDK's jpackage)
./gradlew createInstaller -x compileJni
```

The installer/runtime image is written to `build/installer/` and `build/image/`.

### Producing and publishing releases

`build_and_release.bat` (Windows) is the release tool. It compiles the project, builds the platform packages (Windows installer + portable ZIP, and — for a full release — Linux and macOS packages), and publishes a GitHub Release with those assets.

After publishing, it **automatically regenerates the "Download the Latest Release" section of this README** (the block delimited by the `DOWNLOADS:START` and `DOWNLOADS:END` HTML comment markers) so the links always point at the newest release, then commits and pushes the change. That regeneration is performed by [`.github/update_readme_downloads.ps1`](.github/update_readme_downloads.ps1), which links only the assets that actually exist in the release.

> **Do not edit the download-links section by hand** — it is overwritten on every release.

---

## Updated screenshots

<p align="center">
Refreshed GUI &amp; side navigation<br>
<img src="images/now_playing_k78.png" alt="SDRTrunk Kennebec - Updated GUI" width="800">
<br><br>

Streamlined aliases &amp; inline editing<br>
<img src="images/playlist_editor_k78.png" alt="SDRTrunk Kennebec - Aliases" width="800">
<br><br>

Two-Tone detect configuration<br>
<img src="images/two_tones_k78.png" alt="SDRTrunk Kennebec - Two Tones" width="800">
<br><br>

Streaming integrations (IAmResponding &amp; more)<br>
<img src="images/streaming_k78.png" alt="SDRTrunk Kennebec - Integrations" width="800">
<br><br>

AI monitoring &amp; setup<br>
<img src="images/ai_preferences_k78.png" alt="SDRTrunk Kennebec - AI Setup" width="800">
<br><br>

In-app knowledge base &amp; help viewer<br>
<img src="images/help_docs_k78.png" alt="SDRTrunk Kennebec - Help and Docs" width="800">
<br><br>

Audio recordings review<br>
<img src="images/audio_recordings_k78.png" alt="SDRTrunk Kennebec - Audio Recordings" width="800">
<br><br>

Performance &amp; live log analysis<br>
<img src="images/performance_logs_k78.png" alt="SDRTrunk Kennebec - Performance and Logs" width="800">
</p>

---

## Minimum system requirements

- **Operating system:** Windows (64-bit), Linux (64-bit), or macOS (64-bit, 12.x or higher)
- **CPU:** 4 cores
- **RAM:** 8 GB or more recommended (4 GB may suffice depending on usage)
- **Java:** Java 23+ (automatically provisioned via Gradle toolchains when building from source; bundled in the release packages)

---

## Credits, Lineage & License

SDRTrunk Kennebec stands on the work of two upstream projects and would not exist without them:

- **Original — SDRTrunk by Dennis Sheirer:** [DSheirer/sdrtrunk](https://github.com/DSheirer/sdrtrunk). The decoding, DSP, and trunking engine at the heart of this application is Dennis Sheirer's work. Huge thanks to him and the SDRTrunk contributors.
- **Upstream fork — [actionpagezello/sdrtrunk](https://github.com/actionpagezello/sdrtrunk):** SDRTrunk Kennebec is forked directly from this project and builds on its improvements. Thank you to its author(s) for the work that Kennebec extends.

Kennebec is a community/hobby fork and is not affiliated with or endorsed by the upstream projects.

This project is licensed under the **GNU General Public License v3.0**, the same license as the original SDRTrunk — see [`LICENSE`](LICENSE). Per the GPL, source is available here and any redistributed builds remain under the GPL.

---

<!-- DOWNLOADS:START -->
<!-- This section is regenerated automatically by build_and_release.bat after each GitHub release. Do not edit by hand. -->
## Download the Latest Release

Not sure what to download? Here is a quick guide to getting started with **SDRTrunk Kennebec (v00.143)**:

- **Windows Users (Recommended)**: Download the Native Windows Installer. This is the easiest way to install and manage SDRTrunk on Windows.
  - [Download Windows Installer (.exe)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/00.143/SDRTrunk-00.143-windows-installer.exe)
  - **Note**: Because this is an open-source project without a paid code signing certificate, Windows SmartScreen may show an "Unknown Publisher" warning. To proceed, click **"More info"**, then click **"Run anyway"**.
- **Advanced Windows Users**: Download the Portable ZIP if you prefer to run the application without installing it.
  - [Download Windows Portable ZIP (.zip)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/00.143/SDRTrunk-00.143-windows-x86_64.zip)

*(View all release assets and notes on the [Releases Page](https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/00.143))*
<!-- DOWNLOADS:END -->
