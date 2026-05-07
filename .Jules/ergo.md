## 2024-05-06 - P25P2 Configuration UX Enhancement **Learning:** P25 Phase 2 configuration currently lacks help tooltips with FontAwesome icons like Phase 1, and the toggle buttons don't have descriptions attached to help icons. **Action:** We'll add the `createHelpIcon` method and help icons to P25 Phase 2 configuration.
## 2024-05-18 - Notification Test Actions **Learning:** Added inline test buttons for Telegram and SMTP. **Action:** Next time consider moving network calls to background tasks as implemented.
## 2026-05-07 - SettingsCard and Tooltips for Complex Configs
**Learning:** Using SettingsCard with SettingsRow combined with tooltips and prompt texts reduces cognitive load when configuring advanced features like MQTT.
**Action:** Apply this pattern to other legacy JavaFX GridPane layouts to align with HIG.
