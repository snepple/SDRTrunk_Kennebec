## 2026-05-03 - NBFM Geographic Talkgroup Generator Data Constraints
**Learning:** For desktop interfaces attempting to generate complex sequence structures (e.g. 10-digit integers), verify the backing model properties and validation formatters support 64-bit integer values (`Long`/`long`). Using generic 32-bit `Integer` configurations often introduces hidden overflows.
**Action:** When asked to create numeric fields handling 10-digit IDs, explicitly update the respective properties across formatters, UI controllers, validation logic, and models to `Long` instead of `int`. Or as done in this workaround due to overarching codebase constraints, apply `String` manipulation and direct fallback setters with comments describing architecture limits.
## 2026-05-03 - Prevent Duplicate Talkgroup Configurations\n**Learning:** When users assign talkgroups to channels, they might accidentally create conflicts by assigning the same talkgroup to multiple channels, leading to undefined decoding behavior.\n**Action:** Implemented a pre-save validation hook `getConfiguredTalkgroup()` in the base `ChannelConfigurationEditor` and a conflict check against existing channels within the `save` action handler. This enforces unique talkgroup assignments across the application and improves user experience by explicitly informing them of the conflict before saving.
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
## 2024-05-14 - Empty State Placeholder Text
**Learning:** When a `TableView` is empty, displaying a generic "No [items] Configured" message is a missed UX opportunity. Users benefit from knowing exactly *how* to populate the table.
**Action:** Always provide an actionable empty state message. Update JavaFX `TableView` placeholders with instructions like `mTableView.setPlaceholder(new Label("Click the New button to create a new [Item]"));` to guide the user towards the next logical action.
## 2024-05-19 - Table Inline Editing and Context Menus
**Learning:** Adding double-click-to-edit to JavaFX TableViews using standard `TextFieldTableCell` greatly improves quick-data-entry UX. Creating custom TableCells (like for ColorPicker and ComboBox for Icons) takes more work but keeps the UI clean. Right-click context menus on table headers (Swing) provide an intuitive location for list filtering options without cluttering the UI.
**Action:** Use these patterns for data-heavy tables where users frequently update properties. Use header context menus for column-specific actions like filtering or sorting configuration.
## 2026-05-04 - Alias Configuration UX Overhaul\n**Learning:** When modernizing a JavaFX editor from a horizontal  to a vertical Master-Detail  style with a side menu (), properly converting layout nodes and dynamically updating a  content area is essential for mimicking standard application preferences UI.\n**Action:** Ensure consistent use of layout containers, remove bottom button panels when shifting actions to the header, and always remember to bind the header elements (like titles and icons) directly to the selected item's properties for instantaneous updates.
## 2024-05-18 - Alias Configuration UX Overhaul
**Learning:** When modernizing a JavaFX editor from a horizontal TabPane to a vertical Master-Detail SplitPane style with a side menu (ListView), properly converting layout nodes and dynamically updating a StackPane content area is essential for mimicking standard application preferences UI.
**Action:** Ensure consistent use of layout containers, remove bottom button panels when shifting actions to the header, and always remember to bind the header elements (like titles and icons) directly to the selected item's properties for instantaneous updates.
## 2024-05-04 - Sidebar List Sorting Fix
**Learning:** In JavaFX application editors that use a Sidebar list (ListView) with a content pane (StackPane) map, the order of panes displayed in the list depends heavily on when the `addConfigurationPane` method is called. Constructor order constraints often force items to the top unexpectedly if parent classes add them before subclasses add theirs.
**Action:** To force items to the bottom of the list (e.g. Alerts), extract their addition into a method (`setupAlertsPane()`) and have the subclasses call this method explicitly at the end of their respective constructors.
## 2024-05-18 - Explaining Technical Jargon
**Learning:** When presenting complex technical configuration parameters (like WACN, System ID, NAC) to the user, providing immediate contextual help via tooltips reduces cognitive load. Users should not have to leave the configuration tab to search documentation to understand what these fields mean or when they are required.
**Action:** Consistently apply `Tooltip` components to labels for domain-specific acronyms and required inputs in JavaFX configuration panels.
## 2026-05-05 - TableView Empty State Placeholders
**Learning:** When a `TableView` is empty, displaying a completely blank area is a missed UX opportunity and can confuse users. Providing an actionable empty state message or a clear indication that no data is present improves clarity.
**Action:** Always provide an actionable empty state message. Update JavaFX `TableView` placeholders using `table.setPlaceholder(new Label("Actionable guidance"));` to guide the user towards the next logical action or explain the empty state.
## 2026-05-06 - Missing Tooltips on Icon-Only Buttons in JavaFX Lists
**Learning:** When using dual-list selection interfaces (e.g., available vs. selected items with left/right arrow buttons) in JavaFX applications, the action buttons often only contain icons (`FontAwesome.ANGLE_RIGHT`, etc.) without text labels. This presents an accessibility issue as screen readers cannot announce their function and sighted users lack context on hover.
**Action:** Always add explicit `Tooltip` components to icon-only action buttons (e.g., `mAddButton.setTooltip(new Tooltip("Add selected item"))`). Ensure `javafx.scene.control.Tooltip` is imported when doing so.
## 2026-05-19 - HIG Modernization of MP3 Preferences
**Learning:** Legacy UI preferences often use `GridPane` and manual padding which doesn't align with the Apple Human Interface Guidelines (HIG). Upgrading to custom HIG-compliant `SettingsCard` and `SettingsRow` requires changing the root container from `HBox` or `GridPane` to `VBox` to properly stack cards. Furthermore, replacing plain labels with Tooltips on CheckBoxes and ComboBoxes improves accessibility and layout cleanliness.
**Action:** When updating preferences UI, check if `SettingsCard` and `SettingsRow` can replace `GridPane`s. Remember to add contextual `Tooltip`s to explain technical settings, and use `kennebec-secondary-text` for informational notices.

