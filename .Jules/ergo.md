## 2024-05-06 - P25P2 Configuration UX Enhancement **Learning:** P25 Phase 2 configuration currently lacks help tooltips with FontAwesome icons like Phase 1, and the toggle buttons don't have descriptions attached to help icons. **Action:** We'll add the `createHelpIcon` method and help icons to P25 Phase 2 configuration.
## 2024-05-18 - Notification Test Actions **Learning:** Added inline test buttons for Telegram and SMTP. **Action:** Next time consider moving network calls to background tasks as implemented.
## 2026-05-07 - SettingsCard and Tooltips for Complex Configs
**Learning:** Using SettingsCard with SettingsRow combined with tooltips and prompt texts reduces cognitive load when configuring advanced features like MQTT.
**Action:** Apply this pattern to other legacy JavaFX GridPane layouts to align with HIG.
## 2024-05-18 - DMR Configuration UX Enhancement **Learning:** DMR configuration lacks tooltips for technical checkboxes and limits, similar to P25P2. **Action:** Next time, ensure all decoder configuration panels apply the progressive disclosure help icon pattern uniformly to technical toggles.
## 2025-02-14 - [NBFMConfigurationEditor Unintended Save Prompts] **Learning:** When overriding `setItem(Channel)` in a subclass that also has UI change listeners modifying a global `modified` property, it's critical to surround `super.setItem()` with a `mLoadingConfiguration = true/false` flag and check it in the subclass's change listeners. Failure to do so can cause the `modified` property to instantly flip back to true immediately after the base class processes a `save()` event, triggering unintended 'Save Changes' prompts. **Action:** Audit other `ConfigurationEditor` implementations for similar issues where listeners are missing the `!mLoadingConfiguration` check.
## 2024-05-18 - Fix Bidirectional State Synchronization
**Learning:** Legacy UI toggles that rely on `save()` actions for global properties (like `autoStartProperty`) create a disjointed UX when other UI elements (like Table columns) update the property directly.
**Action:** When a UI element modifies a global model property instantly in one place, all other UI elements controlling the same property should use `bindBidirectional()` to stay in sync automatically.
## 2026-05-08 - Explicit Actions over Hidden Interactions
**Learning:** Depending purely on double-clicks or hidden interactions creates low discoverability. Explicit buttons (like Open) improve UX.
**Action:** Always provide explicit action buttons for important row actions in table views, even if double-click shortcuts exist.
## 2024-05-24 - Application Preference Tooltips
**Learning:** Legacy UI toggles for complex system interactions (like Watchdog or background scripts) lack explanatory tooltips, increasing user confusion.
**Action:** When adding new application-level configuration options, always provide an attached `Tooltip` to clarify their purpose to the user.

## 2024-05-24 - Alias Item Editor Tooltips
**Learning:** In complex configuration panels like AliasItemEditor, primary fields (Name, Group, Listen, Record, Priority) often lack inline explanations, increasing user error and cognitive load.
**Action:** Added clear, descriptive tooltips to primary configuration inputs to clarify their function and behavioral impact.
## 2024-10-24 - [Tooltip on Settings Spinner]
**Learning:** Configuration elements like Spinners that represent arbitrary numeric values without units are confusing.
**Action:** Always provide a descriptive Tooltip for Spinners to explain what the number represents and the expected unit (e.g., seconds).

## $(date +'%Y-%m-%d') - [Convert IntegerTextField to ComboBox for Talkgroups]
**Learning:** Manually typed text fields for configuration values known to the system, like Talkgroups, cause high cognitive load. Replacing `IntegerTextField` with an editable `ComboBox<Integer>` auto-populated from `AliasModel` drastically improves UX.
**Action:** Always scan for text input fields capturing references to known entities and convert them to populated, editable `ComboBox`es, rendering the entity ID alongside its human-readable alias via `ListCell` cell factory.

## 2024-05-13 - P25 Talkgroup Dropdown Conversion
**Learning:** Legacy JavaFX TextFields for Identifiers fail to provide any discovery for existing system configurations. Converting Talkgroup fields to ComboBox&lt;IdentifierValue&gt; populated by AliasList scanning allows rapid re-use of configurations, reducing redundant entry for operators tracking pre-existing Phase 2 talkgroups.
**Action:** When refactoring Identifier UIs, systematically replace text-based ID inputs with searchable ComboBox fields hooked to AliasList tracking. Ensure the ComboBox retains an editable StringConverter fallback so new IDs can still be added.
