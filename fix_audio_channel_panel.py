with open('src/main/java/io/github/dsheirer/audio/playback/AudioChannelPanel.java', 'r') as f:
    content = f.read()

content = content.replace('import javax.swing.JPanel;', 'import javax.swing.JPanel;\nimport javax.swing.UIManager;')

content = content.replace('private final Font mFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);', 'private final Font mFont = UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font").deriveFont(13f) : new Font(Font.SANS_SERIF, Font.PLAIN, 13);')

content = content.replace('mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, Color.BLACK);', 'mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, UIManager.getColor("Panel.background"));')
content = content.replace('mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, Color.LIGHT_GRAY);', 'mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, UIManager.getColor("Label.foreground"));')
content = content.replace('mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);', 'mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);')
content = content.replace('mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, Color.GREEN);', 'mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, UIManager.getColor("Label.foreground"));')

content = content.replace('setLayout(new MigLayout("align center center, insets 0 0 0 0",\n            "[][][align right]0[grow,fill]", ""));', 'setLayout(new MigLayout("align center center, insets 0 2 0 2",\n            "[][][align right]5[grow,fill]", ""));')

content = content.replace('mChannelName.setFont(mFont);', 'mChannelName.setFont(mFont.deriveFont(Font.BOLD));')
content = content.replace('mIdentifierLabel.setFont(mFont);', 'mIdentifierLabel.setFont(mFont.deriveFont(Font.BOLD));')

with open('src/main/java/io/github/dsheirer/audio/playback/AudioChannelPanel.java', 'w') as f:
    f.write(content)
