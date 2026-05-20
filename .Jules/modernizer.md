# JavaFX Migration Phase 1

- Migrated `ClearableHistoryModel`, `MessageActivityModel`, `DecodeEventModel`, and `ChannelMetadataModel` to use `ObservableList` and run updates via `Platform.runLater()`.
- Refactored `DecodeEventPanel`, `MessageActivityPanel`, `ChannelMetadataPanel`, and `NowPlayingPanel` to extend JavaFX layout components (like `VBox`).
- Introduced new FXML descriptors for cleanly separating TableView concerns from the component lifecycle initialization.
- Modified the `Widget` class to accept both Swing `JComponent` and native JavaFX `Node` components, enabling incremental modernization without breaking other features that still rely on Swing UI encapsulation.
