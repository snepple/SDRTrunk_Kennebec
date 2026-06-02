import os
import re

base_dir = r"C:\Users\default.LAPTOP-U0KBII5M\SDRTrunk_Kennebec_Build\src\main\java\org\jdesktop\swingx"

def fix_imports(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replacements for imports
    replacements = {
        "java.awt.Color": "javafx.scene.paint.Color",
        "java.awt.Dimension": "javafx.geometry.Dimension2D",
        "java.awt.Insets": "javafx.geometry.Insets",
        "java.awt.Rectangle": "javafx.geometry.Bounds",
        "java.awt.geom.Rectangle2D": "javafx.geometry.BoundingBox", # Need a bounding box
        "javax.swing.event.MouseInputAdapter": "javafx.event.EventHandler", # approximate
        "java.awt.event.MouseEvent": "javafx.scene.input.MouseEvent",
        "java.awt.event.KeyAdapter": "javafx.event.EventHandler",
        "java.awt.event.KeyEvent": "javafx.scene.input.KeyEvent",
        "javax.swing.Timer": "javafx.animation.KeyFrame", # rough
        "java.awt.Cursor": "javafx.scene.Cursor",
        "jiconfont.swing.IconFontSwing": "jiconfont.javafx.IconFontFX", # Maybe? We can just comment out jiconfont
    }
    
    for k, v in replacements.items():
        content = content.replace("import " + k + ";", "import " + v + ";")
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".java"):
            fix_imports(os.path.join(root, file))

print("Done import replacements.")
