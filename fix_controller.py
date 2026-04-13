with open('src/main/java/io/github/dsheirer/controller/ControllerPanel.java', 'r') as f:
    content = f.read()

content = content.replace('add(mCardPanel, BorderLayout.CENTER);', '''add(mCardPanel, BorderLayout.CENTER);
        add(mAudioPanel, BorderLayout.NORTH);''')

with open('src/main/java/io/github/dsheirer/controller/ControllerPanel.java', 'w') as f:
    f.write(content)
