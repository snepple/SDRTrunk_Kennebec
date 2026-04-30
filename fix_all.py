import re

file_path = "./src/main/java/io/github/dsheirer/gui/playlist/manager/PlaylistManagerEditor.java"

with open(file_path, 'r') as f:
    content = f.read()

# 1. Update imports
content = re.sub(r'import javafx\.scene\.layout\.HBox;\n', '', content)
content = re.sub(r'import javafx\.scene\.control\.TableCell;\n', '', content)
content = re.sub(r'import javafx\.scene\.control\.TableColumn;\n', '', content)
content = re.sub(r'import javafx\.scene\.control\.TableView;\n', '', content)

new_imports = """import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
"""
content = content.replace("import javafx.scene.control.Tooltip;\n", "import javafx.scene.control.Tooltip;\n" + new_imports)

# 2. Change superclass
content = content.replace("public class PlaylistManagerEditor extends HBox", "public class PlaylistManagerEditor extends BorderPane")

# 3. Replace TableView with ListView field
content = content.replace("private TableView<Path> mPlaylistTableView;", "private ListView<Path> mPlaylistListView;")

# 4. Remove VBox field
content = content.replace("private VBox mButtonBox;", "")

# 5. Update Constructor
start_idx = content.find("public PlaylistManagerEditor(PlaylistManager playlistManager, UserPreferences userPreferences)")
end_idx = content.find("public void dispose()", start_idx)
new_constructor = """public PlaylistManagerEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;

        //Register to receive preferences updates
        MyEventBus.getGlobalEventBus().register(this);

        setPadding(new Insets(16));
        setCenter(getPlaylistListView());
        setBottom(getActionBar());
        updateButtons();
    }

    """
content = content[:start_idx] + new_constructor + content[end_idx:]

# 6. Update preferenceUpdated
content = content.replace("getPlaylistTableView().refresh();", "getPlaylistListView().refresh();")

# 7. Replace getter calls
content = content.replace("getPlaylistTableView()", "getPlaylistListView()")

# 8. Refactor getPlaylistListView implementation
start_idx = content.find("private TableView<Path> getPlaylistListView()")
if start_idx == -1: # if we replaced getter call before
    start_idx = content.find("private ListView<Path> getPlaylistListView()")
end_idx = content.find("private void updateButtons()")

new_listview = """private ListView<Path> getPlaylistListView()
    {
        if(mPlaylistListView == null)
        {
            mPlaylistListView = new ListView<>();
            mPlaylistListView.setStyle("-fx-background-insets: 0; -fx-padding: 0;");

            mPlaylistListView.setCellFactory(new Callback<ListView<Path>, ListCell<Path>>() {
                @Override
                public ListCell<Path> call(ListView<Path> param) {
                    return new ListCell<Path>() {
                        private HBox root = new HBox(12);
                        private VBox textContainer = new VBox(2);
                        private Label titleLabel = new Label();
                        private Label pathLabel = new Label();
                        private IconNode statusIcon = new IconNode(FontAwesome.CHECK);

                        {
                            titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                            pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8E8E93;");
                            statusIcon.setIconSize(16);

                            root.setAlignment(Pos.CENTER_LEFT);
                            root.setPadding(new Insets(8, 12, 8, 12));

                            textContainer.getChildren().addAll(titleLabel, pathLabel);
                            root.getChildren().addAll(statusIcon, textContainer);
                        }

                        @Override
                        protected void updateItem(Path item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                            } else {
                                titleLabel.setText(item.getFileName().toString());
                                pathLabel.setText(item.getParent() != null ? item.getParent().toString() : "");

                                if (isCurrent(item)) {
                                    statusIcon.setIconCode(FontAwesome.CHECK);
                                    statusIcon.setFill(Color.web("#34C759"));
                                    statusIcon.setVisible(true);
                                } else if (!item.toFile().exists()) {
                                    statusIcon.setIconCode(FontAwesome.TIMES);
                                    statusIcon.setFill(Color.web("#FF3B30"));
                                    statusIcon.setVisible(true);
                                } else {
                                    statusIcon.setVisible(false);
                                }

                                setGraphic(root);
                            }
                        }
                    };
                }
            });

            mPlaylistListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateButtons());

            //User double-clicks on an entry - make that entry the selected playlist.
            mPlaylistListView.setOnMouseClicked(event ->
            {
                if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2)
                {
                    selectPlayist(getPlaylistListView().getSelectionModel().getSelectedItem());
                }
            });

            List<Path> playlistPaths = mUserPreferences.getPlaylistPreference().getPlaylistList();

            mPlaylistListView.getItems().addAll(playlistPaths);
        }

        return mPlaylistListView;
    }

    """
content = content[:start_idx] + new_listview + content[end_idx:]

# 9. Replace VBox getButtonBox with HBox getActionBar
start_idx = content.find("private VBox getButtonBox()")
end_idx = content.find("/**\n     * Selects the specified playlist")

new_actionbar = """private HBox getActionBar()
    {
        HBox actionBar = new HBox(12);
        actionBar.setPadding(new Insets(12, 0, 0, 0));
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionBar.getChildren().addAll(
            getSelectButton(),
            spacer,
            getNewButton(),
            getAddButton(),
            getCloneButton(),
            getRemoveButton(),
            getDeleteButton()
        );

        return actionBar;
    }

    """

content = content[:start_idx] + new_actionbar + content[end_idx:]

with open(file_path, 'w') as f:
    f.write(content)
