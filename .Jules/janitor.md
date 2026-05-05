## 2024-05-05 - Windows High-DPI Taskbar Icons
**Finding:** Passing a single image to `setIconImage()` in Java Swing on Windows historically leads to blurry icons on high-DPI displays.
**Action:** Use `mMainGui.setIconImages(List<Image>)` passing the single high-res image wrapped in a list to bypass the aggressive downscaling, and additionally use `java.awt.Taskbar.getTaskbar().setIconImage()` to properly set the modern high-DPI Windows Taskbar icon.
