## 2024-05-04 - Topic Selection
**Topic:** How to stream dispatch audio to Zello
**Reason:** Streaming audio to Zello is a popular feature mentioned in the "What's New" section of the help content but lacks a dedicated guide under the "Guides & Documentation" section.
## 2024-05-04 - Topic Selection Updated
**Topic:** How to stream to ThinLine Radio
**Reason:** ThinLine is listed in "What's New" under Streaming & Integrations ("Built-in streaming directly to ThinLine Radio with out-of-the-box debug logging."), but it has no guide. I will create a guide for it and update `HelpViewer.java`.
## 2024-05-04 - Topic Selection Updated
**Topic:** How to stream to Rdio Scanner
**Reason:** Rdio Scanner is listed in "What's New" under Streaming & Integrations, but it has no guide in the "Guides & Documentation" section. I will create a guide for it and update `HelpViewer.java`.
## 2024-05-04 - Topic Selection Updated
**Topic:** How to route audio with Virtual Audio Cable
**Reason:** Virtual Audio Cable (VAC) Routing is a specialized feature mentioned in the "What's New" section of the help content but lacks a dedicated guide under the "Guides & Documentation" section. I will create a guide for it and update `HelpViewer.java`.
## 2024-05-04 - Topic Selection Updated
**Topic:** How to stream to OpenMHz
**Reason:** OpenMHz streaming is a built-in feature but lacks a dedicated guide under the "Guides & Documentation" section. I will create a guide for it and update `HelpViewer.java`.
## 2024-05-04 - Topic Selection Updated
**Topic:** Radio Reference Import
**Learning:** The "Radio Reference Import" feature was listed as a new addition but lacked a dedicated Markdown guide.
**Action:** Created `src/main/resources/docs/radio-reference.md` and integrated it into the navigation tree of `HelpViewer.java`.
## 2024-05-08 - HelpViewer Organization & Search
**Topic:** Organize HelpViewer Categories & Full-Text Search
**Reason:** Organized the help documents into subcategories under "Guides & Documentation" for better navigation, and implemented full-text search to scan document contents, not just titles.

