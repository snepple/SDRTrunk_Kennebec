# Changelog

All notable changes in the SDRTrunk Kennebec fork are documented here. Upstream
DSheirer/sdrtrunk changes are not repeated; only fork deltas are recorded.

Kennebec releases are versioned `K.XX.YYY`. Earlier `0.6.2-ap-<n>` entries refer
to the upstream `actionpagezello/sdrtrunk` fork that Kennebec is based on.

## [K.00.041] - 2026-05-08

This week's release focuses on the JavaFX modernization push, new monitoring and
notification features, and a long list of UI polish and reliability fixes across
the channels, alias, and streaming editors.

### Added
- **AI Audio Monitoring for channels.** Continuously evaluates channel audio with
  Gemini and surfaces results inline in the channel table.
  See [docs/gemini-ai](src/main/resources/docs/gemini-ai.md).
- **System Health Notifications.** Operators can now be alerted when the
  application detects degraded health (tuners, streams, audio output).
- **AI Optimize Audio Filters** button on NBFM channels. One-click suggestions
  for high/low-pass and squelch tail tuned to the current channel audio.
- **Telegram and SMTP notification routing** with a redesigned per-recipient
  Notifications editor and alert support.
  See [docs/notifications](src/main/resources/docs/notifications.md).
- **Geographic ID generator for NBFM channels.** Generate talkgroup IDs based on
  county/region selections.
- **Two Tone detection alerts and streaming icons** in the audio channel panel,
  so active two-tone events are visible at a glance.
  See [docs/two-tone-detect](src/main/resources/docs/two-tone-detect.md).
- **Right-click context menu and inline editing** in the Channels table — rename,
  toggle auto-start, set order, and access common actions without opening the editor.
- **Inline editing in the Alias table** for faster bulk edits, plus separate
  Listen and Priority columns in the Alias Configuration editor.
- **Import from Radio Reference** added to the Channel Editor "New" dropdown.
  See [docs/radio-reference](src/main/resources/docs/radio-reference.md).
- **OpenMHz, Rdio Scanner, Virtual Audio Cable, and ThinLine Radio guides**
  added to the in-app Help Viewer.
  See [docs/openmhz](src/main/resources/docs/openmhz.md),
  [docs/rdio-scanner](src/main/resources/docs/rdio-scanner.md),
  [docs/virtual-audio-cable](src/main/resources/docs/virtual-audio-cable.md), and
  [docs/thinline-radio](src/main/resources/docs/thinline-radio.md).
- **Help Viewer** is now organized by category and includes full-text search.
- **Tooltips and contextual help** added throughout the P25 Phase 1, P25 Phase 2,
  DMR, NBFM, and SDR tuner editors, including WACN helpers and "valid range"
  guidance for talkgroup IDs.
- **Empty-state messages** added to alias, streaming, and tuner tables so first-run
  users see what to do next instead of a blank table.
- **Auto-save and inline name editing** in the Streaming editor, plus context menu
  to hide/show disabled streams.
- **Channel Inactivity Monitor** now only triggers when a channel is actively
  processing, reducing false alerts.
  See [docs/inactivity-monitoring](src/main/resources/docs/inactivity-monitoring.md).
- **Open Log button** added for one-click access to the current log file.
- **Bazineta JMBE fork option** in the JMBE library creator.
- **Native JNI build pipeline** with a multi-platform dynamic loader and a
  VOLK-accelerated FIR filter implementation. Falls back gracefully when no
  native compiler is available.
- **Windows launcher (`SDRTrunk.exe`)** is now generated dynamically as part of
  the build, with multi-resolution Windows taskbar icons for HiDPI displays.

### Changed
- **JavaFX migration continued.** Spectrum, waterfall, broadcast status, history
  management, help viewer, signal power, sidebar, audio output device, MQTT
  preferences, record preferences, and the empty tuner editor have all moved to
  JavaFX/FXML. The Map panel now runs inside a JavaFX shell.
- **Reactive Channel Map model** replaces the legacy AbstractTableModel for
  faster, more consistent updates.
- **Notifications editor** redesigned around per-recipient routing.
- **Channel table** Auto-Start toggle and Order spinner moved to a dedicated
  action box; double-click play/stop on rows has been removed in favor of the
  new context menu. Disabled auto-start is now indicated with an "X" icon.
- **Channel configuration header** now shows system, site, and channel name, and
  the surrounding border has been removed for a cleaner layout.
