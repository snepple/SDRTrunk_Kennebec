## 2026-05-03 - Add streaming icon to alias editor
**Learning:** To render custom icons in JavaFX ListView cells efficiently, reuse an `ImageView` instance initialized in the `ListCell` constructor and update its `Image` in the `updateItem` method, handling both populated and empty/null states.
**Action:** When adding icons to list items in JavaFX, use a custom `CellFactory` with a reused `ImageView` per cell instead of instantiating new UI components on every render.
