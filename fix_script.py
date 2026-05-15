import re

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "r") as f:
    content = f.read()

content = content.replace("import io.github.dsheirer.gui.control.IntegerTextField;",
                          "import io.github.dsheirer.gui.control.IntegerTextField;\nimport javafx.scene.control.ComboBox;")
content = content.replace("import java.util.List;", "import java.util.List;\nimport java.util.TreeSet;")

content = content.replace("private IntegerTextField mWacnTextField;", "private ComboBox<Integer> mWacnComboBox;")
content = content.replace("private IntegerTextField mSystemTextField;", "private ComboBox<Integer> mSystemComboBox;")
content = content.replace("private IntegerTextField mNacTextField;", "private ComboBox<Integer> mNacComboBox;")

content = content.replace("GridPane.setConstraints(getWacnTextField(), 1, row);", "GridPane.setConstraints(getWacnComboBox(), 1, row);")
content = content.replace("gridPane.getChildren().add(getWacnTextField());", "gridPane.getChildren().add(getWacnComboBox());")

content = content.replace("GridPane.setConstraints(getSystemTextField(), 3, row);", "GridPane.setConstraints(getSystemComboBox(), 3, row);")
content = content.replace("gridPane.getChildren().add(getSystemTextField());", "gridPane.getChildren().add(getSystemComboBox());")

content = content.replace("GridPane.setConstraints(getNacTextField(), 5, row);", "GridPane.setConstraints(getNacComboBox(), 5, row);")
content = content.replace("gridPane.getChildren().add(getNacTextField());", "gridPane.getChildren().add(getNacComboBox());")

wacn_method = """    private ComboBox<Integer> getWacnComboBox()
    {
        if(mWacnComboBox == null)
        {
            mWacnComboBox = new ComboBox<>();
            mWacnComboBox.setEditable(true);
            mWacnComboBox.setDisable(true);
            mWacnComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mWacnComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mWacnComboBox;
    }"""
content = re.sub(r'    private IntegerTextField getWacnTextField\(\)\s*\{\s*if\(mWacnTextField == null\)\s*\{\s*mWacnTextField = new IntegerTextField\(\);\s*mWacnTextField.setDisable\(true\);\s*mWacnTextField.textProperty\(\).addListener\(\(observable, oldValue, newValue\) -> modifiedProperty\(\).set\(true\)\);\s*\}\s*return mWacnTextField;\s*\}', wacn_method, content)

sys_method = """    private ComboBox<Integer> getSystemComboBox()
    {
        if(mSystemComboBox == null)
        {
            mSystemComboBox = new ComboBox<>();
            mSystemComboBox.setEditable(true);
            mSystemComboBox.setDisable(true);
            mSystemComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mSystemComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSystemComboBox;
    }"""
content = re.sub(r'    private IntegerTextField getSystemTextField\(\)\s*\{\s*if\(mSystemTextField == null\)\s*\{\s*mSystemTextField = new IntegerTextField\(\);\s*mSystemTextField.setDisable\(true\);\s*mSystemTextField.textProperty\(\).addListener\(\(observable, oldValue, newValue\) -> modifiedProperty\(\).set\(true\)\);\s*\}\s*return mSystemTextField;\s*\}', sys_method, content)

nac_method = """    private ComboBox<Integer> getNacComboBox()
    {
        if(mNacComboBox == null)
        {
            mNacComboBox = new ComboBox<>();
            mNacComboBox.setEditable(true);
            mNacComboBox.setDisable(true);
            mNacComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mNacComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mNacComboBox;
    }"""
content = re.sub(r'    private IntegerTextField getNacTextField\(\)\s*\{\s*if\(mNacTextField == null\)\s*\{\s*mNacTextField = new IntegerTextField\(\);\s*mNacTextField.setDisable\(true\);\s*mNacTextField.textProperty\(\).addListener\(\(observable, oldValue, newValue\) -> modifiedProperty\(\).set\(true\)\);\s*\}\s*return mNacTextField;\s*\}', nac_method, content)

