with open('src/main/java/io/github/dsheirer/audio/playback/AudioChannelsPanel.java', 'r') as f:
    content = f.read()

content = content.replace('import javax.swing.JSeparator;', 'import javax.swing.JSeparator;\nimport javax.swing.UIManager;')

content = content.replace('setLayout(new MigLayout("insets 0 0 0 0",\n            "[][sizegroup abc,grow,fill][][sizegroup abc,grow,fill]", "[grow,fill]"));', 'setLayout(new MigLayout("insets 0 5 0 5, gapx 5",\n            "[][sizegroup abc,grow,fill][][sizegroup abc,grow,fill]", "[grow,fill]"));')

content = content.replace('setBackground(Color.BLACK);', 'setBackground(UIManager.getColor("Panel.background"));')

# update separator color
content = content.replace('separator.setBackground(Color.DARK_GRAY);', 'separator.setForeground(UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.LIGHT_GRAY);\n        separator.setBackground(UIManager.getColor("Panel.background"));')

with open('src/main/java/io/github/dsheirer/audio/playback/AudioChannelsPanel.java', 'w') as f:
    f.write(content)
