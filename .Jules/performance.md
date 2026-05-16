## 2026-05-07 - The Canvas Performer: Refactoring WaterfallPanel to direct Memory Writing
**Learning:** Rendering complex visualizations using individual JavaFX shapes or redundant Canvas clears creates object overhead and blocks the EDT or JavaFX Application Thread.
**Action:** Use a ConcurrentLinkedQueue to stream calculated pixel rows directly to a JavaFX PixelWriter and split the WritableImage drawing into top/bottom segments.

## 2023-10-27 - Radio Reference Import Bottleneck
**Learning:** Mass alias creation and iteration during Radio Reference talkgroup imports blocks the UI thread if run synchronously.
**Action:** Wrap the bulk alias creation logic in `CompletableFuture.supplyAsync`, extracting necessary UI properties on the application thread first, and update the AliasModel in a `Platform.runLater` block.
## 2024-10-24 - Registry Monitor Polling Loop Thread Usage Optimization
**Finding:** Synchronous `Thread.sleep(2000)` blocking the `mRegistryMonitorThread` in `ThemeManager.java`. A full thread was unnecessarily dedicated to a low-frequency polling mechanism, waking every two seconds just to verify a registry key and immediately going back to sleep.
**Action:** Replaced the dedicated Thread with a `ScheduledFuture` managed by `ThreadPool.SCHEDULED`. Utilizing `scheduleWithFixedDelay(new Runnable() {...}, 0, 2, TimeUnit.SECONDS)`, the system can execute the fast polling action directly using the shared executor and save the memory/scheduler overhead of keeping a custom, constantly sleeping thread around.
