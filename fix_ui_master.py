import re

with open('src/main/java/io/github/dsheirer/gui/playlist/twotone/TwoToneEditor.java', 'r') as f:
    content = f.read()

# Master had some updates that were removed (like "Two Tone Paging Detectors" label and syncToPlaylist() method that was probably needed).
# Let's restore the important parts of the master's diff manually.

new_content = content.replace("getChildren().addAll(mTableView, editorGrid, btnBox);", "getChildren().addAll(new Label(\"Two Tone Paging Detectors\"), mTableView, editorGrid, btnBox);")

new_content = new_content.replace("""        Button btnAdd = new Button("Add Tone");
        btnAdd.setOnAction(evt -> {
            TwoToneConfiguration c = new TwoToneConfiguration();
            c.setAlias("New");
            mObservableConfigs.add(c);
            playlistManager.getCurrentPlaylist().getTwoToneConfigurations().add(c);
        });
        Button btnDel = new Button("Delete");
        btnDel.setOnAction(evt -> {
            TwoToneConfiguration c = mTableView.getSelectionModel().getSelectedItem();
            if (c != null) {
                mObservableConfigs.remove(c);
                playlistManager.getCurrentPlaylist().getTwoToneConfigurations().remove(c);
            }
        });""", """        Button btnAdd = new Button("Add");
        btnAdd.setOnAction(evt -> {
            TwoToneConfiguration c = new TwoToneConfiguration();
            c.setAlias("New Detector");
            mObservableConfigs.add(c);
            syncToPlaylist();
            mTableView.getSelectionModel().select(c);
        });
        Button btnDel = new Button("Delete");
        btnDel.setOnAction(evt -> {
            TwoToneConfiguration c = mTableView.getSelectionModel().getSelectedItem();
            if (c != null) {
                mObservableConfigs.remove(c);
                syncToPlaylist();
            }
        });""")

if "syncToPlaylist" not in new_content:
    new_content = new_content.replace("""    public void process(TwoToneTabRequest request)
    {
    }""", """    private void syncToPlaylist() {
        if (mPlaylistManager.getCurrentPlaylist() != null) {
            mPlaylistManager.getCurrentPlaylist().setTwoToneConfigurations(mObservableConfigs);
            // PlaylistManager will listen to this change normally or we trigger a save if needed
        }
    }

    public void process(TwoToneTabRequest request)
    {
    }""")

with open('src/main/java/io/github/dsheirer/gui/playlist/twotone/TwoToneEditor.java', 'w') as f:
    f.write(new_content)
