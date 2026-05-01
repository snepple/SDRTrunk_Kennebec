with open("src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "import org.slf4j.LoggerFactory;" in line:
        new_lines.append(line)
        new_lines.append("import io.github.dsheirer.preference.javafx.FxTableColumnMonitor;\n")
    elif 'mConfiguredBroadcastTableView.setItems(mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts());' in line:
        new_lines.append(line)
        new_lines.append('            mConfiguredBroadcastTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);\n')
    elif 'TableColumn<ConfiguredBroadcast,Boolean> enabledColumn = new TableColumn("Enabled");' in line:
        new_lines.append(line)
        new_lines.append('            enabledColumn.setId("enabled");\n')
    elif 'TableColumn nameColumn = new TableColumn("Name");' in line:
        new_lines.append(line)
        new_lines.append('            nameColumn.setId("name");\n')
    elif 'TableColumn typeColumn = new TableColumn();' in line:
        new_lines.append(line)
        new_lines.append('            typeColumn.setId("format");\n')
    elif 'TableColumn stateColumn = new TableColumn("Stream Status");' in line:
        new_lines.append(line)
        new_lines.append('            stateColumn.setId("status");\n')
    elif 'TableColumn errorColumn = new TableColumn("Last Error");' in line:
        new_lines.append(line)
        new_lines.append('            errorColumn.setId("error");\n')
    elif 'mConfiguredBroadcastTableView.getColumns().addAll(enabledColumn, nameColumn, typeColumn, stateColumn, errorColumn);' in line:
        new_lines.append(line)
        new_lines.append('            new FxTableColumnMonitor(mPlaylistManager.getUserPreferences(), mConfiguredBroadcastTableView, "streamingEditorTable");\n')
    else:
        new_lines.append(line)

with open("src/main/java/io/github/dsheirer/gui/playlist/streaming/StreamingEditor.java", "w") as f:
    f.writelines(new_lines)
