# Windows installer branding

Assets used by the `createInstaller` Gradle task to brand the Windows
`jpackage`/WiX installer wizard.

| File | Purpose | Format |
|------|---------|--------|
| `sdrtrunk-installer-banner.bmp` | Top banner on the interior wizard pages (`WixUIBannerBmp`) | 24-bit BMP, **493 × 58** |
| `sdrtrunk-installer-dialog.bmp` | Background of the Welcome / Finish pages (`WixUIDialogBmp`) | 24-bit BMP, **493 × 312** |
| `generate_installer_graphics.py` | Regenerates the two BMPs from the app icon + a rendered "SDRTrunk" wordmark | — |

The bitmaps must be **24-bit BMP with no alpha channel** — the WiX classic UI
ignores transparency, so the art sits on a solid white field. WiX draws its own
title/body text on the **left** of the banner and the **right** of the welcome
dialog, so the SDRTrunk branding is kept to the opposite, text-free region.

## Regenerating the graphics

```bash
pip install Pillow
python3 src/main/installer/windows/generate_installer_graphics.py
```

Source art lives in `src/main/resources/images/` (`SDRTrunk_Application_Icon.png`).

## How it is wired into the build

`createInstaller` (in `build.gradle`) passes these to `jpackage` on Windows:

* `--license-file`, `--vendor`, `--description`, `--copyright`, `--about-url`
  — metadata shown in the wizard and in Windows "Installed apps".
* `--win-dir-chooser` — enables the full WixUI_InstallDir wizard (welcome /
  license / folder / progress / finish), which is what surfaces these bitmaps.
* `--win-upgrade-uuid` — a fixed UUID so new versions upgrade cleanly in place.
* `--resource-dir <build>/installer/wix-resources` — the task writes an
  `overrides.wxi` there at build time that sets `WixUIBannerBmp` /
  `WixUIDialogBmp` to absolute paths of the BMPs above.

> **First-build note:** the installer only builds on Windows with the WiX
> Toolset installed, and was not buildable in the dev environment. If a given
> WiX/jpackage version rejects the bitmap override, run the task once with
> `jpackage --verbose` — it prints the exact override resource names it expects.
> Removing the `--resource-dir` line falls back to the stock (unbranded) wizard.
