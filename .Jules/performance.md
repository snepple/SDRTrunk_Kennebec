## 2026-05-07 - The Canvas Performer: Refactoring WaterfallPanel to direct Memory Writing
**Learning:** Rendering complex visualizations using individual JavaFX shapes or redundant Canvas clears creates object overhead and blocks the EDT or JavaFX Application Thread.
**Action:** Use a ConcurrentLinkedQueue to stream calculated pixel rows directly to a JavaFX PixelWriter and split the WritableImage drawing into top/bottom segments.
## 2024-05-24 - AudioRecordingsPanel: Offload Files.deleteIfExists to ThreadPool
**Learning:** Performing `Files.deleteIfExists` in `AudioRecordingsPanel` blocks the UI thread, causing UI freezes when selecting and deleting many files synchronously.
**Action:** Move the disk deletion operation (`Files.deleteIfExists`) into a `ThreadPool.CACHED.submit(() -> {...})` background thread, and ensure the UI state (e.g. `mRecordings.remove(item)`) is updated within `Platform.runLater`. Disabled delete buttons during the operation and display error dialogs using `Platform.runLater` as well to ensure thread safety.
