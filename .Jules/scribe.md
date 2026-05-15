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
## 2024-05-20 - Aliases and Talkgroups Diagram
**Learning:** Users often confuse Aliases with Talkgroups—always include a 'Relationship' diagram for these.
**Action:** Added a Mermaid signal flow diagram to aliases-talkgroups.md to clarify the relationship between Frequency, Decoder, Alias, and Audio.
## 2024-05-21 - Topic Selection
**Topic:** Channels, Aliases, and Streaming
**Learning:** Users often struggle with the core interconnectivity of how a decoded channel routes to an alias, which then routes to a stream. A component map and signal flow diagram are essential here to clarify the system architecture for new users.
**Action:** Created `src/main/resources/docs/channels-aliases-streaming.md` and integrated it into the navigation tree of `HelpViewer.java` under "Organization & Playlists".
