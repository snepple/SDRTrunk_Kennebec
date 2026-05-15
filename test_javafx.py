import os

path = "./src/main/java/io/github/dsheirer/audio/playback/AudioChannelPanel.java"
with open(path, "r") as f:
    text = f.read()

import re

# We will create an AudioChannelView and AudioChannelController or just rewrite AudioChannelPanel directly.
