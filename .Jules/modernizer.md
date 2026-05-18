## 2026-05-18 - AudioRecordingsPanel, SignalPowerView, and MapPanel
**Migration:** Migrated JFXPanels and JPanels that were wrapping pure JavaFX roots into full JavaFX implementations (BorderPane, HBox) and fixed parent references in ChannelSpectrumPanel and ControllerPanel. Used SwingNode to nest legacy Swing map controls in a BorderPane.

## 2026-05-08 - Modernize MapPanel integration
**Learning:** Legacy UI integration wrapped a Swing `MapPanel` in a `MapPanelFXWrapper` (which extended JavaFX `BorderPane` and embedded the Swing panel in a `SwingNode`), only to be later re-embedded in a `JFXPanel` inside `ControllerPanel`. This double-embedding (`Swing -> JFXPanel -> Scene -> MapPanelFXWrapper -> SwingNode -> Swing MapPanel`) caused rendering unreliability, delayed painting, and general content rendering issues.
**Action:** Removed `MapPanelFXWrapper` and the intermediary `JFXPanel` in `ControllerPanel`. Attached the Swing `MapPanel` directly to the `mCardPanel` (which is a Swing `JPanel` using `CardLayout`), thus resolving the rendering issues and streamlining the component hierarchy.
## $(date +%Y-%m-%d) - Modernize Standalone Frames and Dialogs
**Learning:** Legacy UI integration heavily relied on `javax.swing.JOptionPane` for popups and `javax.swing.JFrame` for standalone windows.
**Action:** Migrated `FilterEditor`, `SpectrumFrame`, `ChannelizerViewer`, `ChannelizerViewer2`, `HeterodyneChannelizerViewer`, and `SynthesizerViewer` to extend JavaFX `Stage` and use native `Scene` and `VBox` layouts. Systematically replaced all `JOptionPane` instances with `javafx.scene.control.Alert` and `TextInputDialog`, ensuring thread-safety by wrapping executions in `Platform.runLater()`.
## 2026-05-17 - Migrated BroadcastStatusPanel and AudioChannelsPanel
**Finding:** BroadcastStatusPanel and AudioChannelsPanel were using legacy Swing JTables and JPanels.
**Action:** Created JavaFX VBox and HBox equivalents using FXML, wrapped them in JFXPanels, and updated controllers to use JavaFX properties and observable lists, decoupling Swing implementations.
## $(date +%Y-%m-%d) - Modernized UI Leaf Menus
**Learning:** Legacy UI integration heavily relied on `javax.swing.JMenuItem` for menus. While migrating pure leaf components, care must be taken if they are still embedded within legacy Swing `JMenu` or `JPopupMenu` containers, as Swing containers cannot host JavaFX `MenuItem`s.
**Action:** Migrated 4 Tier 1 leaf files (`ColorSettingResetMenuItem.java`, `ColorSettingResetAllMenuItem.java`, `DisableSpectrumWaterfallMenuItem.java`, and `ShowTunerMenuItem.java`) to extend `javafx.scene.control.MenuItem`. Replaced internal action listeners with `setOnAction`. In legacy parent classes (like `SpectralDisplayPanel.java`), inline Swing `JMenuItem` instantiations were used as an intermediate bridge to maintain compatibility until the parent menus are fully migrated to JavaFX.
