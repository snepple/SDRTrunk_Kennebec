import sys
import subprocess

try:
    from PIL import Image
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image

png_path = r'C:\Users\default.LAPTOP-U0KBII5M\SDRTrunk_Kennebec_Build\src\main\resources\images\SDRTrunk_Application_Icon.png'
ico_path = r'C:\Users\default.LAPTOP-U0KBII5M\SDRTrunk_Kennebec_Build\src\main\resources\images\SDRTrunk.ico'
img = Image.open(png_path)
img.save(ico_path, sizes=[(16,16), (32,32), (48,48), (64,64), (128,128), (256,256)])
print("Saved ICO")
