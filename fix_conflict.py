import re

with open('./src/main/java/io/github/dsheirer/source/tuner/rtl/RTL2832UnknownTunerEditor.java', 'r') as f:
    content = f.read()

fixed = content.replace(
'''<<<<<<< HEAD
        getTunerIdLabel().setText(getDiscoveredTuner().getName() + (hasTuner() ? " ID:" + getTuner().getUniqueID() : ""));
=======
        getTunerIdLabel().setText(getDiscoveredTuner().getId() + (hasTuner() ? " ID:" + getTuner().getUniqueID() : "") + getUsbInfo());
>>>>>>> origin/master''',
'''        getTunerIdLabel().setText(getDiscoveredTuner().getName() + (hasTuner() ? " ID:" + getTuner().getUniqueID() : "") + getUsbInfo());'''
)

with open('./src/main/java/io/github/dsheirer/source/tuner/rtl/RTL2832UnknownTunerEditor.java', 'w') as f:
    f.write(fixed)
