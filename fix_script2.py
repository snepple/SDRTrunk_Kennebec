import re

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "r") as f:
    content = f.read()

content = content.replace("import io.github.dsheirer.gui.control.IntegerTextField;",
                          "import io.github.dsheirer.gui.control.IntegerTextField;\nimport javafx.scene.control.ComboBox;")

content = content.replace("private IntegerTextField mWacnTextField;", "private ComboBox<Integer> mWacnComboBox;")
content = content.replace("private IntegerTextField mSystemTextField;", "private ComboBox<Integer> mSystemComboBox;")
content = content.replace("private IntegerTextField mNacTextField;", "private ComboBox<Integer> mNacComboBox;")

content = content.replace("GridPane.setConstraints(getWacnTextField(), 1, row);", "GridPane.setConstraints(getWacnComboBox(), 1, row);")
content = content.replace("gridPane.getChildren().add(getWacnTextField());", "gridPane.getChildren().add(getWacnComboBox());")

content = content.replace("GridPane.setConstraints(getSystemTextField(), 3, row);", "GridPane.setConstraints(getSystemComboBox(), 3, row);")
content = content.replace("gridPane.getChildren().add(getSystemTextField());", "gridPane.getChildren().add(getSystemComboBox());")

content = content.replace("GridPane.setConstraints(getNacTextField(), 5, row);", "GridPane.setConstraints(getNacComboBox(), 5, row);")
content = content.replace("gridPane.getChildren().add(getNacTextField());", "gridPane.getChildren().add(getNacComboBox());")

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "w") as f:
    f.write(content)
