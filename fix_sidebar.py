with open('src/main/java/io/github/dsheirer/gui/SidebarPanel.java', 'r') as f:
    content = f.read()

# Update toggle button logic
content = content.replace('JButton toggleBtn = new JButton(mCollapsed ? "\\u2630" : "\\u2715");', 'JButton toggleBtn = new JButton(mCollapsed ? "\\u276F" : "\\u2715");')

# Ensure hidemode 3 in SidebarItem
content = content.replace('mView = new JPanel(new MigLayout("insets 8, gapx 10", "[][grow]", "[]"));', 'mView = new JPanel(new MigLayout("insets 8, gapx 10, hidemode 3", "[][grow]", "[]"));')

# Ensure hidemode 3 in SubItem
content = content.replace('mView = new JPanel(new MigLayout("insets 6 30 6 6", "[grow]", "[]"));', 'mView = new JPanel(new MigLayout("insets 6 30 6 6, hidemode 3", "[grow]", "[]"));')

with open('src/main/java/io/github/dsheirer/gui/SidebarPanel.java', 'w') as f:
    f.write(content)
