# JavaFX Transition Plan for SDRTrunk

## Current State Assessment (May 2024)
SDRTrunk has made significant progress in adopting JavaFX. The application entry point `io.github.dsheirer.gui.SDRTrunk` now extends JavaFX `Application`.
However, the architecture is currently inverted: the JavaFX outer application hosts a massive legacy Swing `JPanel` (`mMainContentPanel`) via an embedded `SwingNode`. Inside this Swing tree, various components use `JFXPanel` (the bridge class) to embed smaller, modernized JavaFX components back into the Swing hierarchy.

**Current Metrics:**
*   **1** files still extend `JFrame` (e.g., `EventFilterButton.java`, `SymbolViewerFX.java`, `SyncResultsViewer.java`).
*   **40** files still extend `JPanel` (representing the bulk of the legacy UI).
*   **25** files utilize `SwingNode` to bridge Swing components into JavaFX.

## Multi-Phase Transition Plan

### Phase 1: Migrate Remaining Standalone Windows and Dialogs
Before tackling the complex, deeply-nested main window, convert the final standalone Swing windows that float independently of the main layout.
*   **Action:** Convert the remaining 4 classes extending `JFrame` to extend JavaFX `Stage` or utilize custom JavaFX `Scene` graphs.
*   **Action:** Ensure all usages of `JOptionPane` across the codebase (used for alerts and confirmations) have been replaced with JavaFX `javafx.scene.control.Alert`.
*   **Key Consideration:** Wrap new window launches or dialog triggers occurring from background events strictly in `Platform.runLater()`.

### Phase 2: Bottom-Up Leaf Component Migration (The 40 JPanels)
Work from the bottom of the UI tree upwards. Target the smallest, lowest-level of the 40 remaining Swing `JPanel` components that do not contain any nested custom Swing classes.
*   **Action:** Rewrite pure Swing leaf components (e.g., `MapPanel`, `AudioRecordingsPanel`, `SignalPowerView`) using native JavaFX layouts (`VBox`, `HBox`, `GridPane`) and controls.
*   **Action:** If these new JavaFX components must still be hosted inside a legacy Swing parent container temporarily, wrap them in a `JFXPanel`. Use MVC patterns and JavaFX Properties for data binding.
*   **Key Consideration:** Avoid blocking the Swing EDT. Cache JavaFX property values or safely bridge them using listeners.

### Phase 3: Refactor Intermediate Containers
As the leaf components become JavaFX, begin refactoring the mid-level grouping containers.
*   **Action:** Convert intermediate Swing containers like `FilterEditorPanel`, `TunerEditor`, and various viewer panels (e.g., `ChannelizerViewer` panels) to pure JavaFX.
*   **Action:** As a container is converted to JavaFX, remove any `JFXPanel` wrappers from its children (added in Phase 2). The children can now be added directly to the parent JavaFX layout.

### Phase 4: Root Application Layout Inversion (The Big Switch)
With the sub-components and sidebar panels migrated, the core application layout can be updated to natively support them.
*   **Action:** In `SDRTrunk.java`, replace the instantiation of the legacy `mMainContentPanel` (a `JPanel` inside a `SwingNode`) with a native JavaFX root container, such as `BorderPane`.
*   **Action:** Attach the now-native JavaFX `SidebarPanel`, `ControllerPanel`, and spectral displays directly to this new JavaFX root. Wrap any straggling legacy Swing sub-components in individual `SwingNode`s rather than one monolithic Swing tree.
*   **Action:** Completely remove the initial `SwingNode` instantiation and the `javax.swing.SwingUtilities.invokeLater()` initialization block from the main application startup sequence.

### Phase 5: Eradication and Cleanup
Ensure no legacy Swing code or bridging mechanisms remain.
*   **Action:** Search for and remove all remaining `JFXPanel` and `SwingNode` wrappers across the codebase, as everything should now be natively nested in JavaFX.
*   **Action:** Eradicate all `javax.swing.*` and `java.awt.*` (where used for UI layout) imports.
*   **Action:** Audit all event bus listeners and asynchronous callbacks. Convert any lingering `SwingUtilities.invokeLater()` or `EventQueue.invokeLater()` calls to `Platform.runLater()` to guarantee thread safety on the JavaFX Application Thread.
