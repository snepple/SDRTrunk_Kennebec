1. **Enable column menu in `StreamingEditor`:**
   - In `src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java`, modify the `TableView` configuration to enable the column menu:
     ```java
     mConfiguredBroadcastTableView.setTableMenuButtonVisible(true);
     ```
   - Also, in `StreamingEditor`, when setting up the columns for `TableView`, assign an `id` to each column. This is required by `FxTableColumnMonitor` to persist and restore the column state properly.
     ```java
     enabledColumn.setId("enabled");
     nameColumn.setId("name");
     typeColumn.setId("format");
     stateColumn.setId("status");
     errorColumn.setId("error");
     ```

2. **Track visibility in `FxTableColumnMonitor`:**
   - In `src/main/java/io/github/dsheirer/preference/javafx/FxTableColumnMonitor.java`, update it to persist and restore the column's visibility. Add `KEY_VISIBLE` and store/retrieve the boolean value for each column.
   - Listen to visibility changes and trigger save when column visibility is toggled by the user.

3. **Hide empty columns in `FxTableColumnMonitor` or Table creation:**
   - Since the user explicitly asked: "do not show any columns that are empty (no column header and no data assigned)", I need to examine the `TableView` creation in `StreamingEditor`. Are there empty columns created without a header? Yes, wait, `typeColumn` has `setText("Format")`, wait!
   - Actually, JavaFX TableView automatically adds an empty trailing column to fill the space if columns do not cover the whole width, or the table menu button appears as an extra column. Wait, looking at the user's issue: "do not show any columns that are empty (no column header and no data assigned)". In the image provided, there are empty columns on the right side.
   - JavaFX TableView column resizing policy might be needed to eliminate the extra empty column. Let's check `TableView.CONSTRAINED_RESIZE_POLICY`.

4. **Add `FxTableColumnMonitor` to `StreamingEditor`:**
   - Modify `StreamingEditor.java` to instantiate `FxTableColumnMonitor` for `mConfiguredBroadcastTableView`.
   - Pass `UserPreferences` to `StreamingEditor`. Wait, `PlaylistManager` has `getUserPreferences()`. We can add a getter for it if it doesn't exist, or just pass `UserPreferences` in the `StreamingEditor` constructor, but wait, maybe `PlaylistManager` already provides it?

Let's verify `PlaylistManager` for `UserPreferences`.