## 2026-05-20 - HIG Modernization of Record Preferences
**Learning:** Legacy UI preferences often use manual layout padding and `HBox`/`VBox` structures which doesn't align with the Apple Human Interface Guidelines (HIG). Upgrading to custom HIG-compliant `SettingsCard` and `SettingsRow` requires changing the root container to `VBox` to properly stack cards. Furthermore, replacing plain labels with Tooltips on ComboBoxes improves accessibility and layout cleanliness.
**Action:** When updating preferences UI, check if `SettingsCard` and `SettingsRow` can replace manual `HBox`/`VBox` logic. Remember to add contextual `Tooltip`s to explain technical settings.
## 2026-05-08 - Adding Tooltips to Preference Editors
**Learning:** Adding context-specific tooltips to preference UI components (like CheckBoxes and ComboBoxes) significantly improves accessibility and user understanding of what technical settings do.
**Action:** When updating or creating new JavaFX preferences UI, always include `Tooltip`s to explain technical settings, especially those that alter data formats or display behaviors.
## 2023-10-25 - Action Button Accessibility and Feedback
**Learning:** Legacy Swing buttons in lists or toolbars often lack accessibility text or feedback tooltips, which makes interaction ambiguous (violating HIG's Clarity and Feedback rules) and impedes screen readers. Action buttons that are destructive need to indicate this clearly in the tooltip.
**Action:** When working on Swing panels with action buttons (like TunerViewPanel), ensure each JButton has `setToolTipText()`, mnemonics via `setMnemonic()`, and accessible contexts set using `getAccessibleContext().setAccessibleName()` and `getAccessibleContext().setAccessibleDescription()`.
## 2024-06-18 - [Spinner Tooltips]
**Learning:** Numeric input Spinners that lack a visual unit label need descriptive Tooltips explaining the specific unit (e.g. MB, seconds) and behavior. This greatly reduces cognitive load for technical configurations.
**Action:** Always verify if a numeric input needs a unit label, and default to adding an informative Tooltip if it's missing.
