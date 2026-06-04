import os
import re

directory = r'C:\Users\default.LAPTOP-U0KBII5M\SDRTrunk_Kennebec_Build\src\main\java\io\github\dsheirer'

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            # Find 'Alert alert = new Alert(...);' and append 'io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());'
            # Also handle if it's named 'alert' or something else, but looking at the grep, it's mostly 'Alert alert = new Alert(...);'
            
            new_content = re.sub(r'(Alert\s+(\w+)\s*=\s*new\s+Alert\([^)]+\);)', 
                                 r'\1 io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(\2.getDialogPane());', 
                                 content)
                                 
            # Some might be 'Dialog<ButtonType> dialog = new Dialog<>();'
            new_content = re.sub(r'(Dialog<[^>]+>\s+(\w+)\s*=\s*new\s+Dialog(?:<[^>]*>)?\([^)]*\);)',
                                 r'\1 io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(\2.getDialogPane());',
                                 new_content)

            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print("Updated " + file)
