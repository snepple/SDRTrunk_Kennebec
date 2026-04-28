import re

with open('src/main/java/io/github/dsheirer/module/decode/p25/P25TrafficChannelManager.java', 'r') as f:
    content = f.read()

content = re.sub(
    r'mTS1ChannelGrantEventMap\.clear\(\);\s*mTS2ChannelGrantEventMap\.clear\(\);',
    r'mTS1ChannelGrantEventMap.clear();\n            mTS2ChannelGrantEventMap.clear();\n            mFrequencyBandMap.clear();',
    content
)

with open('src/main/java/io/github/dsheirer/module/decode/p25/P25TrafficChannelManager.java', 'w') as f:
    f.write(content)

with open('src/main/java/io/github/dsheirer/module/decode/p25/phase1/P25P1MessageProcessor.java', 'r') as f:
    content = f.read()

content = re.sub(
    r'//Only store the frequency band if it\'s new so we don\'t hold on to more than one instance of the\s*//frequency band message\.  Otherwise, we\'ll hold on to several instances of each message as they get\s*//injected into other messages with channel information\.\s*if\(!mFrequencyBandMap\.containsKey\(bandIdentifier\.getIdentifier\(\)\)\)\s*\{\s*mFrequencyBandMap\.put\(bandIdentifier\.getIdentifier\(\), bandIdentifier\);\s*\}',
    r'mFrequencyBandMap.put(bandIdentifier.getIdentifier(), bandIdentifier);',
    content
)

with open('src/main/java/io/github/dsheirer/module/decode/p25/phase1/P25P1MessageProcessor.java', 'w') as f:
    f.write(content)
