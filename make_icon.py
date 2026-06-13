"""
Regenerate SDRTrunk.ico from SDRTrunk_Application_Icon.png.

Pillow's built-in ICO saver silently drops the alpha channel.  This script
writes the ICO container by hand so every embedded PNG keeps its RGBA
transparency, which is required for the icon to look correct on Windows
taskbars, the installer, and the system tray.

Usage (from the repo root):
    python make_icon.py
"""
import io
import os
import struct
import sys

try:
    from PIL import Image
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
PNG_PATH = os.path.join(ROOT, "src", "main", "resources", "images", "SDRTrunk_Application_Icon.png")
ICO_PATH = os.path.join(ROOT, "src", "main", "resources", "images", "SDRTrunk.ico")

SIZES = [16, 32, 48, 64, 128, 256]

src = Image.open(PNG_PATH).convert("RGBA")
print(f"Source: {src.size} mode={src.mode}")

# Build per-size PNG blobs keeping RGBA
blobs = []
for sz in SIZES:
    resampled = src.resize((sz, sz), Image.LANCZOS)
    buf = io.BytesIO()
    resampled.save(buf, format="PNG")
    blob = buf.getvalue()
    color_type = blob[25]
    blobs.append((sz, blob))
    print(f"  {sz:3d}x{sz}: color_type={color_type} ({'RGBA' if color_type == 6 else 'RGB - MISSING ALPHA'})")

# Write ICO manually:  header + directory + image data
n = len(blobs)
header = struct.pack("<HHH", 0, 1, n)          # reserved, type=ICO, count
data_offset = 6 + n * 16

dir_entries = b""
image_data = b""
current_offset = data_offset
for sz, blob in blobs:
    w = sz if sz < 256 else 0   # 256 is encoded as 0 per the ICO spec
    dir_entries += struct.pack("<BBBBHHII", w, w, 0, 0, 1, 32, len(blob), current_offset)
    image_data += blob
    current_offset += len(blob)

with open(ICO_PATH, "wb") as f:
    f.write(header + dir_entries + image_data)

print(f"\nWrote {ICO_PATH}  ({os.path.getsize(ICO_PATH):,} bytes)")
