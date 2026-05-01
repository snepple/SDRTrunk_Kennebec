with open("src/main/java/io/github/dsheirer/preference/javafx/FxTableColumnMonitor.java", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if 'private static final String KEY_SORT_TYPE = ".sort.type.";' in line:
        new_lines.append(line)
        new_lines.append('    private static final String KEY_VISIBLE = ".visible";\n')
    elif 'private ChangeListener<Number> mWidthListener = (obs, oldVal, newVal) -> scheduleSave();' in line:
        new_lines.append(line)
        new_lines.append('    private ChangeListener<Boolean> mVisibilityListener = (obs, oldVal, newVal) -> scheduleSave();\n')
    elif 'column.widthProperty().removeListener(mWidthListener);' in line:
        new_lines.append(line)
        new_lines.append('                column.visibleProperty().removeListener(mVisibilityListener);\n')
    elif 'column.widthProperty().addListener(mWidthListener);' in line:
        new_lines.append(line)
        new_lines.append('            column.visibleProperty().addListener(mVisibilityListener);\n')
    elif 'entry.getValue().setPrefWidth(width);' in line:
        new_lines.append(line)
        new_lines.append('                }\n')
        new_lines.append('                String visibleStr = getStringPref(mKey + "." + entry.getKey() + KEY_VISIBLE);\n')
        new_lines.append('                if (visibleStr != null) {\n')
        new_lines.append('                    entry.getValue().setVisible(Boolean.parseBoolean(visibleStr));\n')
        new_lines.append('                }\n')
        new_lines.append('                if (false) { // dummy block\n')
    elif 'mUserPreferences.getSwingPreference().setInt(mKey + "." + column.getId() + KEY_WIDTH, (int) column.getWidth());' in line:
        new_lines.append(line)
        new_lines.append('                setStringPref(mKey + "." + column.getId() + KEY_VISIBLE, String.valueOf(column.isVisible()));\n')
    else:
        new_lines.append(line)

with open("src/main/java/io/github/dsheirer/preference/javafx/FxTableColumnMonitor.java", "w") as f:
    f.writelines(new_lines)
