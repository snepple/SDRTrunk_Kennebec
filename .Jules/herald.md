## YYYY-MM-DD - JavaFX Integration Fixes
**Finding:** Changing views caused `IllegalStateException: Not on FX application thread` due to Swing modifying a BorderPane directly, and `JmbeCreator` crashed on Windows because of bad path management.
**Action:** Completed JavaFX `BorderPane` migration for `SDRTrunk.java` application root, embedding the legacy `mMainContentPanel` into individual `SwingNode` components and updating views asynchronously via `Platform.runLater()`. Fixed `JmbeCreator.java` to set the correct working directory and redirect error streams for the build script.
