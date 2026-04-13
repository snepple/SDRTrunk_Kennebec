with open('src/main/java/io/github/dsheirer/audio/playback/AudioPanel.java', 'r') as f:
    content = f.read()

content = content.replace('import javax.swing.SwingUtilities;', 'import javax.swing.SwingUtilities;\nimport javax.swing.UIManager;\nimport javax.swing.BorderFactory;')

content = content.replace('setLayout(new MigLayout("insets 0 0 0 0", "[]0[grow,fill]", "[fill]0[]"));', 'setLayout(new MigLayout("insets 2 5 2 5", "[]5[grow,fill]", "[fill]0[]"));\n        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.LIGHT_GRAY));')

content = content.replace('setBackground(Color.BLACK);', 'setBackground(UIManager.getColor("Panel.background"));')

with open('src/main/java/io/github/dsheirer/audio/playback/AudioPanel.java', 'w') as f:
    f.write(content)