content = content.replace("getWacnTextField().setDisable(false);", "getWacnComboBox().setDisable(false);")
content = content.replace("getSystemTextField().setDisable(false);", "getSystemComboBox().setDisable(false);")
content = content.replace("getNacTextField().setDisable(false);", "getNacComboBox().setDisable(false);")

populate = """            java.util.Set<Integer> knownWacns = new java.util.TreeSet<>();
            java.util.Set<Integer> knownSystems = new java.util.TreeSet<>();
            java.util.Set<Integer> knownNacs = new java.util.TreeSet<>();
            if(getPlaylistManager() != null && getPlaylistManager().getChannelModel() != null) {
                for(io.github.dsheirer.controller.channel.Channel channel : getPlaylistManager().getChannelModel().getChannels()) {
                    if(channel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2 p25) {
                        ScrambleParameters sp = p25.getScrambleParameters();
                        if(sp != null) {
                            knownWacns.add(sp.getWACN());
                            knownSystems.add(sp.getSystem());
                            knownNacs.add(sp.getNAC());
                        }
                    }
                }
            }
            getWacnComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownWacns));
            getSystemComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownSystems));
            getNacComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownNacs));
"""
content = content.replace("            ScrambleParameters scrambleParameters = decodeConfig.getScrambleParameters();", populate + "\n            ScrambleParameters scrambleParameters = decodeConfig.getScrambleParameters();")


content = content.replace("getWacnTextField().set(scrambleParameters.getWACN());", "getWacnComboBox().setValue(scrambleParameters.getWACN());\n                getWacnComboBox().getEditor().setText(String.valueOf(scrambleParameters.getWACN()));")
content = content.replace("getSystemTextField().set(scrambleParameters.getSystem());", "getSystemComboBox().setValue(scrambleParameters.getSystem());\n                getSystemComboBox().getEditor().setText(String.valueOf(scrambleParameters.getSystem()));")
content = content.replace("getNacTextField().set(scrambleParameters.getNAC());", "getNacComboBox().setValue(scrambleParameters.getNAC());\n                getNacComboBox().getEditor().setText(String.valueOf(scrambleParameters.getNAC()));")

content = content.replace("getWacnTextField().set(0);", "getWacnComboBox().setValue(0);\n                getWacnComboBox().getEditor().setText(\"0\");")
content = content.replace("getSystemTextField().set(0);", "getSystemComboBox().setValue(0);\n                getSystemComboBox().getEditor().setText(\"0\");")
content = content.replace("getNacTextField().set(0);", "getNacComboBox().setValue(0);\n                getNacComboBox().getEditor().setText(\"0\");")

content = content.replace("getWacnTextField().setDisable(true);", "getWacnComboBox().setDisable(true);")
content = content.replace("getSystemTextField().setDisable(true);", "getSystemComboBox().setDisable(true);")
content = content.replace("getNacTextField().setDisable(true);", "getNacComboBox().setDisable(true);")

content = content.replace("int wacn = getWacnTextField().get();", "Integer wacnVal = getWacnComboBox().getValue(); if (wacnVal == null) { try { wacnVal = Integer.parseInt(getWacnComboBox().getEditor().getText()); } catch(Exception e) {} } int wacn = wacnVal != null ? wacnVal : 0;")
content = content.replace("int system = getSystemTextField().get();", "Integer systemVal = getSystemComboBox().getValue(); if (systemVal == null) { try { systemVal = Integer.parseInt(getSystemComboBox().getEditor().getText()); } catch(Exception e) {} } int system = systemVal != null ? systemVal : 0;")
content = content.replace("int nac = getNacTextField().get();", "Integer nacVal = getNacComboBox().getValue(); if (nacVal == null) { try { nacVal = Integer.parseInt(getNacComboBox().getEditor().getText()); } catch(Exception e) {} } int nac = nacVal != null ? nacVal : 0;")

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "w") as f:
    f.write(content)
