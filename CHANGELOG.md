# Changelog

All notable changes in the `actionpagezello/sdrtrunk` fork are documented here. Upstream
DSheirer/sdrtrunk changes are not repeated; only the `ap-` fork deltas are recorded.

Versioning follows `0.6.2-ap-<n>` where `<n>` increments for each fork release.

## [K.00.041] - 2026-05-08

Weekly Kennebec release. Major UI modernization push (JavaFX migration of core panels),
new in-app help and tooltips throughout, expanded Notifications and AI Audio features,
and a large batch of UX fixes for channels, aliases, streaming, and audio recording.

### Added
- Right-click context menu on the Channel Table for quick actions.
- Inline editing in the Channel Table and Alias Table.
- "Import from Radio Reference" entry in the Channel Editor's New dropdown.
- New Notifications system with per-recipient routing, alerts, and Telegram/SMTP settings.
- AI Audio Monitoring per channel and an "AI Optimize Audio Filters" button on NBFM channels.
- System Health notifications and an enhanced Log Analyzer UI with progress indicator and Markdown rendering.
- Geographic ID generator and Talkgroup ID generator for NBFM channels.
- Two Tone detection alert and streaming icons in the Audio Channel Panel.
- Streaming service icons in the Alias Editor lists.
- Visual conflict indicator and channel-name detail in the talkgroup conflict dialog.
- Duplicate-talkgroup detection when saving a channel.
- Editable ComboBox in the NBFM Talkgroup Editor showing channel details.
- Auto-sync of Talkgroup ID changes to Alias lists.
- Confirmation dialog before deleting a map track.
- Helpful empty-state messages and actionable placeholders across TableViews.
- Tooltips throughout the app: SDR Tuner settings, P25 Phase 1 and Phase 2 fields, DMR
  technical fields, WACN editor, NBFM Talkgroup ranges, decoder/preference editors,
  and icon-only buttons (for accessibility).
- In-app Help Viewer: organized categories, full-text search, and new guides for
  ThinLine Radio, OpenMHz streaming, Rdio Scanner streaming, Virtual Audio Cable
  routing, and Radio Reference imports.
- Discoverable "Open Log" button.
- "Bazineta JMBE fork" option in the JMBE creator.
- Multi-platform native JNI build with dynamic loader, plus a `compileJni` Gradle task.

### Changed
- Major UI modernization to JavaFX and Apple HIG: Spectrum and Waterfall panels,
  Broadcast Status panel, History Management panel, Help Viewer, Signal Power view,
  Sidebar, Streaming Editor, MQTT Preference Editor, Record Preference Editor,
  MP3 Preference, Notification Preferences, Empty Tuner Editor, and several legacy dialogs.
- Alias Configuration UX redesigned to match Channel Configuration.
- Two Tone Editor simplified with a clearer empty-state placeholder.
- Streaming Editor: auto-save, inline name editing, left-aligned status columns,
  context menu, dynamic configuration area, and a fixed initial divider position.
- Channel Configuration header now shows system, site, and name; structural border removed.
- Audio Filters: Squelch Tail and High Pass toggles relocated to the Audio Filters tab.
- Channel Spectrum panel split to a 50/50 layout.
- Sidebar toggle restyled with a custom icon and SDRTrunk logo when expanded.
- Decoder type icons now use SVG with proper high-DPI scaling.
- Auto-Start toggle and Order spinner relocated to the action box; auto-start "X" icon
  shown when disabled.
- Listen and Priority columns separated in the Alias Configuration Editor.
- P25 Identifier fields converted to dropdowns with channel autocomplete.
- AI review moved to table rows with an updated prompt; friendlier error messages
  when the AI quota is exhausted; AI Log Analyzer now respects the user's selected
  Gemini model.
- Channel Inactivity Monitor only triggers when the channel is actively processing.
- SDRPlay API and unused audio hardware warnings downgraded to debug level.
- Performance: faster waterfall rendering, batched live log updates, P25 hex
  formatting, ByteUtil hex output, single-pass StringUtils replacement, cached icon
  nodes in table cells, async playlist file I/O, and compiled icecast regex.
- Security hardening for ScriptAction (path traversal and arbitrary execution).
- Updated SDRTrunk logo, multi-resolution Windows taskbar icons, and high-DPI icon scaling.

### Fixed
- High-DPI scaling for decoder icons in the Channel Editor.
- Channel auto-start synchronization.
- NBFM Talkgroup ID Generator now correctly persists state and passes the generated
  ID into channel configuration.
- NBFM configuration editor no longer shows unintended save prompts.
- Talkgroup tooltip ranges and 32-bit unsigned bound corrected.
- Talkgroup ID display in the Channel Editor table for unsigned 32-bit values.
- Channel talkgroup updates now refresh the channel table.
- Playlist editor submenu now opens the requested tab.
- Alias and Streaming sidebar menu items now open the content window.
- Sidebar Alias and Streaming menu item routing.
- Map Panel rendering reliability.
- Spectrum waterfall drawing and layout issues.
- Spectral Display Panel resizing.
- Audio Panel layout constrained to the right content area.
- Audio Recordings Panel blank-layout and NPE issues.
- Frequency Editor layout.
- Notification Preference Editor scrollbar.
- Top content panel layout no longer spans the full width incorrectly.
- "Hide Disabled Streams" toggle now reflects the active filter state.
- JavaFX MenuButton dropdown when the header item was disabled.
- SwingNode initial rendering and JavaFX SceneState NPE.
- "Already set as root" exception path eliminated by removing unnecessary FX wrappers.
- SVG max attribute size parsing error and resulting log UI freeze.
- Icon loading falls back to PNG when an SVG is missing; FlatSVGIcon URL handling.
- Application crash on startup caused by an invalid MigLayout `fill` constraint.
- GUI NPE in AudioRecordingsPanel and StackOverflowError in SettingsCard.
- Resource leak in AudioSegmentRecorder.
- Truncated AI optimization button and wrapped status text.
- Channel configuration editor no longer overlaps the table.
- County list population in the Geographic Schema Generator.
- Windows C++ JNI build issue with spaces in paths; `compileJni` now skips when no
  compiler is present.

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

[0.6.2-ap-14.6]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.6
[0.6.2-ap-14.5]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.5
[0.6.2-ap-14.4]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.4
[0.6.2-ap-14.3]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v14.3
[0.6.2-ap-15]: https://github.com/actionpagezello/sdrtrunk/releases/tag/v15
[K.00.041]: https://github.com/snepple/sdrtrunk_kennebec/releases/tag/K.00.041
