import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replacements
    content = content.replace("javax.swing.JPanel", "javafx.scene.layout.Pane")
    content = content.replace("javax.swing.JComponent", "javafx.scene.layout.Region")
    content = content.replace("javax.swing.SwingUtilities", "javafx.application.Platform")
    content = content.replace("extends JPanel", "extends Pane")
    content = content.replace("extends JComponent", "extends Region")
    content = re.sub(r"import javax\.swing\.[a-zA-Z0-9_]+;", "", content)
    content = content.replace("import javax.swing.*;", "")
    content = content.replace("import java.awt.Graphics2D;", "import javafx.scene.canvas.GraphicsContext;")
    content = content.replace("import java.awt.Graphics;", "")
    content = content.replace("Graphics2D ", "GraphicsContext ")
    content = content.replace("Graphics ", "GraphicsContext ")
    content = content.replace("paintComponent(GraphicsContext g)", "draw(GraphicsContext g)")
    content = content.replace("super.paintComponent(g);", "")
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = r"C:\Users\default.LAPTOP-U0KBII5M\SDRTrunk_Kennebec_Build\src\main\java\org\jdesktop\swingx"
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".java"):
            process_file(os.path.join(root, file))

print("Done basic replacements.")
