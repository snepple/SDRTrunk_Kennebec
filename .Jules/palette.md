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
## 2026-05-11 - Squelch Auto Track Mnemonic and Tooltip Formatting
Learning: JCheckBox configurations inside control panels (like SignalPowerView) are prime candidates for keyboard mnemonics (e.g., `Alt+A`) and require clear, HTML-wrapped tooltips and AccessibleContext settings to align with HIG accessibility guidelines.
Action: Add `setMnemonic(KeyEvent.VK_XX)` to frequently accessed checkboxes, structure long tooltips with `<html>` tags for wrapping, and always apply descriptive `AccessibleContext` names/descriptions.
## 2026-06-14 - [Settings Format Standardization]
**Learning:** [Apple HIG states preferences should be grouped in bordered card views with separators. The original manual VBox/HBox combinations broke layout consistency.]
**Action:** [Migrated Application Preference Editor to use standard SettingsCard and SettingsRow containers.]
## 2024-05-21 - Adding Tooltips to Preference Form Buttons
**Learning:** Adding context-specific tooltips to preference UI action buttons (like Select/Reset in JMBE library preferences) significantly improves accessibility and user understanding of what those actions actually do.
**Action:** When updating or creating new JavaFX preferences UI, always include `Tooltip`s for buttons to explain what they do.
## 2026-05-20 - Adding Tooltips to Numeric Spinners
**Learning:** Numeric input Spinners that lack a visual unit label need descriptive Tooltips explaining the specific unit (e.g., MB, seconds) and behavior. This greatly reduces cognitive load for technical configurations.
**Action:** Always verify if a numeric input needs a unit label, and default to adding an informative Tooltip if it's missing.

## 2024-05-13 - P25P2Viewer Context
**Learning:** Jargon-heavy configuration fields like WACN, System, and NAC can be confusing for users without context, leading to increased cognitive load and configuration errors.
**Action:** Adding HIG-compliant tooltips via info circle icons to these configuration labels provides immediate, deferential feedback and clarity, improving the user experience without cluttering the interface.
## 2026-06-14 - Adding Tooltips to JMBE Action Buttons
**Learning:** Action buttons in configuration panels (like `JmbeLibraryPreferenceEditor.java`) that lack textual context or explanation for their underlying complex behaviors (e.g., checking for updates, file selection, removing a library) cause user friction and fail accessibility checks.
**Action:** Always verify if action buttons in JavaFX preference editors lack tooltips and add descriptive `Tooltip` objects explaining their specific functionality (e.g., "Select an existing JMBE audio library file from your system.") to improve context and accessibility.
## 2024-05-23 - Adding Tooltips to Audio Test Buttons
**Learning:** Action buttons, especially those that trigger auditory feedback like "Test" buttons for playback devices or tones, should have descriptive Tooltips indicating what they are testing. This provides important context, especially since they are grouped with other similar testing functions.
**Action:** Always add descriptive `Tooltip` components to test buttons in audio playback configuration panels.

## 2024-05-18 - Directory Preference Layout HIG Update
**Learning:** Legacy `GridPane` layouts for preferences often break aesthetic integrity due to mismatched alignment and unstandardized spacing, making it difficult for the user to intuitively understand grouping.
**Action:** Always replace legacy nested layout panels (like `HBox` containing `GridPane`) with a vertical flow `VBox` housing `SettingsCard` and `SettingsRow` elements. This provides consistent standard spacing, automatic right-alignment of trailing controls, and clear visual grouping according to Apple HIG principles.
## 2024-05-16 - Add help tooltips to MPT1327 Channel Configuration fields
**Learning:** Users need contextual help to understand what configuration options like "Max Traffic Channels" and "Call Timeout" mean in MPT1327 systems without leaving the interface.
**Action:** Added help tooltips (createHelpIcon) to labels that provide explanations of these properties on hover to align with HIG accessibility and clarity principles.
## 2024-05-23 - Adding Tooltips to General Test and Action Buttons
**Learning:** Action buttons (like "Test", "Add", "Remove") in configuration panels (e.g., `AIPreferenceEditor.java` and `NotificationPreferenceEditor.java`) that lack textual context or explanation for their underlying behaviors cause user friction and fail accessibility checks.
**Action:** Always verify if action buttons in JavaFX preference editors lack tooltips and add descriptive `Tooltip` objects explaining their specific functionality (e.g., "Test the provided Gemini API key...") to improve context and accessibility.

