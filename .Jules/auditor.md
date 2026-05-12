## 2024-05-24 - Notification Editor Redesign
**Learning:** When moving complex global configuration into a per-recipient Master-Detail layout in JavaFX, using `SplitPane` with a dynamic `ListView` and a conditionally rendered `VBox` detail pane provides a very clean, Apple HIG-aligned UX. The use of `ControlsFX` `ToggleSwitch` controls paired with descriptive sub-labels significantly improves clarity over traditional dense checkbox grids.
**Action:** When refactoring other configuration panels that suffer from "spreadsheet grid" or "checkbox overload" anti-patterns, default to this Master-Detail pattern. Ensure descriptive text is muted (`-fx-text-fill: gray; -fx-font-size: 0.9em;`) and primary labels are bolded to establish a clear visual hierarchy.

## 2025-02-28 - [RecordingTunerEditor HIG Consistency]
**Global UI Constant:** All editor panels should strictly use the 8pt grid with standardized MigLayout gaps/insets for consistency, specifically adopting "insets 16 16 16 16, gapy 8" where explicit padding is required instead of arbitrary magic numbers.
