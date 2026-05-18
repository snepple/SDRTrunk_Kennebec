# JavaFX Transition Plan for SDRTrunk

## Current State Assessment
SDRTrunk has already made significant progress in adopting JavaFX. The application entry point `io.github.dsheirer.gui.SDRTrunk` now extends JavaFX `Application`. However, the architecture is currently inverted: the JavaFX outer application hosts a massive legacy Swing `JPanel` (`mMainContentPanel`) via an embedded `SwingNode`. Inside this Swing tree, various components use `JFXPanel` (the bridge class) to embed smaller, modernized JavaFX components back into the Swing hierarchy (e.g., `WaterfallPanel`, `HelpViewer`, `ChannelDetailPanel`).

There are currently ~102 files still referencing `javax.swing`, including several top-level `JFrame` classes, standalone dialogs, and nested `JPanel` containers.

## Multi-Phase Transition Plan

### Phase 1: Migrate Standalone Windows and Dialogs
Before tackling the complex, deeply-nested main window, convert standalone Swing windows that float independently of the main layout.
*   **Action:** Convert all classes currently extending `JFrame` (e.g., `FilterEditor`, `SpectrumFrame`, `ChannelizerViewer`, `SynthesizerViewer`) to extend JavaFX `Stage` or utilize custom JavaFX `Scene` graphs.
*   **Action:** Replace all usage of `JOptionPane` across the codebase (used for alerts and confirmations) with JavaFX `javafx.scene.control.Alert`.
*   **Key Consideration:** Ensure all new window launches or dialog triggers occurring from background events (like `USBAlertEvent`) are strictly wrapped in `Platform.runLater()`.

### Phase 2: Bottom-Up Leaf Component Migration
Work from the bottom of the UI tree upwards. Identify the smallest, lowest-level Swing `JPanel` components that do not contain any nested custom Swing classes.
*   **Action:** Rewrite pure Swing leaf components (e.g., `BroadcastStatusPanel`, `AudioChannelsPanel`, `MapPanel`) using native JavaFX layouts (`VBox`, `HBox`, `GridPane`) and controls.
*   **Action:** If these new JavaFX components must still be hosted inside a legacy Swing parent container temporarily, wrap them in a `JFXPanel`.
*   **Key Consideration:** Enforce the MVC pattern. Decouple Swing `ActionListener` implementations by moving to JavaFX `Properties` and Data Binding, separating the UI from the underlying radio logic.

### Phase 3: Refactor Intermediate Containers
As the leaf components become JavaFX, begin refactoring the mid-level grouping containers.
*   **Action:** Convert intermediate Swing containers like `ControllerPanel`, `SidebarPanel`, and various tuner editors (e.g., `TunerEditor`, `AirspyTunerEditor`) to pure JavaFX.
*   **Action:** As a container is converted to JavaFX, remove any `JFXPanel` wrappers from its children (which were added in Phase 2 or prior). The children can now be added directly to the parent JavaFX layout.

### Phase 4: Root Application Layout Inversion (The Big Switch)
With the sub-components and sidebar panels migrated, the core application layout can be updated to natively support them.
*   **Action:** In `SDRTrunk.java`, replace the instantiation of the legacy `mMainContentPanel` (a `JPanel` inside a `SwingNode`) with a native JavaFX root container, such as `BorderPane`.
*   **Action:** Attach the now-native JavaFX `SidebarPanel`, `ControllerPanel`, and spectral displays directly to this new JavaFX root.
*   **Action:** Completely remove the `SwingNode` instantiation and the `javax.swing.SwingUtilities.invokeLater()` initialization block from the main application startup sequence.

### Phase 5: Eradication and Cleanup
Ensure no legacy Swing code or bridging mechanisms remain.
*   **Action:** Search for and remove all remaining `JFXPanel` wrappers across the codebase, as everything should now be natively nested in JavaFX.
*   **Action:** Eradicate all `javax.swing.*` and `java.awt.*` (where used for UI layout) imports.
*   **Action:** Audit all event bus listeners and asynchronous callbacks. Convert any lingering `SwingUtilities.invokeLater()` or `EventQueue.invokeLater()` calls to `Platform.runLater()` to guarantee thread safety on the JavaFX Application Thread.
