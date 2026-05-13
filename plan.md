1. **Goal:** Upgrade `mTalkgroupField` in `P25FullyQualifiedTalkgroupEditor.java` to a `ComboBox<IdentifierValue>` and populate it from known alias talkgroups.
2. **Analysis:** The `P25FullyQualifiedTalkgroupEditor` currently takes `UserPreferences` and `PlaylistManager`. I can retrieve all aliases using `mPlaylistManager.getAliasModel().getAliases()`. I can iterate through all `Alias` items, look at their `getIdentities()`, check if they are `P25FullyQualifiedTalkgroup` instances, and if so, add their Talkgroup value and `Alias` name to the `mTalkgroupField` items as an `IdentifierValue`!
3. **Implementation details:**
    - Change `TextField mTalkgroupField` to `ComboBox<IdentifierValue> mTalkgroupField`.
    - Update `getTalkgroupField()` to return a `ComboBox<IdentifierValue>` that is editable, uses a `CellFactory` to show `HEX - Label`, and a `StringConverter` to allow user text entry (same as `getWacnField()` and `getSystemField()`).
    - Update `setItem(...)` to call `mTalkgroupField.setValue(null)` and `mTalkgroupField.getEditor().setText("")`.
    - Update `updateTextFormatter()` to set the formatter using `mTalkgroupField.getEditor().setTextFormatter(...)`.
    - Update `updateTextFormatter()` to set initial value:
        ```java
        if(getItem() != null) {
            mTalkgroupTextFormatter.setValue(getItem().getValue());
            mTalkgroupField.setValue(new IdentifierValue(getItem().getValue(), ""));
        } else {
            mTalkgroupTextFormatter.setValue(null);
            mTalkgroupField.setValue(null);
        }
        ```
    - Update event listeners:
        ```java
        mTalkgroupField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(getItem() != null && newValue != null && newValue.getValue() != null) {
                getItem().setValue(newValue.getValue());
                modifiedProperty().set(true);
            }
        });
        mTalkgroupTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
        ```
    - In `populateDropdowns()`, iterate through `mPlaylistManager.getAliasModel().getAliases()` to extract `P25FullyQualifiedTalkgroup` IDs and populate `mTalkgroupField`.
4. **Pre-commit:** Verify `./gradlew classes`.
