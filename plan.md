1. Explore `ControllerPanel.java`. Use `run_in_bash_session` with `cat ./src/main/java/io/github/dsheirer/controller/ControllerPanel.java` (I already have the full source from a previous `grep -v 'import '` command in the trace).
2. Update `SidebarPanel` to be a pure JavaFX component. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to rewrite `./src/main/java/io/github/dsheirer/gui/SidebarPanel.java` to extend `VBox`.
3. Verify `SidebarPanel.java` modification. Use `run_in_bash_session` with `cat` to ensure it extends `VBox`.
4. Create `SidebarPanelWrapper`. Use `run_in_bash_session` with `cat << 'EOF' > ./src/main/java/io/github/dsheirer/gui/SidebarPanelWrapper.java` to create the file which extends `JFXPanel` and embeds `SidebarPanel`.
5. Verify `SidebarPanelWrapper.java` creation. Use `run_in_bash_session` with `ls` and `cat`.
6. Update `HelpViewer` to be a pure JavaFX component. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to rewrite `./src/main/java/io/github/dsheirer/gui/help/HelpViewer.java` to extend `VBox` instead of `JFXPanel`.
7. Update `MapPanel` to be a pure JavaFX component. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to rewrite `./src/main/java/io/github/dsheirer/map/MapPanel.java` to extend `BorderPane` instead of `JFXPanel`.
8. Verify `HelpViewer` and `MapPanel` modifications using `run_in_bash_session` with `cat`.
9. Update `JavaFxWindowManager` to return JavaFX `Node` instead of `JFXPanel` from `getView()`. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to perform the modification.
10. Verify `JavaFxWindowManager` modification using `run_in_bash_session` with `cat`.
11. Update `ControllerPanel` to be a pure JavaFX component. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to rewrite `./src/main/java/io/github/dsheirer/controller/ControllerPanel.java` to extend `BorderPane`. Use `SwingNode` to wrap any child components that are still `JPanel` (like `NowPlayingPanel`, `TunerViewPanel`, `AudioRecordingsPanel`).
12. Verify `ControllerPanel.java` modification. Use `run_in_bash_session` with `cat`.
13. Create `ControllerPanelWrapper`. Use `run_in_bash_session` with `cat << 'EOF' > ./src/main/java/io/github/dsheirer/controller/ControllerPanelWrapper.java` to create the wrapper extending `JFXPanel`.
14. Verify `ControllerPanelWrapper.java` creation using `run_in_bash_session` with `cat`.
15. Update `SDRTrunk.java` to instantiate `SidebarPanelWrapper` and `ControllerPanelWrapper`. Use `run_in_bash_session` with `cat << 'EOF' > script.py && python3 script.py && rm script.py` to inject the changes.
16. Verify `SDRTrunk.java` modifications using `run_in_bash_session` with `cat`.
17. Run `./gradlew test` using `run_in_bash_session` to test the build and tests.
18. Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
