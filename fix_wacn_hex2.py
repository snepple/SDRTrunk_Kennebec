import re

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "r") as f:
    content = f.read()

wacn_conv = """            mWacnComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? String.format("%X", object) : ""; }
                @Override
                public Integer fromString(String string) {
                    try {
                        string = string.trim();
                        if(string.startsWith("0x") || string.startsWith("0X")) string = string.substring(2);
                        return Integer.parseInt(string, 16);
                    } catch(NumberFormatException e) { return null; }
                }
            });"""

sys_conv = """            mSystemComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? String.format("%X", object) : ""; }
                @Override
                public Integer fromString(String string) {
                    try {
                        string = string.trim();
                        if(string.startsWith("0x") || string.startsWith("0X")) string = string.substring(2);
                        return Integer.parseInt(string, 16);
                    } catch(NumberFormatException e) { return null; }
                }
            });"""

nac_conv = """            mNacComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? String.format("%X", object) : ""; }
                @Override
                public Integer fromString(String string) {
                    try {
                        string = string.trim();
                        if(string.startsWith("0x") || string.startsWith("0X")) string = string.substring(2);
                        return Integer.parseInt(string, 16);
                    } catch(NumberFormatException e) { return null; }
                }
            });"""

content = re.sub(r'mWacnComboBox\.setConverter\([^)]+\)\s*\{\s*@Override\s*public String toString[^}]+\}\s*@Override\s*public Integer fromString[^}]+\}\s*\}\);', wacn_conv, content)
content = re.sub(r'mSystemComboBox\.setConverter\([^)]+\)\s*\{\s*@Override\s*public String toString[^}]+\}\s*@Override\s*public Integer fromString[^}]+\}\s*\}\);', sys_conv, content)
content = re.sub(r'mNacComboBox\.setConverter\([^)]+\)\s*\{\s*@Override\s*public String toString[^}]+\}\s*@Override\s*public Integer fromString[^}]+\}\s*\}\);', nac_conv, content)

content = content.replace("getWacnComboBox().getEditor().setText(String.valueOf(scrambleParameters.getWACN()));", "getWacnComboBox().getEditor().setText(String.format(\"%X\", scrambleParameters.getWACN()));")
content = content.replace("getSystemComboBox().getEditor().setText(String.valueOf(scrambleParameters.getSystem()));", "getSystemComboBox().getEditor().setText(String.format(\"%X\", scrambleParameters.getSystem()));")
content = content.replace("getNacComboBox().getEditor().setText(String.valueOf(scrambleParameters.getNAC()));", "getNacComboBox().getEditor().setText(String.format(\"%X\", scrambleParameters.getNAC()));")


content = content.replace("wacnVal = Integer.parseInt(getWacnComboBox().getEditor().getText());", "wacnVal = Integer.parseInt(getWacnComboBox().getEditor().getText(), 16);")
content = content.replace("systemVal = Integer.parseInt(getSystemComboBox().getEditor().getText());", "systemVal = Integer.parseInt(getSystemComboBox().getEditor().getText(), 16);")
content = content.replace("nacVal = Integer.parseInt(getNacComboBox().getEditor().getText());", "nacVal = Integer.parseInt(getNacComboBox().getEditor().getText(), 16);")

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "w") as f:
    f.write(content)
