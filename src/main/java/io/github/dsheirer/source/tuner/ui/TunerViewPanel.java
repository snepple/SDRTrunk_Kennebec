package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.recording.AddRecordingTunerDialog;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import java.text.DecimalFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jiconfont.icons.font_awesome.FontAwesome;
import io.github.dsheirer.gui.VisibilityListener;
import javafx.geometry.Orientation;

public class TunerViewPanel extends VBox {
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(TunerViewPanel.class);
    private static final String TABLE_PREFERENCE_KEY = "tuner.view.panel";

    private UserPreferences mUserPreferences;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private DiscoveredTunerEditor mDiscoveredTunerEditor;
    private TunerConfigurationManager mTunerConfigurationManager;
    private TableView<DiscoveredTuner> mTunerTable;
    private SplitPane mSplitPane;
    private Button mAddRecordingButton;
    private Button mRemoveRecordingButton;
    private VisibilityListener mVisibilityListener;

    public TunerViewPanel(TunerManager tunerManager, UserPreferences userPreferences, VisibilityListener visibilityListener) {
        mVisibilityListener = visibilityListener;
        mDiscoveredTunerModel = tunerManager.getDiscoveredTunerModel();
        mDiscoveredTunerEditor = new DiscoveredTunerEditor(userPreferences, tunerManager);
        mTunerConfigurationManager = tunerManager.getTunerConfigurationManager();
        mUserPreferences = userPreferences;
        init();
    }

