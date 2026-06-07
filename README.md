### Latest Compiled Release: [Download K.00.075](https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/K.00.075)
### Latest Compiled Release: [Download K.00.075](https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/K.00.075)

<h1>sdrtrunk - Kennebec Version</h1>

## 🆕 Recent Updates (Week of May 10, 2026)
* **🚀 New Features:**
  * **Audio Transcriptions:** Support for transcribing radio audio using OpenAI Whisper and Google Speech-to-Text APIs.
  * **NBFM AI Audio Optimizer:** AI-driven noise filtering for analog channels.
  * **IAmResponding:** Real-time UDP streaming feature specifically for the IAmResponding platform.
  * **Smart Bandwidth:** Auto-optimizing sample rate to save CPU.
  * **Native Sequential Paging:** Phase 4 Native Sequential Paging Detection.
  * **Contextual Help Viewer:** An embedded, searchable documentation viewer right inside the app.
  * **System Health:** Implemented a new System Health Notification feature to alert you of application issues.
  * **Geographic IDs:** Added a Geographic ID generator for NBFM channels.
  * **JMBE Support:** Added support for building the bazineta JMBE fork.
* **🎨 UI/UX Improvements:**
  * **macOS Aesthetic Redesign:** A major macOS System Settings-style UI redesign, including a custom collapsible left-hand sidebar and high-dpi SVG icons.
  * **Channel Table Enhancements:** Enabled inline editing and added a right-click context menu to the channel table rows.
  * **Alias & Streaming:** Added inline editing to the alias table, separated listen and priority columns in the Alias Configuration Editor, and added context menu filtering to the streaming widget.
  * **Live Console:** Added multiple selection and clipboard copy support to Live Console logs.
  * **Notifications:** Redesigned the Notifications configuration and added alerts.
* **🛡️ Security & Stability:**
  * Fixed a GUI threading issue ensuring main GUI initialization and Swing UI component updates are correctly handled on the Event Dispatch Thread (EDT).
  * Resolved a deadlock and buffer leak in the NativeBufferManager affecting the waterfall display.
  * Fixed a resource leak in the AudioSegmentRecorder.
  * Prevented application crashes on startup caused by MigLayout constraints.
* **🔧 Bug Fixes:**
  * Fixed an issue where the tuner enable button failed silently due to a bandwidth bug.
  * Resolved UI bugs causing empty spectrum and waterfall displays.
* **📚 Documentation:**
  * Added new guides for Radio Reference import, OpenMHz streaming, Rdio Scanner streaming, and ThinLine Radio documentation.


<p>Welcome to the Kennebec version of sdrtrunk—a modernized, cross-platform Java application engineered for decoding, monitoring, recording, and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).</p>

<p>This repository is a fork of <a href="https://github.com/actionpagezello/sdrtrunk">https://github.com/actionpagezello/sdrtrunk</a>, which is itself a fork of the original SDRTrunk application (<a href="https://github.com/DSheirer/sdrtrunk">https://github.com/DSheirer/sdrtrunk</a>). The Kennebec version adds an extensive layer of new capabilities on top of the features introduced by the actionpagezello fork.</p>

<p>This version is explicitly designed for listening to public safety and other radio frequencies and streaming audio to various internet streaming services. It takes the robust decoding engine of sdrtrunk and pairs it with a highly refined, context-aware user experience.</p>

<p>Prerelease Notice: The current version is a prerelease pending the completion of unit and system testing.</p>

<h2>What is SDRTrunk?</h2>
<p>SDRTrunk is a Java-based application that transforms a standard computer and compatible Software Defined Radio hardware, such as RTL-SDR, Airspy, or HackRF, into a powerful, multi-channel radio scanner. Unlike traditional hardware scanners that can only listen to one frequency at a time, SDRTrunk captures a wide swath of the radio spectrum simultaneously.</p>

<p>This wideband capture allows the software to monitor entire trunked radio systems, where conversations dynamically jump across multiple frequencies. SDRTrunk automatically tracks system control channels, decodes the digital or analog voice traffic, and pieces the conversations back together in real time. It supports a variety of common public safety and commercial radio protocols, including Project 25 (P25) Phase 1 and 2, DMR, LTR, and standard analog FM. By utilizing software-based digital signal processing, it provides an accessible and highly configurable way to monitor local radio traffic, manage talkgroups, and route the resulting audio to external internet streaming platforms.</p>


<h2>How does trunking radios work?</h2>
<p>For those not familiar, trunking systems allow a large number of user groups to share a limited number of radio frequencies by temporarily, dynamically assigning radio frequencies to talkgroups (channels) on-demand. It is understood that most user groups actually use the radio very sporadically and don't need a dedicated frequency.</p>

<p>Most trunking system types (such as SmartNet and P25) set aside one of the radio frequencies as a "control channel" that manages and broadcasts radio frequency assignments. When someone presses the Push to Talk button on their radio, the radio sends a message to the system which then assigns a voice frequency and broadcasts a Channel Grant message about it on the control channel. This lets the radio know what frequency to transmit on and tells other radios set to the same talkgroup to listen.</p>

<p>In order to follow all of the transmissions, SDRTrunk Kennebec constantly listens to and decodes the control channel. When a frequency is granted to a talkgroup, SDRTrunk Kennebec creates a monitoring process which decodes the portion of the radio spectrum for that frequency from the SDR that is already pulling it in.</p>

