1. **Remove structural border from Channel Configuration editor**:
   - Use `run_in_bash_session` to edit `src/main/java/io/github/dsheirer/gui/playlist/channel/ChannelConfigurationEditor.java`.
   - Modify the constructor to add `getStyleClass().remove("preferences-card");` and/or `inspectorCard.getStyleClass().remove("preferences-card");` to explicitly remove the CSS class that causes the border. Alternatively, I will ensure the border is removed.
2. **Verify the change**:
   - Use `run_in_bash_session` to execute `cat` or `grep` on `ChannelConfigurationEditor.java` to confirm the class removal logic is correctly added.
3. **Complete pre-commit steps**:
   - Complete pre commit steps to make sure proper testing, verifications, reviews and reflections are done.
4. **Run tests**:
   - Use `run_in_bash_session` to execute `./gradlew test --tests "io.github.dsheirer.*"`.
