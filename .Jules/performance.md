## 2026-05-07 - The Canvas Performer: Refactoring WaterfallPanel to direct Memory Writing
**Learning:** Rendering complex visualizations using individual JavaFX shapes or redundant Canvas clears creates object overhead and blocks the EDT or JavaFX Application Thread.
**Action:** Use a ConcurrentLinkedQueue to stream calculated pixel rows directly to a JavaFX PixelWriter and split the WritableImage drawing into top/bottom segments.
## Date: 2024-05-18
- **What**: Offloaded the `PlaylistManager.isPlaylist()` file I/O check in `PlaylistManagerEditor` to a background thread.
- **Why**: Prevented blocking of the JavaFX Application Thread (EDT) when users add an existing playlist, avoiding UI/waterfall stutter.
- **Impact**: Improves overall application responsiveness when adding playlists from the file system.
- **Measurement**: Add Playlist file check runs in background without freezing the UI.
