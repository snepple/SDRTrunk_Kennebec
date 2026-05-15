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

content = re.sub(r'mWacnComboBox\.setConverter\([^;]+;\s*\}\s*\)\s*;\s*\}\s*\)\s*;', wacn_conv, content)
content = re.sub(r'mSystemComboBox\.setConverter\([^;]+;\s*\}\s*\)\s*;\s*\}\s*\)\s*;', sys_conv, content)
content = re.sub(r'mNacComboBox\.setConverter\([^;]+;\s*\}\s*\)\s*;\s*\}\s*\)\s*;', nac_conv, content)

with open("src/main/java/io/github/dsheirer/gui/playlist/channel/P25P2ConfigurationEditor.java", "w") as f:
    f.write(content)