    @Subscribe
    public void process(USBAlertEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText(String.valueOf(event.getMessage()));
            alert.showAndWait();
        });
    }

    private void init() {
        io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().register(this);

        ToolBar toolBar = new ToolBar();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageBtn = new Button("Settings"); // Replace with icon logic if needed later
        manageBtn.setTooltip(new Tooltip("Settings"));
        manageBtn.setOnAction(e -> {
            ContextMenu popup = new ContextMenu();
            String specText = "Toggle Spectrum/Waterfall";
            if (mVisibilityListener != null) {
                specText = mVisibilityListener.isSpectrumVisible() ? "Hide Spectrum/Waterfall" : "Show Spectrum/Waterfall";
            }
            MenuItem specItem = new MenuItem(specText);
            specItem.setOnAction(evt -> {
                if(mVisibilityListener != null) mVisibilityListener.onToggleSpectrum();
            });
            popup.getItems().add(specItem);

            String resourceText = "Toggle Resource Status";
            if (mVisibilityListener != null) {
                resourceText = mVisibilityListener.isResourceVisible() ? "Hide Resource Status" : "Show Resource Status";
            }
            MenuItem resourceItem = new MenuItem(resourceText);
            resourceItem.setOnAction(evt -> {
                if(mVisibilityListener != null) mVisibilityListener.onToggleResource();
            });
            popup.getItems().add(resourceItem);

            popup.show(manageBtn, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        toolBar.getItems().addAll(spacer, manageBtn);
        getChildren().add(toolBar);

        mTunerTable = new TableView<>(mDiscoveredTunerModel.getObservableList());
        mTunerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<DiscoveredTuner, TunerStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTunerStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TunerStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item == TunerStatus.ERROR) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (item == TunerStatus.ENABLED) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<DiscoveredTuner, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<DiscoveredTuner, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> {
            DiscoveredTuner tuner = cellData.getValue();
            return new SimpleStringProperty(tuner.hasTuner() ? tuner.getTuner().getTunerType().getLabel() : "");
        });

        TableColumn<DiscoveredTuner, String> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> {
            DiscoveredTuner tuner = cellData.getValue();
            if (tuner.hasTuner()) {
                long freq = tuner.getTuner().getTunerController().getFrequency();
                return new SimpleStringProperty(new DecimalFormat("0.00000").format(freq / 1E6D) + " MHz");
            }
            return new SimpleStringProperty("");
        });

        TableColumn<DiscoveredTuner, String> channelsCol = new TableColumn<>("Channels");
        channelsCol.setCellValueFactory(cellData -> {
            DiscoveredTuner tuner = cellData.getValue();
            if (tuner.hasTuner()) {
                int count = tuner.getTuner().getChannelSourceManager().getTunerChannelCount();
                String lock = tuner.getTuner().getTunerController().isLockedSampleRate() ? "LOCKED" : "UNLOCKED";
                return new SimpleStringProperty(count + " (" + lock + ")");
            }
            return new SimpleStringProperty("");
        });

        TableColumn<DiscoveredTuner, String> stabilityCol = new TableColumn<>("Live Stability");
        stabilityCol.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(io.github.dsheirer.source.tuner.manager.AIFrequencyStabilizer.getInstance(mUserPreferences).getStabilityStatus());
        });

        mTunerTable.getColumns().addAll(statusCol, nameCol, typeCol, freqCol, channelsCol, stabilityCol);

        
        mTunerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            getRemoveRecordingButton().setDisable(false);
            if (newVal != null) {
                mDiscoveredTunerEditor.setItem(newVal);
                getRemoveRecordingButton().setDisable(!(newVal instanceof DiscoveredRecordingTuner));
            } else {
                mDiscoveredTunerEditor.setItem(null);
            }
        });

        ContextMenu popupMenu = new ContextMenu();
        MenuItem logStateMenuItem = new MenuItem("Log Tuner State");
        logStateMenuItem.setOnAction(e -> {
            DiscoveredTuner selected = mTunerTable.getSelectionModel().getSelectedItem();
            if(selected != null) {
                selected.logState();
            } else {
                mLog.error("Can't log state - tuner not selected");
            }
        });
        popupMenu.getItems().add(logStateMenuItem);
        mTunerTable.setContextMenu(popupMenu);

        mDiscoveredTunerModel.addListener(tunerEvent -> {
            switch(tunerEvent.getEvent()) {
                case UPDATE_LOCK_STATE:
                    if(tunerEvent.getTuner() != null) {
                        DiscoveredTuner selectedTuner = mTunerTable.getSelectionModel().getSelectedItem();
                        if(selectedTuner != null && selectedTuner.hasTuner() && tunerEvent.getTuner() == selectedTuner.getTuner()) {
                            mDiscoveredTunerEditor.setTunerLockState(selectedTuner.getTuner().getTunerController().isLockedSampleRate());
                        }
                    }
                    break;
            }
        });

        VBox tunerTablePanel = new VBox();
        VBox.setVgrow(mTunerTable, Priority.ALWAYS);
        tunerTablePanel.getChildren().add(mTunerTable);

        HBox buttonPanel = new HBox(5);
        buttonPanel.getChildren().addAll(getAddRecordingButton(), getRemoveRecordingButton());
        tunerTablePanel.getChildren().add(buttonPanel);

        ScrollPane editorScroller = new ScrollPane(mDiscoveredTunerEditor);
        editorScroller.setFitToWidth(true);

        mSplitPane = new SplitPane();
        mSplitPane.setMinWidth(0);
        mTunerTable.setMinWidth(0);
        mSplitPane.setOrientation(Orientation.HORIZONTAL);
        mSplitPane.getItems().addAll(tunerTablePanel, editorScroller);
        mSplitPane.setDividerPositions(0.5);

        VBox.setVgrow(mSplitPane, Priority.ALWAYS);
        getChildren().add(mSplitPane);
    }

    private Button getAddRecordingButton() {
        if(mAddRecordingButton == null) {
            mAddRecordingButton = new Button("Add Recording Tuner");
            mAddRecordingButton.setTooltip(new Tooltip("Add a new recording tuner to the workspace"));
            mAddRecordingButton.setOnAction(e -> {
                AddRecordingTunerDialog dialog = new AddRecordingTunerDialog(mUserPreferences, mDiscoveredTunerModel, mTunerConfigurationManager);
                // Swing dialog adaptation might be needed if dialog isn't JavaFX yet, but keeping logic
                Platform.runLater(() -> dialog.setVisible(true));
            });
        }
        return mAddRecordingButton;
    }

    private Button getRemoveRecordingButton() {
        if(mRemoveRecordingButton == null) {
            mRemoveRecordingButton = new Button("Remove Recording Tuner");
            mRemoveRecordingButton.setTooltip(new Tooltip("Permanently remove the selected recording tuner. This action cannot be undone."));
            mRemoveRecordingButton.setDisable(false);
            mRemoveRecordingButton.setOnAction(e -> {
                DiscoveredTuner selected = mTunerTable.getSelectionModel().getSelectedItem();
                if(selected instanceof DiscoveredRecordingTuner) {
                    DiscoveredRecordingTuner discoveredRecordingTuner = (DiscoveredRecordingTuner) selected;
                    mLog.info("Removing Tuner: " + discoveredRecordingTuner);
                    discoveredRecordingTuner.stop();
                    mTunerConfigurationManager.removeTunerConfiguration(discoveredRecordingTuner.getTunerConfiguration());
                    Platform.runLater(() -> mDiscoveredTunerModel.removeDiscoveredTuner(discoveredRecordingTuner));
                }
            });
        }
        return mRemoveRecordingButton;
    }
}
