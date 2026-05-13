import re

file_path = 'src/main/java/io/github/dsheirer/gui/playlist/alias/identifier/P25FullyQualifiedTalkgroupEditor.java'
with open(file_path, 'r') as f:
    content = f.read()

print("ComboBox<IdentifierValue> mTalkgroupField" in content)
