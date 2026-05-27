1. **Analyze Requirements and Context**: As the "Ergo" persona, the goal is to implement a micro-UX improvement that reduces cognitive load, ideally under 100 lines. The current focus is on converting a text field to a smart dropdown based on existing aliases, specifically the `RadioIdEditor` which historically forced manual entry.

2. **Update `RadioIdEditor.java`**:
   - Change `mRadioIdField` from `TextField` to `ComboBox<Integer>`.
   - Update the constructor to accept a `PlaylistManager` reference (needed to get the `AliasModel`).
   - Populate the `ComboBox` with existing `Radio` IDs from aliases that match the current protocol.
   - Use a custom `CellFactory` to display the ID along with its associated alias name (if one exists).

3. **Update `IdentifierEditorFactory.java`**:
   - Change the instantiation of `RadioIdEditor` to pass in `playlistManager`: `new RadioIdEditor(userPreferences, playlistManager)`.

4. **Verify Compilation and Functionality**:
   - Run `./gradlew classes` to ensure the changes compile cleanly.

5. **Update `.Jules/ergo.md` and bump version**:
   - Write a journal entry recording this learning (as required by Ergo persona)
   - Read and append correctly.
   - Bump the version in `gradle.properties` (using `grep -i "projectversion"` to read safely, and `sed` to bump).

6. **Complete pre-commit steps**:
   - Call `pre_commit_instructions` and follow them to ensure proper testing, verification, review, and reflection are done.

7. **Submit the Pull Request**:
   - Use the `submit` tool with title "📐 Ergo: [UX/Config improvement]" and required description components ("What", "Why", "Before/After", "Cognitive Load").
