# JavaFX Transition Plan for SDRTrunk

## Current State Assessment (Latest)
SDRTrunk has made massive strides in adopting JavaFX. The application entry point `io.github.dsheirer.gui.SDRTrunk` now extends JavaFX `Application`.

**Current Metrics:**
*   **0** files still extend `JFrame` (Down from 4 - Stage Migration is Complete!).
*   **14** core application files still extend `JPanel` (Down from 40), excluding the 3rd-party `JXMapViewer` components.
*   **74** files utilize `SwingNode` to bridge legacy Swing components into JavaFX.
*   **65** files utilize `JFXPanel` to embed JavaFX back into Swing.

The temporary increase in `SwingNode` and `JFXPanel` instances is expected during the "Bridge Phase" of incremental migration.

## Multi-Phase Transition Plan (Updated)

### Phase 1: Complete the "Leaf" Panel Migrations (Remaining 14 JPanels)
We are down to the final set of legacy Swing `JPanel` components. These should be migrated to pure JavaFX components (e.g., `VBox`, `HBox`, `BorderPane`).

**Suggested Prompts for Agents:**
*   **Prompt 1 (Audio):** "Migrate `AudioPanel` and `AudioChannelPanel` from `JPanel` to JavaFX `VBox`/`HBox`. Replace any legacy Swing method calls with their JavaFX equivalents and wrap any `SwingNode` instances if needed."
*   **Prompt 2 (Metadata & Decode):** "Migrate `NowPlayingPanel`, `ChannelMetadataPanel`, `DecodeEventPanel`, and `MessageActivityPanel` to native JavaFX. Ensure to use JavaFX Observables for updates instead of Swing `repaint()`."
*   **Prompt 3 (Spectrum & Tuner):** "Migrate `SpectralDisplayPanel`, `OverlayPanel`, `FrequencyOverlayPanel`, and `TunerViewPanel` to JavaFX. *Caution*: High-frequency repaints must use `AnimationTimer` or custom coalescing, do not simply replace `repaint()` with `Platform.runLater()`."
*   **Prompt 4 (Controls):** "Migrate `JFrequencyControl`, `Editor`, `SynthesizerViewer`, and `ChannelSpectrumPanel` to JavaFX. Replace any `CardLayout` usage with `StackPane`."

*Note on Maps:* `JXMapViewer` and `JXMapKit` do not have native JavaFX equivalents. Retain these as Swing components and embed them using `SwingNode` when migrating their parents.

### Phase 2: Eliminate JFXPanel (Swing-in-FX-in-Swing)
As the parent Swing containers are migrated to JavaFX, we no longer need `JFXPanel` to host their JavaFX children.

**Suggested Prompts for Agents:**
*   **Prompt 5:** "Search for `JFXPanel` across the codebase. Now that their parent components are likely native JavaFX, remove the `JFXPanel` wrappers and add the JavaFX child components directly to the parent JavaFX layout. Change class inheritance from `JFXPanel` to `VBox`/`HBox`."

### Phase 3: Eliminate SwingNode (Where Possible)
Once all pure Swing components are either migrated or isolated (like `JXMapViewer`), we can remove the `SwingNode` wrappers.

**Suggested Prompts for Agents:**
*   **Prompt 6:** "Search for `SwingNode`. Remove it for components that have been fully migrated to JavaFX and attach them directly to the scene graph. Retain `SwingNode` *only* for necessary third-party Swing dependencies like `JXMapViewer`."

### Phase 4: Threading and Import Cleanup
Ensure no legacy Swing threading or imports remain outside of the explicit `SwingNode` boundaries.

**Suggested Prompts for Agents:**
*   **Prompt 7:** "Audit the codebase for `javax.swing.SwingUtilities.invokeLater()` and replace with `Platform.runLater()` where appropriate. Eradicate all `javax.swing.*` and `java.awt.*` imports (except those required for remaining `SwingNode` boundaries like `JXMapViewer`)."
