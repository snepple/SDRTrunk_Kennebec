## 2024-05-18 - ThreadingBridge Log Batching
**Learning:** `ThreadingBridge` was introducing unnecessary abstraction and logging overhead when bridging Swing and JavaFX. We can rely on `Platform.runLater()` and `SwingUtilities.invokeLater()` with `Platform.isFxApplicationThread()` and `SwingUtilities.isEventDispatchThread()` checks for synchronous optimizations.
**Action:** Always favor native JavaFX concurrency checks and execution mechanisms instead of creating custom abstraction layers. Use Strangler Fig to incrementally wrap old Swing UIs.

## 2024-05-24 - JavaFX Context Menus vs Swing
**Learning:** `JPopupMenu` and other Swing menus cannot natively host `javafx.scene.control.MenuItem`. A full migration is needed if we intend to modernize the menu items.
**Action:** Prioritize standalone dialogs and top-level Windows/Frames for JavaFX migration over highly nested context menu items which are coupled with Swing components.

## 2024-05-24 - JFXPanel vs Native Stage
**Learning:** Top-level Swing components such as `JFrame` dialogs can safely be replaced by JavaFX `Stage`s without the need for `JFXPanel` wrappers.
**Action:** Always check if a target is a top-level `JFrame` before applying `JFXPanel`. Use `Stage` directly instead.
