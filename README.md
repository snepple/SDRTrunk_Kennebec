## 📥 Download the Latest Release

Not sure what to download? Here is a quick guide to getting started with **SDRTrunk Kennebec (vK.00.083)**:

- **Windows Users (Recommended)**: Download the Native Windows Installer. This is the easiest way to install and manage SDRTrunk on Windows.
  - [Download Windows Installer (.exe)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/K.00.083/SDRTrunk-K.00.083-windows-installer.exe)
  
- **Advanced Windows Users**: Download the Portable ZIP if you prefer to run the application without installing it.
  - [Download Windows Portable ZIP (.zip)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/K.00.083/SDRTrunk-K.00.083-windows-x86_64.zip)

- **Mac Users**:
  - [Download macOS Portable ZIP (.zip)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/K.00.083/SDRTrunk-K.00.083-mac-x86_64.zip)

- **Linux Users**:
  - [Download Linux Portable ZIP (.zip)](https://github.com/snepple/SDRTrunk_Kennebec/releases/download/K.00.083/SDRTrunk-K.00.083-linux-x86_64.zip)

*(View all release assets and notes on the [Releases Page](https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/K.00.083))*

---


<h1>sdrtrunk - Kennebec Version</h1>



<p>Welcome to <strong>SDRTrunk Kennebec</strong>—a highly customized, feature-rich fork of the original SDRTrunk application. Engineered for robust decoding, monitoring, recording, and streaming of trunked mobile and related radio protocols using Software Defined Radios (SDR).</p>

<p>While the original SDRTrunk provides an excellent decoding engine, the Kennebec build is explicitly designed for the modern public safety listening environment. It layers an extensive suite of new capabilities on top of the original engine to provide a highly refined, context-aware, and automated user experience.</p>

<h2>🌟 Kennebec Highlights</h2>
<p>Kennebec introduces features not found in any other SDR software:</p>
<ul>
  <li><strong>AI-Powered Transcriptions & Optimization:</strong> Real-time radio audio transcription (via OpenAI Whisper / Google Speech-to-Text) and NBFM AI audio noise filtering.</li>
  <li><strong>Advanced Integrations:</strong> Real-time UDP streaming specifically for the IAmResponding platform, plus enhanced OpenMHz and Broadcastify integration.</li>
  <li><strong>Modern UX/UI Redesign:</strong> A beautiful Apple-style aesthetic with a custom collapsible left-hand sidebar, high-DPI SVG icons, and inline table editing.</li>
  <li><strong>Resilience & Health:</strong> Automated tuner self-healing logic, Smart Bandwidth (auto-optimizing sample rate to save CPU), and built-in System Health notifications.</li>
  <li><strong>In-App Documentation:</strong> An embedded, fully searchable knowledge base right inside the application.</li>
</ul>

<h2>What is SDRTrunk?</h2>
<p>SDRTrunk is a Java-based application that transforms a standard computer and compatible Software Defined Radio hardware, such as RTL-SDR, Airspy, or HackRF, into a powerful, multi-channel radio scanner. Unlike traditional hardware scanners that can only listen to one frequency at a time, SDRTrunk captures a wide swath of the radio spectrum simultaneously.</p>

<p>This wideband capture allows the software to monitor entire trunked radio systems, where conversations dynamically jump across multiple frequencies. SDRTrunk automatically tracks system control channels, decodes the digital or analog voice traffic, and pieces the conversations back together in real time. It supports a variety of common public safety and commercial radio protocols, including Project 25 (P25) Phase 1 and 2, DMR, LTR, and standard analog FM. By utilizing software-based digital signal processing, it provides an accessible and highly configurable way to monitor local radio traffic, manage talkgroups, and route the resulting audio to external internet streaming platforms.</p>


<h2>How does trunking radios work?</h2>
<p>For those not familiar, trunking systems allow a large number of user groups to share a limited number of radio frequencies by temporarily, dynamically assigning radio frequencies to talkgroups (channels) on-demand. It is understood that most user groups actually use the radio very sporadically and don't need a dedicated frequency.</p>

<p>Most trunking system types (such as SmartNet and P25) set aside one of the radio frequencies as a "control channel" that manages and broadcasts radio frequency assignments. When someone presses the Push to Talk button on their radio, the radio sends a message to the system which then assigns a voice frequency and broadcasts a Channel Grant message about it on the control channel. This lets the radio know what frequency to transmit on and tells other radios set to the same talkgroup to listen.</p>

<p>In order to follow all of the transmissions, SDRTrunk Kennebec constantly listens to and decodes the control channel. When a frequency is granted to a talkgroup, SDRTrunk Kennebec creates a monitoring process which decodes the portion of the radio spectrum for that frequency from the SDR that is already pulling it in.</p>

<p>No message is transmitted on the control channel when a conversation on a talkgroup is over. The monitoring process keeps track of transmissions and if there has been no activity for a specified period, it ends the recording.</p>
<h2>What the Kennebec Version Adds (Versus the Source Fork)</h2>
<p>The Kennebec version builds on the upstream <a href="https://github.com/actionpagezello/sdrtrunk">sdrtrunk</a> codebase with a massive set of improvements for operators who need reliable, unattended streaming and monitoring. Below is an in-depth comparison of features explicitly added or significantly overhauled in the Kennebec fork:</p>

<h3>Performance & Resource Optimization</h3>
<ul>
  <li><strong>Reduced CPU & Memory Utilization:</strong> Major underlying engine optimizations aimed at reducing heap allocations, aggressive memory management, and smarter thread pooling.</li>
  <li><strong>GPU Acceleration for Spectral Visuals:</strong> Waterfall and Spectrum displays now offload heavy rendering tasks to the GPU, producing much smoother frame rates while drastically reducing CPU tax.</li>
  <li><strong>Smart Bandwidth:</strong> Automatically calculates and reduces SDR tuner sample rates when observing narrower bandwidths, further conserving CPU.</li>
</ul>

<h3>Modernized Interface and Workflow</h3>
<ul>
  <li><strong>Unified User Preferences:</strong> We completely eliminated the need to hunt across multiple menus. <em>All</em> application settings, memory allocation configurations, and tuner preferences are now consolidated into a single, intuitive "User Preferences" area.</li>
  <li><strong>New GUI for Logs & Recordings:</strong> Replaced the old file-system approach with a beautiful, in-app GUI for reviewing live application/tuner logs and browsing, filtering, and playing back recorded audio files natively.</li>
  <li><strong>Refreshed Apple-Style Aesthetic:</strong> Updated icons, a collapsible left-hand navigation sidebar, and a highly polished "Now Playing" view for better visual ergonomics.</li>
  <li><strong>Windows Native Installer & Friendly <code>.exe</code>:</strong> Added a seamless Windows native installer. No more launching via ugly batch scripts—SDRTrunk Kennebec installs cleanly to your Program Files and launches via a silent, Windows-friendly native <code>.exe</code> wrapper (no background terminal window!).</li>
</ul>

<h3>Advanced Monitoring & Integrations</h3>
<ul>
  <li><strong>Two-Tone Detection:</strong> Brand new, fully integrated Two-Tone paging detection for monitoring and dispatching fire/EMS alerts.</li>
  <li><strong>IAmResponding Streaming:</strong> A custom-built, native UDP streaming integration designed specifically to route audio dispatches directly into the IAmResponding platform.</li>
  <li><strong>Inactivity & Error Alerts:</strong> Receive alerts via Telegram or Email when a channel remains silent for a configurable duration or when the application/tuner faults.</li>
</ul>

<h3>Artificial Intelligence Integration</h3>
<ul>
  <li><strong>AI-Powered Audio Transcriptions:</strong> Real-time radio audio transcription capabilities via OpenAI Whisper and Google Speech-to-Text.</li>
  <li><strong>NBFM AI Audio Optimization:</strong> AI-driven noise filtering specifically designed to clean up analog NBFM signals.</li>
  <li><strong>Intelligent System Health Advisor:</strong> Optional AI assistant that can automatically review system logs, monitor application performance, and provide auto-remediation advice if a channel becomes unintelligible or a tuner locks up.</li>
</ul>

<h3>In-App Knowledge Base</h3>
<ul>
  <li>An embedded, searchable help viewer brings all documentation directly into the application. You no longer need to switch to a browser to look up configuration details or DSP explanations!</li>
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

<h2>📸 Updated Screenshots</h2>

<p align="center">
Refreshed Apple-style GUI & Side Navigation<br>
<img src="images/now_playing_k78.png" alt="SDRTrunk Kennebec - Updated GUI" width="800">
<br><br>

Streamlined Aliases & Inline Editing<br>
<img src="images/playlist_editor_k78.png" alt="SDRTrunk Kennebec - Aliases" width="800">
<br><br>

Enhanced Two Tone Detect Configuration<br>
<img src="images/two_tones_k78.png" alt="SDRTrunk Kennebec - Two Tones" width="800">
<br><br>

Streaming Integrations (IAmResponding & more)<br>
<img src="images/streaming_k78.png" alt="SDRTrunk Kennebec - Integrations" width="800">
<br><br>

AI Monitoring & Setup<br>
<img src="images/ai_preferences_k78.png" alt="SDRTrunk Kennebec - AI Setup" width="800">
<br><br>

In-App Knowledge Base & Help Viewer<br>
<img src="images/help_docs_k78.png" alt="SDRTrunk Kennebec - Help and Docs" width="800">
<br><br>

Audio Recordings Review<br>
<img src="images/audio_recordings_k78.png" alt="SDRTrunk Kennebec - Audio Recordings" width="800">
<br><br>

Performance & Live Log Analysis<br>
<img src="images/performance_logs_k78.png" alt="SDRTrunk Kennebec - Performance and Logs" width="800">
</p>

<h2>Historical Screenshots</h2>

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