<p>No message is transmitted on the control channel when a conversation on a talkgroup is over. The monitoring process keeps track of transmissions and if there has been no activity for a specified period, it ends the recording.</p>
<h2>What the Kennebec Version Adds (Versus the Source Fork)</h2>
<p>The Kennebec version builds on the upstream sdrtrunk codebase with a focused set of improvements for operators who need reliable, unattended streaming and monitoring.</p>

<h3>Modernized Interface and Workflow</h3>
<ul>
  <li>Refreshed GUI with updated icons and an improved Now Playing view</li>
  <li>Consolidated settings in a single user preference area, eliminating the need to hunt across multiple menus</li>
  <li>New interface for reviewing logs and browsing recorded audio files</li>
  <li>Ability to set allocated memory directly via the user preferences Ux/GUI</li>
  <li>AI Integration for Audio Monitoring and System Health Notifications</li>
  <li>Automated Geographic ID generation for NBFM channels</li>
</ul>

<h3>In-App Knowledge Base</h3>
<ul>
  <li>An embedded, searchable help viewer brings documentation directly into the application. You no longer need to switch to a browser to look up configuration details or protocol explanations.</li>
  <li>Contextual DSP explanations and interactive configuration.</li>
</ul>

<h3>Streaming and Audio Reliability</h3>
<ul>
  <li>Automated audio recording, streaming, and metadata tagging</li>
  <li>SDR tuner width auto-calculation to reduce manual configuration</li>
  <li>New stream type for IamResponding (local UDP) using Two Tone Detect</li>
  <li>Tuner self-healing logic that automatically attempts to recover from hardware errors</li>
  <li>Automated tuner reset on Windows 10 and higher using PowerShell scripts for hard-reset of locked or failed SDR devices</li>
</ul>

<h3>Monitoring and Alerts</h3>
<ul>
  <li>Two Tone Detect functionality for paging and dispatch monitoring</li>
  <li>Inactivity monitoring: alerts via Telegram or Email when a channel remains silent for a configurable duration</li>
  <li>Configurable error notifications via Telegram or Email for application or tuner faults</li>
</ul>

<h3>Optional AI Integration</h3>
<ul>
  <li>When enabled, Gemini AI can automatically set channel filters, review logs, monitor application performance, assess audio quality, and notify you if a channel becomes unintelligible.</li>
</ul>

<h3>OS and Java Integration</h3>
<ul>
  <li>Deep OS integration via modern Java and JNA provides native backdrop effects, theme syncing with the system appearance, and DPI-aware rendering on Windows.</li>
</ul>

<h2>Core SDRTrunk Features</h2>
<ul>
  <li>Comprehensive digital and analog trunking support (P25 Phase 1 & 2, DMR, LTR, etc.)</li>
  <li>Multi-channel tracking from a wideband capture</li>
  <li>Automated control channel following and voice traffic decoding</li>
  <li>Manage talkgroups and route audio to various streaming platforms</li>
</ul>

<h2>Installing via Native Installer</h2>
<p>If you prefer an easy-to-use application installer (such as a <code>.exe</code> installer for Windows) instead of extracting a ZIP file and running the <code>sdr-trunk.bat</code> script, you can easily generate one! The build system natively supports the JDK's <code>jpackage</code> utility.</p>
<p>Simply run the following command from the source code root directory (using the Gradle wrapper):</p>
<pre><code>./gradlew createInstaller -x compileJni</code></pre>
<p>This will bundle the compiled Java application, dependencies, and native Java runtime into a standard OS installer package in the <code>build/installer/</code> directory.</p>

<h2>📚 Documentation & Guides</h2>
<p><strong><a href="https://sam-64221fcd.mintlify.app/">Read the official SDRTrunk Kennebec Documentation</a></strong></p>
<p>For detailed information, setup guides, tutorials, and troubleshooting, please visit our comprehensive documentation portal. The portal covers everything from initial hardware setup to advanced streaming and AI integration.</p>

<h2>Screenshots</h2>

<p align="center">
Refreshed GUI (Now Playing)<br>
<img src="images/nowplayingscreenshot.png" alt="SDRTrunk Kennebec - Now Playing" width="800">
<br><br>

In-App Knowledge Base &amp; Help Viewer<br>
<img src="images/HelpScreenshot.png" alt="SDRTrunk Kennebec - Help and Docs" width="800">
<br><br>

Two Tone Detect Functionality<br>
<img src="images/TwoToneDetectScreenShot.png" alt="SDRTrunk Kennebec - Two Tone Detect" width="800">
<br><br>

Audio Recordings Review<br>
<img src="images/AudioRecordingScreenshot.png" alt="SDRTrunk Kennebec - Audio Recordings" width="800">
<br><br>

Consolidated User Preferences<br>
<img src="images/UserPreferencesScreenshot.png" alt="SDRTrunk Kennebec - User Preferences" width="800">
</p>

<h2>Minimum System Requirements</h2>
<ul>
  <li>Operating System: Windows (64-bit), Linux (64-bit) or Mac (64-bit, 12.x or higher)</li>
  <li>CPU: 4-core</li>
  <li>RAM: 8GB or more (preferred). Depending on usage, 4GB may be sufficient.</li>
  <li>Java: Requires Java 23+ (automatically provisioned via Gradle Toolchains).</li>
</ul>
