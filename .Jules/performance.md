## 2026-05-07 - The Canvas Performer: Refactoring WaterfallPanel to direct Memory Writing
**Learning:** Rendering complex visualizations using individual JavaFX shapes or redundant Canvas clears creates object overhead and blocks the EDT or JavaFX Application Thread.
**Action:** Use a ConcurrentLinkedQueue to stream calculated pixel rows directly to a JavaFX PixelWriter and split the WritableImage drawing into top/bottom segments.
## 2026-05-14 - Dash: AudioRecordingsPanel async delete offloading
**Learning:** Performing bulk deletions of files (e.g., Recordings) directly within the JavaFX UI thread leads to UI freezes, especially on slow disks or with a high number of files.
**Action:** Wrapped bulk deletion logic for "Delete Selected" and "Delete All" in `io.github.dsheirer.util.ThreadPool.CACHED.submit()`. Implemented batch UI updates via `Platform.runLater()` after the deletion loop finishes to avoid flooding the JavaFX application thread with individual remove events.
