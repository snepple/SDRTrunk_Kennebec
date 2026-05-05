1. **Disable Double-Click to Toggle Play/Stop**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java` and remove or comment out the `setOnMouseClicked` handler inside `getChannelTableView()`.

2. **Add Context Menu for Play/Stop**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java`.
    - Modify the `setRowFactory` for `mChannelTableView` to add a `ContextMenu`.
    - Add "Play" and "Stop" `MenuItem`s. Use `mPlaylistManager.getChannelProcessingManager().start(item)` and `.stop(item)`.
    - Dynamically update the enable/disable state of these items inside `contextMenu.setOnShowing()` based on `item.isProcessing()`.

3. **Update Auto-Start Column Icon**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java`.
    - Update `autoStartColumn.setCellFactory` so that when `item` is `false`, it displays an `IconNode` with `jiconfont.icons.font_awesome.FontAwesome.TIMES` colored `Color.RED`.
    - Follow the memory constraint by instantiating the graphic node once and reusing it within the `TableCell` implementation.

4. **Enable Inline Table Editing**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java`.
    - Add `import javafx.scene.control.cell.TextFieldTableCell;` if not present.
    - Call `mChannelTableView.setEditable(true)`.
    - Set the `CellFactory` for `systemColumn`, `siteColumn`, and `nameColumn` to `TextFieldTableCell.forTableColumn()`.
    - Add `setOnEditCommit` handlers for each to update the `Channel` via `setSystem`, `setSite`, and `setName`.
    - Also invoke `getChannelConfigurationEditor().setItem(editedItem)` if the item being edited is the same as the one selected, as instructed in memory.
    - For `autoStartColumn`, change its column definition to allow edits and update the custom cell factory to attach a `setOnMouseClicked` listener to toggle the underlying `autoStart` property when double-clicked, firing an update so the "CHECK" or "TIMES" icon toggles.

5. **Verify Changes for ChannelEditor**:
    - Use `run_in_bash_session` to run `cat src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java | grep -n -A 10 "ContextMenu"` and other commands to verify the edits.

6. **Add Preferred Tuner Column**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java`.
    - Add `TableColumn<Channel, String> preferredTunerColumn = new TableColumn<>("Preferred Tuner");`.
    - Add a `CellValueFactory` that fetches `SourceConfiguration` from the `Channel`, casts it to `SourceConfigTuner` or `SourceConfigTunerMultipleFrequency`, and returns a `SimpleStringProperty(getPreferredTuner())`.

7. **Rename Tuner Column to Current Tuner**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java`.
    - Change `TableColumn<Channel, String> tunerColumn = new TableColumn<>("Tuner");` to `TableColumn<Channel, String> tunerColumn = new TableColumn<>("Current Tuner");`.

8. **Verify Column Changes**:
    - Use `run_in_bash_session` to run `cat src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java | grep -n "Current Tuner"` and `cat src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelEditor.java | grep -n "Preferred Tuner"` to verify column edits.

9. **Relocate Auto-Start Toggle**:
    - Use `run_in_bash_session` to execute a python script to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelConfigurationEditor.java`.
    - Remove the `getAutoStartSwitch()` logic from `getTextFieldPane()`.
    - Insert `getAutoStartSwitch()` next to the `Reset` and `Save` buttons inside `actionBox` (around line 155).

10. **Verify Changes for ChannelConfigurationEditor**:
    - Use `run_in_bash_session` to run `cat src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelConfigurationEditor.java | grep -n -C 5 "actionBox.getChildren()"` to ensure `getAutoStartSwitch()` was relocated properly.

11. **Run tests**:
    - Use `run_in_bash_session` to execute `./gradlew test --tests "io.github.dsheirer.*"`.
    - If there are failures, fix them.

12. Complete pre commit steps to ensure proper testing, verification, review, and reflection are done.

13. Submit the completed task.