## 2024-05-17 - Add Placeholder Guidance to Notification Editors
**Learning:** Complex technical inputs (like SMTP settings, API tokens) often lack clear context, causing user hesitation. Placeholder text combined with descriptive tooltips provides necessary guidance without cluttering the UI.
**Action:** Always add `setPromptText` alongside `setTooltip` for inputs requiring specific formats or technical values (e.g., ports, hostnames, tokens).
## 2026-05-17 - HIG Compliant Tooltips for Editors
**Learning:** Discovered more opportunities for HIG compliant help tooltips on labels instead of directly on input fields in the alias identifier and channel configuration editors.
**Action:** Applied `createHelpIcon` and attached it to the Labels for Talkgroup, Talkgroup Range, Channel Bandwidth, Squelch Threshold, Squelch Auto-Track, and Talkgroup To Assign across various UI configuration editors.
## 2026-06-08 - Adding Tooltips and A11y to Action Buttons
**Learning:** Action buttons like "Send Test", "Add Recipient", and "Remove Selected" lack semantic meaning for screen readers without proper A11y bindings. For destructive actions, consequence warnings are required.
**Action:** Always add `Tooltip`s with clear functionality, `accessibleTextProperty`, `accessibleHelpProperty`, and mnemonics to JavaFX action buttons. Include "This action cannot be undone." in tooltips for destructive actions.
## 2026-06-09 - Add accessibility warnings to destructive track deletion buttons
**Learning:** Destructive actions (like deleting tracks) in JavaFX applications need clear consequence warnings in tooltips and explicit accessible descriptions to comply with Apple HIG and ensure screen reader compatibility.
**Action:** Always include a warning like 'This action cannot be undone.' in tooltips for destructive buttons, assign keyboard mnemonics, and set `accessibleTextProperty` and `accessibleHelpProperty`.
## 2024-06-10 - Sidebar Toggle Button Accessibility
**Learning:** Icon-only buttons in JavaFX FXML, such as the toggle button in the Sidebar, lack clear meaning for screen readers and new users without proper accessible text and tooltips.
**Action:** Always add `accessibleText` attributes and explicit `<tooltip>` elements for icon-only `<Button>` definitions in FXML files to improve both usability (mouse users) and accessibility (screen reader users).
## 2024-03-08 - Accessible Recording Buttons
**Learning:** Destructive actions and key functionality in JavaFX lack mnemonics, accessible descriptions, and informative tooltips describing consequences.
**Action:** When adding buttons, especially destructive ones like delete, provide descriptive tooltips including consequence warnings, add mnemonics to enable keyboard navigation, and set accessible properties (`accessibleTextProperty` and `accessibleHelpProperty`) for screen reader support.
## 2026-06-14 - Adding Tooltips and A11y to Calibration Buttons
**Learning:** Action buttons for operations like "Reset All Calibrations" lack semantic meaning for screen readers without proper A11y bindings. For destructive actions like reset, consequence warnings are required.
**Action:** Always add `accessibleTextProperty`, `accessibleHelpProperty`, and mnemonics to JavaFX action buttons. Include "This action cannot be undone." in tooltips for destructive actions.
## 2026-06-14 - Adding Tooltips and Mnemonics to Audio Output Device Editor
**Learning:** Legacy UI FXML definitions often lack accessibility support, specifically missing keyboard mnemonics on Labels/Checkboxes, not utilizing `labelFor` binding for ComboBoxes/Sliders, and lacking descriptive tooltips. This makes keyboard navigation impossible and obscures functionality for screen readers and new users.
**Action:** When updating FXML-based editors (like `AudioOutputDeviceEditor.fxml`), always add `mnemonicParsing="true"` with underscores on text elements, correctly link `<Label>` to inputs using `labelFor="$fxid"`, and embed `<tooltip><Tooltip text="..."/></tooltip>` definitions to explain specific technical settings.