## 2026-05-09 - Markdown Compatibility in HelpViewer
**Learning:** The internal Java `HelpViewer` uses a standard Markdown parser and does not support MDX React components (like `<Steps>`, `<CardGroup>`, etc.) used by Mintlify.
**Action:** When syncing documentation from `docs/` to `src/main/resources/docs/`, strip MDX tags and replace them with standard markdown equivalent (e.g. converting `<Step>` into numbered lists, `<Card>` into headers/links).
## 2024-05-15 - Topic Selection
**Topic:** Ignore Unwanted Talkgroups
**Learning:** The "Ignore Unwanted Talkgroups" feature for DMR and P25 was listed in the "What's New" section but lacked a dedicated Markdown guide.
**Action:** Created `src/main/resources/docs/ignore-unwanted-talkgroups.md` and integrated it into the navigation tree of `HelpViewer.java` under "Advanced & System".
## 2024-05-19 - Topic Selection
**Topic:** P25 Talkgroup Override
**Learning:** The "P25 NAC Override" and talkgroup assignment features for P25 Phase 1 and 2 channels were listed in the "What's New" section but lacked a dedicated mention in the setup guide.
**Action:** Updated `src/main/resources/docs/p25.md` to include instructions on setting a Talkgroup Override when setting up a P25 channel.
## 2024-05-19 - Topic Selection
**Topic:** Spectrum & Waterfall Display
**Learning:** Added a new documentation guide covering the Spectrum & Waterfall display, as there was no getting started guide for this core navigational component, and added a signal flow Mermaid diagram to clarify interactions.
**Action:** Created `src/main/resources/docs/spectrum-&-waterfall.md` and integrated it into the navigation tree of `HelpViewer.java` under "Hardware & Tuners".
## 2024-06-10 - Topic Selection
**Topic:** Smart Bandwidth Optimization
**Learning:** The "Smart Bandwidth" feature lacked a comprehensive Mintlify-style guide with a Mermaid flow diagram and was missing from the Help Viewer navigation tree.
**Action:** Rewrote `src/main/resources/docs/smart-bandwidth.md` with visual diagrams, and integrated it into `HelpViewController.java` under "Advanced & System".
## 2026-06-14 - Topic Selection
**Topic:** Application Watchdog
**Learning:** The built-in Application Watchdog and its REST API endpoints for external monitoring were undocumented.
**Action:** Created `src/main/resources/docs/application-watchdog.md` with Mermaid diagram and integrated it into the `HelpViewController.java` under "Advanced & System".
## 2024-06-15 - Topic Selection
**Topic:** IAmResponding Integration
**Learning:** The "IAmResponding" feature was added but lacked its entry in the `HelpViewer` navigation tree under "Integrations & Streaming".
**Action:** Added `Iamresponding` node to `HelpViewController.java` to match the existing `iamresponding.md` file.
## 2026-06-16 - Topic Selection
**Topic:** Channel Images
**Learning:** The premium image upload pipeline was listed in "What's New" but lacked a dedicated Markdown guide.
**Action:** Created src/main/resources/docs/channel-images.md with visual diagrams and integrated it into the HelpViewer navigation tree.
## 2026-06-17 - Topic Selection
**Topic:** 3 New Documentation Guides Added
**Learning:** Three missing features from the "What's New" section (CTCSS/DCS/NAC Filtering, Native Sequential Paging, and Waterfall Audio Controls) lacked dedicated Markdown guides.
**Action:** Created `ctcss-dcs-nac-filtering.md`, `native-sequential-paging.md`, and `waterfall-audio-controls.md` adhering to the Visual-First Mintlify style, and integrated them into the navigation tree of `HelpViewController.java`.
## 2026-06-19 - Topic Selection
**Topic:** 3 New Audio Enhancement Guides Added
**Learning:** Three missing audio features from the "What's New" section (Analog Hiss Reduction, Anti-Clipping, and P25 Audio Enhancements) lacked dedicated Markdown guides.
**Action:** Created `analog-hiss-reduction.md`, `anti-clipping.md`, and `p25-audio-enhancements.md` adhering to the Visual-First Mintlify style, and integrated them into the navigation tree of `HelpViewController.java` under "Channels & Decoding".
## 2026-06-23 - Topic Selection
**Topic:** 3 New Documentation Guides Added
**Learning:** Three missing features/concepts from the "What's New" section and core logic (Audio Playback Bar, Streaming Connection Console, and Call Flow Logic) lacked dedicated Markdown guides.
**Action:** Created `audio-playback-bar.md`, `streaming-connection-console.md`, and `call-flow-logic.md` adhering to the Visual-First Mintlify style, and integrated them into the navigation tree of `HelpViewController.java`.
## 2026-06-24 - Topic Selection
**Topic:** 3 New Documentation Guides Added
**Learning:** Three missing features/concepts from the recent updates (Managing System Logs, P25 Phase 2 Setup, and Two Tone Aliases Setup) lacked dedicated Markdown guides.
**Action:** Created `managing-system-logs.md`, `p25-phase-2-setup.md`, and `two-tone-aliases-setup.md` adhering to the Visual-First Mintlify style, and integrated them into the navigation tree of `HelpViewController.java`.
## 2026-06-26 - Topic Selection
**Topic:** 3 New Documentation Guides Added
**Learning:** Three existing missing features/concepts from the recent updates (Audio Recordings, Zello Integration, and Virtual Audio Cable) lacked Mintlify-styled "Visual First" Markdown guides.
**Action:** Created `audio-recordings.md`, `zello.md`, and `virtual-audio-cable.md` adhering to the Visual-First Mintlify style and correctly mapped them to the `HelpViewController.java` navigation tree via the `help/` directory.
