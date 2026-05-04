## 2026-05-03 - Add streaming icon to alias editor
**Learning:** To render custom icons in JavaFX ListView cells efficiently, reuse an `ImageView` instance initialized in the `ListCell` constructor and update its `Image` in the `updateItem` method, handling both populated and empty/null states.
**Action:** When adding icons to list items in JavaFX, use a custom `CellFactory` with a reused `ImageView` per cell instead of instantiating new UI components on every render.
## 2025-02-23 - Relocated UI Component with sequential labels\n**Learning:** When moving UI configuration components across JavaFX tabs (e.g. from Decoder to Audio Filters panes in NBFMConfigurationEditor.java), remember to account for and update sequentially numbered headers or section titles to maintain UX consistency.\n**Action:** Use sed to correctly replace and update all subsequent numbered labels when inserting a new UI component section into an existing list.
## 2026-05-18 - Remove border from channel configuration
**Learning:** To remove a border and box styling from a JavaFX container, find where the `preferences-card` or similar CSS class is added to the component's style class list and remove it.
**Action:** When a user requests to remove a box/border from a UI area, inspect the corresponding Java file for `.getStyleClass().add(...)` lines that apply border/card styles.
