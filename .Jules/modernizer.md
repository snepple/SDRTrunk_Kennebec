## 2026-05-18 - AudioRecordingsPanel, SignalPowerView, and MapPanel
**Migration:** Migrated JFXPanels and JPanels that were wrapping pure JavaFX roots into full JavaFX implementations (BorderPane, HBox) and fixed parent references in ChannelSpectrumPanel and ControllerPanel. Used SwingNode to nest legacy Swing map controls in a BorderPane.

