package io.github.dsheirer.gui.playlist.alias.action;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.twotone.TwoToneAction;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class TwoToneActionEditor extends ActionEditor<TwoToneAction> {
    private ComboBox<TwoToneConfiguration> mDetectorCombo;
    private TwoToneAction mAction;
    private PlaylistManager mPlaylistManager;

    public TwoToneActionEditor(PlaylistManager playlistManager) {
        super();
        mPlaylistManager = playlistManager;
        init();
    }

    private void init() {
        mDetectorCombo = new ComboBox<>();
        mDetectorCombo.setPromptText("Select a Two-Tone Decoder...");

        if (mPlaylistManager != null && mPlaylistManager.getCurrentPlaylist() != null) {
            mDetectorCombo.getItems().addAll(mPlaylistManager.getCurrentPlaylist().getTwoToneConfigurations());
        }

        mDetectorCombo.setCellFactory(param -> new javafx.scene.control.ListCell<TwoToneConfiguration>() {
            @Override
            protected void updateItem(TwoToneConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAlias());
                }
            }
        });
        mDetectorCombo.setButtonCell(new javafx.scene.control.ListCell<TwoToneConfiguration>() {
            @Override
            protected void updateItem(TwoToneConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getAlias());
                }
            }
        });

        mDetectorCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (mAction != null && newVal != null) {
                mAction.setDetectorName(newVal.getAlias());
            } else if (mAction != null) {
                mAction.setDetectorName(null);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.add(new Label("Decoder:"), 0, 0);
        grid.add(mDetectorCombo, 1, 0);

        getChildren().add(grid);
    }

    @Override
    public void setItem(TwoToneAction action) {
        super.setItem(action);
        mAction = action;
        if (action != null && action.getDetectorName() != null && mDetectorCombo != null) {
            for (TwoToneConfiguration conf : mDetectorCombo.getItems()) {
                if (action.getDetectorName().equals(conf.getAlias())) {
                    mDetectorCombo.getSelectionModel().select(conf);
                    break;
                }
            }
        }
    }

    @Override
    public void save() {
        if (mAction == null) {
            mAction = new TwoToneAction();
        }
        TwoToneConfiguration conf = mDetectorCombo.getSelectionModel().getSelectedItem();
        if (conf != null) {
            mAction.setDetectorName(conf.getAlias());
        }
    }

    @Override
    public void dispose() {
        // No-op
    }
}