- **Alias configuration UX** redesigned to match the Channel configuration layout.
- **Talkgroup ID changes** in channels now sync automatically to alias lists.
- **P25 identifier fields** converted to dropdowns with channel autocomplete.
- **Squelch Tail and High Pass toggles** moved to the Audio Filters area.
- **Alerts menu item** moved to the bottom of the Channel Configuration sidebar.
- **AI quota errors** now display friendly, actionable messages instead of raw
  exceptions.
- **Performance:** waterfall rendering, hex formatting in P25 identifiers and
  network monitors, Icecast regex compilation, `ByteUtil.toHexString`,
  `StringUtils.replaceIllegalCharacters`, and TableCell icon caching have all
  been optimized. Live log rendering uses batched ListView updates.
- **Playlist file I/O** moved off the UI thread to keep the interface responsive
  on large playlists.
- **SDRPlay API and audio hardware warnings** for unused configurations are now
  logged at debug level instead of warning.
- **Decoder type icons** now use SVG with proper HiDPI fallback to PNG.

### Fixed
- **Path traversal and arbitrary execution** vulnerability in Script Actions.
- **Application crash on startup** caused by an invalid MigLayout `fill` constraint.
- **GUI NullPointerException** in the Audio Recordings panel and a StackOverflowError
  in SettingsCard.
- **JavaFX SceneState NullPointerException** and FlatSVGIcon usage issues.
- **SVG parsing** errors and a related log UI freeze caused by the StAX
  `maxAttributeSize` limit.
- **Talkgroup ID display** in the channel editor now correctly handles unsigned
  32-bit values; NBFM talkgroup max bound updated to the full unsigned range.
- **Talkgroup conflict dialog** now shows the correct channel name and a visual
  conflict indicator.
- **NBFM Configuration editor** no longer prompts to save when nothing changed.
- **NBFM Talkgroup ID Generator** now correctly persists state and passes the
  generated ID into the channel configuration.
- **Channel Auto-Start synchronization** between the table and editor.
- **Playlist editor** submenu now opens the requested tab.
- **Streaming editor** divider now shows the configuration view by default;
  status table columns are left-aligned; "Hide Disabled Streams" toggle reflects
  the actual filter state.
- **Map panel** rendering reliability improved by removing redundant FX wrappers.
- **Spectrum/waterfall** drawing and layout, including a fix for the panel
  resizing incorrectly inside a scroll pane.
- **Audio panel** layout no longer overflows the right content area.
- **Notification Preference editor** now scrolls when content exceeds the window.
- **Sidebar panel** components now render on the EDT to avoid intermittent blank
  panels at startup.
- **HiDPI scaling** for decoder icons in the channel editor.
- **County list population** in the Geographic Schema Generator.
- **Resource leak** in `AudioSegmentRecorder` audio recording.
- **Windows JNI build** failures caused by spaces in project paths and missing
  compilers (the JNI step now skips cleanly instead of failing the build).
- **AI Log Analyzer** now uses the Gemini model selected in user preferences.
- **AI optimize audio filter** button rendering and truncated status text.

## [0.6.2-ap-15] - 2026-05-01

### Added
- feat: Add Universal SDR USB Monitor for Windows
- Feat: Add Telegram Notifications and Gemini AI Audio Auditing
- Add System Health & Performance Advisor
- Add Intelligent Log Analysis with Gemini API
- Add Audio Recordings Panel UI
- Add popup dialogue for AI audio analyzer results
- Add variable placeholders to TwoTone text message templates
- Add What's New section to Help Viewer
- Add AIPreferenceEditor to User Preferences dialog
- Add new, refresh, clone, and save buttons to Two Tone Editor
- Add feature to prompt, manage, and update USB Monitor script on Windows 10+

### Changed
- Refactor PlaylistEditor and AliasEditor to align with HIG
- Refactor TwoToneEditor layout to remove accordions
- Replace TitledPane accordions with ScrollPanes in channel configuration editors
- Refactor LogsPanel to follow Apple HIG using JavaFX
- Refactor ChannelSpectrumPanel for Apple HIG compliance
- Refactor User Preferences UI for Apple HIG Compliance
- Refactor PlaylistManagerEditor to Apple HIG
- Refactor StreamingEditor to align with Apple HIG
- Refactor NowPlaying widgets to align with HIG card UI
- Refactor TunerViewPanel and DiscoveredTunerEditor to JavaFX
- Separate spectrum/waterfall display controls between Now Playing and Tuners pages
- ⚡ Bolt: Optimize `getIntAsHex` with fast char array lookup
- ⚡ Bolt: Optimize Hexadecimal String Formatting
- ⚡ Optimize empty int array allocation in BCH
- perf: optimize case conversion in CRCUtil using Locale.ROOT
- chore(performance): optimize System.currentTimeMillis loops in Calibration WARMUP routines
- ⚡ Performance: Pre-compile regex Pattern in AudioBufferManager
- Add ReentrantLock to HeterodyneChannelSourceManager for thread-safe access
- Security: Prevent command injection in ScriptAction via file validation

