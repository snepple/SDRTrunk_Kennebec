## 2026-05-17 - Migrated BroadcastStatusPanel and AudioChannelsPanel
**Finding:** BroadcastStatusPanel and AudioChannelsPanel were using legacy Swing JTables and JPanels.
**Action:** Created JavaFX VBox and HBox equivalents using FXML, wrapped them in JFXPanels, and updated controllers to use JavaFX properties and observable lists, decoupling Swing implementations.
