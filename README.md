### Latest Compiled Release: [Download pr-K.00.002](https://github.com/snepple/sdrtrunk_Sam/releases/tag/pr-K.00.002)

<h1>sdrtrunk - Kennebec Version</h1>

<p>Welcome to the Kennebec version of sdrtrunk—a modernized, cross-platform Java application engineered for decoding, monitoring, recording, and streaming trunked mobile and related radio protocols using Software Defined Radios (SDR).</p>

<p>This repository is a fork of <a href="https://github.com/actionpagezello/sdrtrunk">https://github.com/actionpagezello/sdrtrunk</a>, which is itself a fork of the original SDRTrunk application (<a href="https://github.com/DSheirer/sdrtrunk">https://github.com/DSheirer/sdrtrunk</a>). The Kennebec version adds an extensive layer of new capabilities on top of the features introduced by the actionpagezello fork.</p>

<p>This version is explicitly designed for listening to public safety and other radio frequencies and streaming audio to various internet streaming services. It takes the robust decoding engine of sdrtrunk and pairs it with a highly refined, context-aware user experience.</p>

<p>Prerelease Notice: The current version is a prerelease pending the completion of unit and system testing.</p>

<h2>What is SDRTrunk?</h2>
<p>SDRTrunk is a Java-based application that transforms a standard computer and compatible Software Defined Radio hardware, such as RTL-SDR, Airspy, or HackRF, into a powerful, multi-channel radio scanner. Unlike traditional hardware scanners that can only listen to one frequency at a time, SDRTrunk captures a wide swath of the radio spectrum simultaneously.</p>

<p>This wideband capture allows the software to monitor entire trunked radio systems, where conversations dynamically jump across multiple frequencies. SDRTrunk automatically tracks system control channels, decodes the digital or analog voice traffic, and pieces the conversations back together in real time. It supports a variety of common public safety and commercial radio protocols, including Project 25 (P25) Phase 1 and 2, DMR, LTR, and standard analog FM. By utilizing software-based digital signal processing, it provides an accessible and highly configurable way to monitor local radio traffic, manage talkgroups, and route the resulting audio to external internet streaming platforms.</p>

<h2>What the Kennebec Version Adds (Versus the Source Fork)</h2>
<p>The Kennebec version builds on the upstream sdrtrunk codebase with a focused set of improvements for operators who need reliable, unattended streaming and monitoring.</p>

<h3>Modernized Interface and Workflow</h3>
<ul>
  <li>Refreshed GUI with updated icons and an improved Now Playing view</li>
  <li>Consolidated settings in a single user preference area, eliminating the need to hunt across multiple menus</li>
  <li>New interface for reviewing logs and browsing recorded audio files</li>
  <li>Ability to set allocated memory directly via the user preferences Ux/GUI</li>
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

<h2>Documentation</h2>
<p>For more detailed information, setup guides, and tutorials, please visit the <a href="https://sam-64221fcd.mintlify.app/">SDRTrunk Kennebec Documentation</a>.</p>

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
