1. Migrate `HelpIconLabel` to JavaFX.
   - Use `run_in_bash_session` with `cat << 'EOF'` to completely rewrite `src/main/java/io/github/dsheirer/gui/help/HelpIconLabel.java` to extend `javafx.scene.control.Label`, using JavaFX `Tooltip` and `jiconfont.javafx.IconNode`.
   - Use `run_in_bash_session` to verify the rewrite with `cat`.
   - Update `src/main/java/io/github/dsheirer/filter/FilterEditorPanel.java` to wrap this new JavaFX `HelpIconLabel` inside a `JFXPanel` or integrate it as part of JavaFX, depending on its usage context.
   - Use `run_in_bash_session` with `cat` to verify `FilterEditorPanel.java` modifications.

2. Migrate `NotificationManager` to JavaFX.
   - Use `run_in_bash_session` with `cat << 'EOF'` to rewrite `src/main/java/io/github/dsheirer/gui/NotificationManager.java` to use JavaFX `Alert` instead of `JOptionPane` for fallback notifications, ensuring that UI updates are wrapped in `Platform.runLater()`.
   - Use `run_in_bash_session` to verify the rewrite with `cat`.

3. Migrate `Widget` and `WidgetContainer` to JavaFX.
   - Create `src/main/resources/fxml/Widget.fxml` and `src/main/resources/fxml/WidgetContainer.fxml` using `run_in_bash_session` with `cat << 'EOF'`.
   - Use `run_in_bash_session` with `cat << 'EOF'` to rewrite `src/main/java/io/github/dsheirer/gui/widget/Widget.java` to extend `javafx.scene.layout.VBox` or use `FXMLLoader`.
   - Use `run_in_bash_session` with `cat << 'EOF'` to rewrite `src/main/java/io/github/dsheirer/gui/widget/WidgetContainer.java` to use JavaFX layouts (`VBox` or `FlowPane`).
   - Modify `src/main/java/io/github/dsheirer/channel/metadata/NowPlayingPanel.java` to embed the new JavaFX `WidgetContainer` using a `JFXPanel`.
   - Use `run_in_bash_session` to verify all the file creations/rewrites with `ls` and `cat`.

4. Run tests and verify the build.
   - Use `run_in_bash_session` with `./gradlew classes test` to ensure that all changes compile properly and no tests are broken.

5. Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

6. Output final details.
   - Use `run_in_bash_session` with `echo 'Task complete'` to output PR completion status.