### Fixed
- Fix standard icon display and migration
- Fix blurry JavaFX and Swing icons on high DPI Windows displays
- Fix USB monitor powershell script installation
- Fix GUI stuck at startup by moving UsbMonitorManager dialog to EDT
- Fix HiDPI fuzzy rendering of icons in JavaFX and Swing on Windows 11
- Fix channel configuration editor overlapping the table
- Fix NullPointerException in JavaFX interop by avoiding JFXPanel reparenting
- Fix absolute path SVG icon loading in user preferences
- Fix XMLStreamException when parsing large SVG icons
- Fix TwoToneEditor TableView updates by converting primitive double variables to DoubleProperty
- Fix 'already set as root' exception for JavaFX embedded panels
- Fix IconManager selection bugs and missing icon previews
- Fix UI layout: remove empty space at the bottom of widgets on the now playing page

## [0.6.2-ap-14.6] - 2026-04-11

Runtime-diagnostics release. Adds per-category DEBUG toggles, persistent channel table sort,
and ThinLine Radio debug-by-default.

### Added
- Diagnostics preferences panel (Application -> Diagnostics (Logging)) with per-category
  DEBUG toggles for Zello, ThinLine Radio, Rdio Scanner, SDRPlay, RTL-SDR, the channelizer,
  the tuner manager, the P25 decoder, and NBFM/audio output.
- Master "Enable ALL diagnostics categories" checkbox with a warning about log volume.
- Runtime Logback level control (`LogLevelController`) that applies persisted preference
  state on startup and immediately when a checkbox is toggled, with no application restart.
- `FxTableColumnMonitor` helper that persists JavaFX TableView column widths, visible
  order, and sort order across restarts. Wired into the Channels editor with stable column
  ids (`channelTable.system`, `.site`, `.name`, `.frequency`, `.protocol`, `.playing`,
  `.autoStart`).
- ThinLine Radio logger defaults to DEBUG in `logback.xml` so live streaming sessions get
  full diagnostic output without user action.

### Fixed
- Channel table sort order and column widths are no longer lost when reopening the
  Channels menu. Previously the view reverted to the default ordering on every show.

### Changed
- `DiagnosticsPreference` is initialized by `UserPreferences` and immediately pushes its
  state into the Logback context via `LogLevelController.applyAll`, guaranteeing the user's
  last diagnostics selection is active from the first log line after startup.
- `.github/ISSUE_TEMPLATE/config.yml` now enables blank issues so fork issues can be filed
  without going through the upstream support wiki template.

## [0.6.2-ap-14.5] - 2026-04-10

- Treat "failed to start sending message" and "failed to stop sending message" as transient
  Zello server errors that trigger reconnect rather than broadcaster shutdown.
- Bumped `projectVersion` for the fix above.

## [0.6.2-ap-14.4] - 2026-04-09

- NBFM hiss reduction: wire new post-demod audio filters into the NBFM path.
- Default filter updates to reduce clipping on loud voice channels.
- Zello reconnect button on the broadcaster status panel for manual recovery.
- Verbose Zello diagnostic logging (session epoch, stream ids, opus state).
- Fix Opus encoder crash when frame size changed mid-session.
- Updated Zello broadcaster default configuration values.

## [0.6.2-ap-14.3] - 2026-04-07

- Earlier Zello reliability improvements (initial transient-error handling).
- Audio pipeline tuning for Cambridge COMIRS P25 trunking system.
- Rdio Scanner stream wiring and API-key reporting improvements.

[K.00.041]: https://github.com/snepple/sdrtrunk_kennebec/releases/tag/pr-K.00.041
[0.6.2-ap-14.6]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.6
[0.6.2-ap-14.5]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.5
[0.6.2-ap-14.4]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.4
[0.6.2-ap-14.3]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.3
[0.6.2-ap-15]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v15
