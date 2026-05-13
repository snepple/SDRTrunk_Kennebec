import re

with open('src/main/java/io/github/dsheirer/gui/playlist/alias/identifier/RadioIdEditor.java', 'r') as f:
    print(f.read().find('TextField mRadioIdField'))
