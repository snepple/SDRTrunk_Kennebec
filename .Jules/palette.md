## 2024-05-18 - First Insights
Learning: SDR Trunk utilizes Java Swing and MigLayout heavily for its interface. It integrates `jiconfont.swing.IconFontSwing` with `FontAwesome` for its icons. The application's UI is migrating to FlatLaf for a modernized look.
Action: Utilize `IconFontSwing` to upgrade legacy icons. Use `MigLayout` features for generous spacing. Take advantage of `FlatLaf` features if appropriate. Pay attention to keyboard accessibility on `JToggleButton` and other interactive elements.

## 2024-05-18 - Mute button icons
Learning: The `AudioPanel` uses legacy low-resolution raster graphics `audio_muted.png` and `audio_unmuted.png` for its mute button.
Action: Upgrade the Mute button to use vector icons via `IconFontSwing.buildIcon` with `FontAwesome.VOLUME_OFF` and `FontAwesome.VOLUME_UP` to improve high-dpi rendering and Apple-style visual crispness.

## 2024-05-18 - AudioPanel Mute Button Icons Refinement
Learning: Using static variables for icons created via `IconFontSwing` breaks dark mode and dynamic theming, because `IconFontSwing.buildIcon` without an explicit color defaults to black, which is hardcoded at class-load time.
Action: To support FlatLaf dynamic themes, update icons dynamically using `UIManager.getColor("Label.foreground")` when creating vector icons for UI components.

## 2026-05-02 - Adding Tooltips to JavaFX Action Buttons
**Learning:** In desktop Java UI development (JavaFX/Swing), web-centric ARIA attributes (like `aria-label`) are not applicable. Instead, standard `Tooltip` objects should be used to provide accessible labels and hover context for action buttons.
**Action:** Always check if JavaFX/Swing UI components lack `setTooltip()` or `setToolTipText()` and add them to improve accessibility for screen readers and visual users, especially on icon-heavy or dense control panels.

## 2026-05-02 - Delete Map Tracks Confirmation
**Learning:** Implementing bulk or irreversible delete actions (e.g., Delete All, Delete Selected) in the UI always requires user approval via a confirmation dialogue window (e.g., JOptionPane.showConfirmDialog in Swing) before execution to prevent accidental data loss.
**Action:** Add confirmation dialogs before bulk delete actions.
## 2026-05-03 - Adding Tooltips to Icon-Only Buttons for Better Accessibility
**Learning:** JavaFX accessibility and interaction cues can be heavily improved by explicitly setting  objects on  controls. Specifically for icon-only buttons, which visually provide no text content, screen reader users and visual users alike benefit greatly from descriptive tooltips.
**Action:** When creating icon-only buttons (using  icons or SVG nodes), proactively instantiate and assign a  (e.g. ).
## 2026-05-03 - Adding Tooltips to Icon-Only Buttons for Better Accessibility
**Learning:** JavaFX accessibility and interaction cues can be heavily improved by explicitly setting Tooltip objects on Button controls. Specifically for icon-only buttons, which visually provide no text content, screen reader users and visual users alike benefit greatly from descriptive tooltips.
**Action:** When creating icon-only buttons, proactively instantiate and assign a Tooltip.
