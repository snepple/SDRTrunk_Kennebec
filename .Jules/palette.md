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
