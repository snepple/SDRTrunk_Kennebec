import re

with open('src/main/java/io/github/dsheirer/gui/SidebarPanel.java', 'r') as f:
    content = f.read()

# Fix the duplicate JLabel variables
content = re.sub(r'(\s*private JLabel mTextLabel;){2,}', r'\1', content)

with open('src/main/java/io/github/dsheirer/gui/SidebarPanel.java', 'w') as f:
    f.write(content)
