#!/usr/bin/env python3
"""
Generate the Windows installer wizard bitmaps used by jpackage/WiX.

jpackage drives the WiX Toolset to build the Windows .exe/.msi installer. The
classic WixUI_InstallDir wizard reads two bitmaps:

  * WixUIBannerBmp - 493 x 58  - banner across the top of the interior pages
  * WixUIDialogBmp - 493 x 312 - full background of the Welcome / Finish pages

WiX draws its own title/body text on the LEFT of the banner and on the RIGHT of
the welcome dialog, so the SDRTrunk branding here is kept to the opposite,
text-free region (right of the banner, left strip of the dialog).

The repo has no text wordmark asset, so "SDRTrunk" is rendered from a font
next to the application-icon mascot. Output is 24-bit BMP with no alpha
(required by the WiX classic UI).

Re-run after changing the source art or text:
    python3 generate_installer_graphics.py
"""

import base64
import re
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
IMAGES = HERE.parent.parent / "resources" / "images"     # src/main/resources/images
ICON_PNG = IMAGES / "SDRTrunk_Application_Icon.png"
ICON_SVG = IMAGES / "SDRTrunk_Application_Icon.svg"

BANNER = HERE / "sdrtrunk-installer-banner.bmp"
DIALOG = HERE / "sdrtrunk-installer-dialog.bmp"

WORDMARK_TEXT = "SDRTrunk"
WHITE = (255, 255, 255)
INK = (40, 44, 52)                 # dark slate for the wordmark text
FALLBACK_ACCENT = (227, 90, 92)    # red, matching the mascot's accent

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]


def font(size):
    for path in FONT_CANDIDATES:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def load_rgba(source):
    return Image.open(source).convert("RGBA")


def render_svg(path):
    try:
        import cairosvg
    except ImportError:
        return None
    return load_rgba(BytesIO(cairosvg.svg2png(url=str(path), output_width=500, output_height=500)))


def embedded_png(path):
    match = re.search(r"data:image/png;base64,([^\"']+)", path.read_text(encoding="utf-8"))
    if not match:
        return None
    return load_rgba(BytesIO(base64.b64decode(match.group(1))))


def load_icon():
    if ICON_PNG.exists():
        return load_rgba(ICON_PNG)
    if ICON_SVG.exists():
        rendered = render_svg(ICON_SVG)
        if rendered is not None:
            return rendered
        embedded = embedded_png(ICON_SVG)
        if embedded is not None:
            return embedded
    raise FileNotFoundError(f"Could not load installer source art from {ICON_PNG} or {ICON_SVG}")


def flatten(rgba, bg=WHITE):
    base = Image.new("RGB", rgba.size, bg)
    base.paste(rgba, (0, 0), rgba)
    return base


def square(rgba, size):
    """Scale a (roughly square) image to size x size, preserving aspect."""
    w, h = rgba.size
    s = min(size / w, size / h)
    return rgba.resize((round(w * s), round(h * s)), Image.LANCZOS)


def dominant_accent(icon_rgba):
    """Pick a representative, reasonably saturated colour from the app icon."""
    small = icon_rgba.resize((64, 64), Image.LANCZOS)
    best, best_score = None, -1.0
    for count, (r, g, b, a) in small.getcolors(64 * 64) or []:
        if a < 200:
            continue
        mx, mn = max(r, g, b), min(r, g, b)
        if mx < 40 or (mn > 230 and mx - mn < 20):     # skip near-black / near-white
            continue
        sat = (mx - mn) / mx if mx else 0
        score = sat * count
        if score > best_score:
            best, best_score = (r, g, b), score
    return best or FALLBACK_ACCENT


def text_size(draw, s, fnt):
    l, t, r, b = draw.textbbox((0, 0), s, font=fnt)
    return r - l, b - t, l, t


def make_banner(icon, accent):
    """493x58: mascot + 'SDRTrunk' grouped on the RIGHT; WiX title text sits left."""
    img = Image.new("RGB", (493, 58), WHITE)
    draw = ImageDraw.Draw(img)
    mark = square(icon, 44)
    mw, mh = mark.size
    fnt = font(26)
    tw, th, tox, toy = text_size(draw, WORDMARK_TEXT, fnt)
    gap, pad = 8, 16
    total = mw + gap + tw
    x0 = 493 - total - pad
    img.paste(flatten(mark), (x0, (58 - mh) // 2), mark)
    draw.text((x0 + mw + gap - tox, (58 - th) // 2 - toy), WORDMARK_TEXT, font=fnt, fill=INK)
    draw.rectangle([0, 56, 492, 57], fill=accent)            # accent rule along bottom
    img.save(BANNER, "BMP")
    return img.size


def make_dialog(icon, accent):
    """493x312: mascot + 'SDRTrunk' in the left strip (x<130); WiX welcome text
    is drawn on the right over white."""
    img = Image.new("RGB", (493, 312), WHITE)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 5, 311], fill=accent)              # accent bar, far left
    mark = square(icon, 96)
    mw, mh = mark.size
    img.paste(flatten(mark), (22, 42), mark)
    fnt = font(26)
    tw, th, tox, toy = text_size(draw, WORDMARK_TEXT, fnt)
    tx, ty = 20, 42 + mh + 14
    draw.text((tx - tox, ty - toy), WORDMARK_TEXT, font=fnt, fill=INK)
    draw.rectangle([tx, ty + th + 8, tx + tw, ty + th + 10], fill=accent)  # underline
    img.save(DIALOG, "BMP")
    return img.size


def main():
    icon = load_icon()
    accent = dominant_accent(icon)
    b = make_banner(icon, accent)
    d = make_dialog(icon, accent)
    print(f"accent colour : {accent}")
    print(f"banner        : {BANNER.name} {b}")
    print(f"dialog        : {DIALOG.name} {d}")


if __name__ == "__main__":
    main()
