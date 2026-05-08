/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelAlertConfiguration;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.SplitPane;
import java.util.LinkedHashMap;
import javafx.geometry.Orientation;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel configuration editor
 */
public abstract class ChannelConfigurationEditor extends Editor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelConfigurationEditor.class);

    private PlaylistManager mPlaylistManager;
    private Label mChannelNameLabel;
    protected TunerManager mTunerManager;
    protected UserPreferences mUserPreferences;
    protected EditorModificationListener mEditorModificationListener = new EditorModificationListener();
    private Button mPlayButton;
    private TextField mSystemField;
    private TextField mSiteField;
    private TextField mNameField;
    private ComboBox<String> mAliasListComboBox;
    private Button mNewAliasListButton;
    private GridPane mTextFieldPane;
    private VBox mAlertsPane;
    private ToggleSwitch mInactivityAlertEnabledSwitch;
    private Spinner<Integer> mInactivityDurationSpinner;
    private ToggleSwitch mAiAudioMonitoringEnabledSwitch;
    private Spinner<Integer> mAiAudioMonitoringCheckIntervalSpinner;
    private ToggleSwitch mAiAudioMonitoringWaitNewAudioSwitch;
    private Spinner<Integer> mAiAudioMonitoringAlertThresholdSpinner;
    private Button mSaveButton;
    private Button mResetButton;
    private VBox mButtonBox;
    private SplitPane mSplitPane;
    private ListView<String> mSidebarList;
    private StackPane mContentPane;
    private java.util.Map<String, javafx.scene.Node> mConfigurationPanes = new LinkedHashMap<>();

    private ToggleSwitch mAutoStartSwitch;
    private Spinner<Integer> mAutoStartOrderSpinner;
    private IconNode mPlayGraphicNode;
    private IconNode mStopGraphicNode;
    private ChannelProcessingMonitor mChannelProcessingMonitor = new ChannelProcessingMonitor();
    private IFilterProcessor mFilterProcessor;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public ChannelConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                      UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mUserPreferences = userPreferences;
        mFilterProcessor = filterProcessor;

        setMaxWidth(Double.MAX_VALUE);

        VBox inspectorCard = new VBox(15);
        this.getStyleClass().remove("preferences-card");
        inspectorCard.getStyleClass().remove("preferences-card");
        inspectorCard.setPadding(new Insets(8));
        inspectorCard.setMaxWidth(Double.MAX_VALUE);

        Label headerLabel = new Label("Channel Configuration");
        headerLabel.getStyleClass().add("preferences-section-header");

        mChannelNameLabel = new Label("");
        mChannelNameLabel.getStyleClass().add("preferences-section-header");
        // Add padding to the left so it has some spacing if we just bind text
        mChannelNameLabel.setPadding(new Insets(0, 0, 0, 5));

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label autoStartOrderLabel = new Label("Order:");
        autoStartOrderLabel.setPadding(new Insets(0, 0, 0, 10)); // Add some space before

        actionBox.getChildren().addAll(getAutoStartSwitch(), autoStartOrderLabel, getAutoStartOrderSpinner(), getPlayButton(), getResetButton(), getSaveButton());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerBox = new HBox(headerLabel, mChannelNameLabel, spacer, actionBox);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox.setVgrow(getSplitPane(), Priority.ALWAYS);
        inspectorCard.getChildren().addAll(headerBox, getSplitPane());


        // Setup General Pane
        javafx.scene.control.ScrollPane generalScroll = new javafx.scene.control.ScrollPane(getTextFieldPane());
        generalScroll.setFitToWidth(true);
        generalScroll.setFitToHeight(true);
        addConfigurationPane("General", generalScroll);

        getChildren().add(inspectorCard);
    }

    /**
     * Setup the Alerts pane. Called by subclasses after their specific panes are added
     * so that the Alerts pane appears at the bottom of the list.
     */
    protected void setupAlertsPane()
    {
        // Setup Alerts Pane
        javafx.scene.control.ScrollPane alertsScroll = new javafx.scene.control.ScrollPane(getAlertsPane());
        alertsScroll.setFitToWidth(true);
        alertsScroll.setFitToHeight(true);
        addConfigurationPane("Alerts", alertsScroll);
    }

    /**
     * Provides subclass access to the playlist manager and related components
     */
    protected PlaylistManager getPlaylistManager()
    {
        return mPlaylistManager;
    }

    @Override
    public void dispose()
    {
    }

    /**
     * Starts the current channel when the play button is not disabled.
     */
    public void startChannel()
    {
        if(!getPlayButton().disabledProperty().get())
        {
            getPlayButton().fire();
        }
    }

    public abstract DecoderType getDecoderType();

    @Override
    public void setItem(Channel channel)
    {
        if(getItem() != null)
        {
            getItem().processingProperty().removeListener(mChannelProcessingMonitor);
            mChannelNameLabel.textProperty().unbind();
            getAutoStartSwitch().selectedProperty().unbindBidirectional(getItem().autoStartProperty());
        }

        super.setItem(channel);

        if(getItem() != null)
        {
            setPlayButtonState(getItem().processingProperty().get());
            getItem().processingProperty().addListener(mChannelProcessingMonitor);
        }

        boolean disable = (channel == null);

        getPlayButton().setDisable(disable);
        getSystemField().setDisable(disable);
        getSiteField().setDisable(disable);
        getNameField().setDisable(disable);
        getAliasListComboBox().setDisable(disable);
        getNewAliasListButton().setDisable(disable);
        getAutoStartSwitch().setDisable(disable);

        if(channel != null)
        {
                        mChannelNameLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
                StringBuilder sb = new StringBuilder(" - ");
                if (channel.getSystem() != null && !channel.getSystem().isEmpty()) {
                    sb.append(channel.getSystem()).append(" - ");
                }
                if (channel.getSite() != null && !channel.getSite().isEmpty()) {
                    sb.append(channel.getSite()).append(" - ");
                }
                if (channel.getName() != null && !channel.getName().isEmpty()) {
                    sb.append(channel.getName());
                } else {
                    if (sb.length() > 3) {
                        sb.setLength(sb.length() - 3); // Remove trailing " - "
                    }
                }
                if (sb.length() == 3) {
                    return ""; // Just " - "
                }
                return sb.toString();
            }, channel.systemProperty(), channel.siteProperty(), channel.nameProperty()));

            getSystemField().setText(channel.getSystem());
            getSiteField().setText(channel.getSite());
            getNameField().setText(channel.getName());
            String aliasListName = channel.getAliasListName();

            if(aliasListName != null)
            {
                if(!getAliasListComboBox().getItems().contains(aliasListName))
                {
                    mPlaylistManager.getAliasModel().addAliasList(aliasListName);
                }

                getAliasListComboBox().getSelectionModel().select(aliasListName);
            }
            else
            {
                getAliasListComboBox().getSelectionModel().select(null);
            }

            getAutoStartSwitch().selectedProperty().bindBidirectional(channel.autoStartProperty());
            getAutoStartOrderSpinner().setDisable(!channel.isAutoStart());
            Integer order = channel.getAutoStartOrder();
            getAutoStartOrderSpinner().getValueFactory().setValue(order != null ? order : 0);

            setDecoderConfiguration(channel.getDecodeConfiguration());

            SourceConfiguration sourceConfiguration = channel.getSourceConfiguration();
            if(sourceConfiguration == null)
            {
                sourceConfiguration = new SourceConfigTuner();
            }
            setSourceConfiguration(sourceConfiguration);

            AuxDecodeConfiguration auxDecodeConfiguration = channel.getAuxDecodeConfiguration();
            if(auxDecodeConfiguration == null)
            {
                auxDecodeConfiguration = new AuxDecodeConfiguration();
            }
            setAuxDecoderConfiguration(auxDecodeConfiguration);

            EventLogConfiguration eventLogConfiguration = channel.getEventLogConfiguration();
            if(eventLogConfiguration == null)
            {
                eventLogConfiguration = new EventLogConfiguration();
            }
            setEventLogConfiguration(eventLogConfiguration);

            RecordConfiguration recordConfiguration = channel.getRecordConfiguration();
            if(recordConfiguration == null)
            {
                recordConfiguration = new RecordConfiguration();
            }
            setRecordConfiguration(recordConfiguration);
            ChannelAlertConfiguration alertConfiguration = channel.getAlertConfiguration();
            if(alertConfiguration == null) {
                alertConfiguration = new ChannelAlertConfiguration();
                channel.setAlertConfiguration(alertConfiguration);
            }
            setAlertConfiguration(alertConfiguration);
        }
        else
        {
            mChannelNameLabel.setText("");
            getSystemField().setText(null);
            getSiteField().setText(null);
            getNameField().setText(null);
            getAliasListComboBox().getSelectionModel().select(null);
            getAutoStartSwitch().selectedProperty().set(false);
            getAutoStartOrderSpinner().setDisable(true);
            getAutoStartOrderSpinner().getValueFactory().setValue(0);

            setDecoderConfiguration(null);
            setAuxDecoderConfiguration(null);
            setEventLogConfiguration(null);
            setRecordConfiguration(null);
            setSourceConfiguration(null);
            setAlertConfiguration(null);
        }

        modifiedProperty().setValue(false);
    }

    @Override
    public void save()
    {
        if(modifiedProperty().get())
        {
            getItem().setSystem(getSystemField().getText());
            getItem().setSite(getSiteField().getText());

            //Hack - change the name to something else and then set it to the real value to trigger change events
            getItem().setName(" ");
            getItem().setName(getNameField().getText());
            getItem().setAliasListName(getAliasListComboBox().getSelectionModel().getSelectedItem());

            Integer order = getAutoStartOrderSpinner().getValue();

            if(order == null || order < 1)
            {
                getItem().setAutoStartOrder(null);
            }
            else
            {
                getItem().setAutoStartOrder(getAutoStartOrderSpinner().getValue());
            }

            saveDecoderConfiguration();
            saveAuxDecoderConfiguration();
            saveEventLogConfiguration();
            saveRecordConfiguration();
            saveSourceConfiguration();
            saveAlertConfiguration();

            modifiedProperty().set(false);
        }
    }


    protected void setAlertConfiguration(ChannelAlertConfiguration config) {
        if(config != null) {
            getInactivityAlertEnabledSwitch().setDisable(false);
            getInactivityAlertEnabledSwitch().setSelected(config.isInactivityAlertEnabled());

            getInactivityDurationSpinner().setDisable(!config.isInactivityAlertEnabled());
            getInactivityDurationSpinner().getValueFactory().setValue(config.getInactivityDurationThresholdMinutes());

            getAiAudioMonitoringEnabledSwitch().setDisable(false);
            getAiAudioMonitoringEnabledSwitch().setSelected(config.isAiAudioMonitoringEnabled());

            getAiAudioMonitoringCheckIntervalSpinner().setDisable(!config.isAiAudioMonitoringEnabled());
            getAiAudioMonitoringCheckIntervalSpinner().getValueFactory().setValue(config.getAiAudioMonitoringCheckInterval());

            getAiAudioMonitoringWaitNewAudioSwitch().setDisable(!config.isAiAudioMonitoringEnabled());
            getAiAudioMonitoringWaitNewAudioSwitch().setSelected(config.isAiAudioMonitoringWaitNewAudio());

            getAiAudioMonitoringAlertThresholdSpinner().setDisable(!config.isAiAudioMonitoringEnabled());
            getAiAudioMonitoringAlertThresholdSpinner().getValueFactory().setValue(config.getAiAudioMonitoringAlertThreshold());
        } else {
            getInactivityAlertEnabledSwitch().setDisable(true);
            getInactivityAlertEnabledSwitch().setSelected(false);
            getInactivityDurationSpinner().setDisable(true);

            getAiAudioMonitoringEnabledSwitch().setDisable(true);
            getAiAudioMonitoringEnabledSwitch().setSelected(false);
            getAiAudioMonitoringCheckIntervalSpinner().setDisable(true);
            getAiAudioMonitoringWaitNewAudioSwitch().setDisable(true);
            getAiAudioMonitoringWaitNewAudioSwitch().setSelected(false);
            getAiAudioMonitoringAlertThresholdSpinner().setDisable(true);
        }
    }

    protected void saveAlertConfiguration() {
        if(getItem() != null) {
            ChannelAlertConfiguration config = getItem().getAlertConfiguration();
            if(config == null) {
                config = new ChannelAlertConfiguration();
                getItem().setAlertConfiguration(config);
            }
            config.setInactivityAlertEnabled(getInactivityAlertEnabledSwitch().isSelected());
            config.setInactivityDurationThresholdMinutes(getInactivityDurationSpinner().getValue());
            config.setAiAudioMonitoringEnabled(getAiAudioMonitoringEnabledSwitch().isSelected());
            config.setAiAudioMonitoringCheckInterval(getAiAudioMonitoringCheckIntervalSpinner().getValue());
            config.setAiAudioMonitoringWaitNewAudio(getAiAudioMonitoringWaitNewAudioSwitch().isSelected());
            config.setAiAudioMonitoringAlertThreshold(getAiAudioMonitoringAlertThresholdSpinner().getValue());
        }
    }

    private ToggleSwitch getInactivityAlertEnabledSwitch() {
        if(mInactivityAlertEnabledSwitch == null) {
            mInactivityAlertEnabledSwitch = new ToggleSwitch("Enable Alert");
            mInactivityAlertEnabledSwitch.selectedProperty().addListener((obs, old, newValue) -> {
                modifiedProperty().set(true);
                getInactivityDurationSpinner().setDisable(!newValue);
            });
        }
        return mInactivityAlertEnabledSwitch;
    }

    private Spinner<Integer> getInactivityDurationSpinner() {
        if(mInactivityDurationSpinner == null) {
            mInactivityDurationSpinner = new Spinner<>();
            mInactivityDurationSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1440, 10));
            mInactivityDurationSpinner.setEditable(true);
            mInactivityDurationSpinner.valueProperty().addListener((obs, old, newValue) -> modifiedProperty().set(true));
        }
        return mInactivityDurationSpinner;
    }

    private ToggleSwitch getAiAudioMonitoringEnabledSwitch() {
        if(mAiAudioMonitoringEnabledSwitch == null) {
            mAiAudioMonitoringEnabledSwitch = new ToggleSwitch("Enable Monitoring");
            mAiAudioMonitoringEnabledSwitch.selectedProperty().addListener((obs, old, newValue) -> {
                modifiedProperty().set(true);
                getAiAudioMonitoringCheckIntervalSpinner().setDisable(!newValue);
                getAiAudioMonitoringWaitNewAudioSwitch().setDisable(!newValue);
                getAiAudioMonitoringAlertThresholdSpinner().setDisable(!newValue);
            });
        }
        return mAiAudioMonitoringEnabledSwitch;
    }

    private Spinner<Integer> getAiAudioMonitoringCheckIntervalSpinner() {
        if(mAiAudioMonitoringCheckIntervalSpinner == null) {
            mAiAudioMonitoringCheckIntervalSpinner = new Spinner<>();
            mAiAudioMonitoringCheckIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1));
            mAiAudioMonitoringCheckIntervalSpinner.setEditable(true);
            mAiAudioMonitoringCheckIntervalSpinner.valueProperty().addListener((obs, old, newValue) -> modifiedProperty().set(true));
        }
        return mAiAudioMonitoringCheckIntervalSpinner;
    }

    private ToggleSwitch getAiAudioMonitoringWaitNewAudioSwitch() {
        if(mAiAudioMonitoringWaitNewAudioSwitch == null) {
            mAiAudioMonitoringWaitNewAudioSwitch = new ToggleSwitch("Wait for New Audio on Failure");
            mAiAudioMonitoringWaitNewAudioSwitch.selectedProperty().addListener((obs, old, newValue) -> modifiedProperty().set(true));
        }
        return mAiAudioMonitoringWaitNewAudioSwitch;
    }

    private Spinner<Integer> getAiAudioMonitoringAlertThresholdSpinner() {
        if(mAiAudioMonitoringAlertThresholdSpinner == null) {
            mAiAudioMonitoringAlertThresholdSpinner = new Spinner<>();
            mAiAudioMonitoringAlertThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 3));
            mAiAudioMonitoringAlertThresholdSpinner.setEditable(true);
            mAiAudioMonitoringAlertThresholdSpinner.valueProperty().addListener((obs, old, newValue) -> modifiedProperty().set(true));
        }
        return mAiAudioMonitoringAlertThresholdSpinner;
    }

    private VBox getAlertsPane() {
        if(mAlertsPane == null) {
            mAlertsPane = new VBox(20);
            mAlertsPane.setPadding(new Insets(10));

            // Inactivity Alerts Section
            VBox inactivitySection = new VBox(10);
            Label inactivityHeader = new Label("Channel Inactivity Alerts");
            inactivityHeader.setStyle("-fx-font-weight: bold;");

            GridPane inactivityGrid = new GridPane();
            inactivityGrid.setHgap(10);
            inactivityGrid.setVgap(10);
            inactivityGrid.add(getInactivityAlertEnabledSwitch(), 0, 0, 2, 1);
            inactivityGrid.add(new Label("Duration Threshold (minutes):"), 0, 1);
            inactivityGrid.add(getInactivityDurationSpinner(), 1, 1);

            inactivitySection.getChildren().addAll(inactivityHeader, inactivityGrid);

            // AI Audio Monitoring Section
            VBox aiSection = new VBox(10);
            Label aiHeader = new Label("AI Audio Monitoring");
            aiHeader.setStyle("-fx-font-weight: bold;");

            GridPane aiGrid = new GridPane();
            aiGrid.setHgap(10);
            aiGrid.setVgap(10);
            aiGrid.add(getAiAudioMonitoringEnabledSwitch(), 0, 0, 2, 1);
            aiGrid.add(new Label("Check Interval (hours):"), 0, 1);
            aiGrid.add(getAiAudioMonitoringCheckIntervalSpinner(), 1, 1);
            aiGrid.add(getAiAudioMonitoringWaitNewAudioSwitch(), 0, 2, 2, 1);
            aiGrid.add(new Label("Alert Threshold (failures):"), 0, 3);
            aiGrid.add(getAiAudioMonitoringAlertThresholdSpinner(), 1, 3);

            aiSection.getChildren().addAll(aiHeader, aiGrid);

            mAlertsPane.getChildren().addAll(inactivitySection, aiSection);
        }
        return mAlertsPane;
    }

    protected abstract void setAuxDecoderConfiguration(AuxDecodeConfiguration config);
    protected abstract void saveAuxDecoderConfiguration();
    protected abstract void setDecoderConfiguration(DecodeConfiguration config);
    protected abstract void saveDecoderConfiguration();
    protected abstract void setEventLogConfiguration(EventLogConfiguration config);
    protected abstract void saveEventLogConfiguration();
    protected abstract void setRecordConfiguration(RecordConfiguration config);
    protected abstract void saveRecordConfiguration();
    protected abstract void setSourceConfiguration(SourceConfiguration config);
    protected abstract void saveSourceConfiguration();

    private Button getPlayButton()
    {
        if(mPlayButton == null)
        {
            mPlayGraphicNode = new IconNode(FontAwesome.PLAY);
            mPlayGraphicNode.setFill(Color.GREEN);
            mPlayGraphicNode.setIconSize(24);

            mStopGraphicNode = new IconNode(FontAwesome.STOP);
            mStopGraphicNode.setFill(Color.RED);
            mStopGraphicNode.setIconSize(24);

            mPlayButton = new Button("Play");
            mPlayButton.setMaxWidth(Double.MAX_VALUE);
            mPlayButton.setMaxHeight(Double.MAX_VALUE);
            mPlayButton.setDisable(true);
            mPlayButton.setOnAction((ActionEvent event) -> {
                if(getItem() != null)
                {
                    if(modifiedProperty().get())
                    {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Do you want to save these changes?", ButtonType.YES, ButtonType.NO);
                        alert.setTitle("Channel Configuration Modified");
                        alert.setHeaderText("Channel configuration has unsaved changes");
                        alert.initOwner((getPlayButton()).getScene().getWindow());
                        alert.showAndWait().ifPresent(buttonType -> {
                            if(buttonType == ButtonType.YES)
                            {
                                save();
                            }
                        });
                    }

                    if(requiresJmbeLibrarySetup() &&
                       mUserPreferences.getJmbeLibraryPreference().getAlertIfMissingLibraryRequired() &&
                       !getItem().processingProperty().get())
                    {
                        String content = "The decoder for this channel configuration requires the (optional) JMBE " +
                            "library to produce audio and the JMBE library is not currently setup.  Do you want to " +
                            "setup the JMBE library?";

                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
                        alert.setTitle("JMBE Library");
                        alert.setHeaderText("Setup JMBE Library?");

                        Label label = new Label(content);
                        label.setMaxWidth(Double.MAX_VALUE);
                        label.setMaxHeight(Double.MAX_VALUE);
                        label.getStyleClass().add("content");
                        label.setWrapText(true);

                        ToggleSwitch toggleSwitch = new ToggleSwitch("Don't Show This Again");
                        toggleSwitch.selectedProperty().addListener((observable2, oldValue2, newValue2) -> {
                            boolean dontShowAgain = newValue2;
                            mUserPreferences.getJmbeLibraryPreference().setAlertIfMissingLibraryRequired(!dontShowAgain);
                        });

                        VBox contentBox = new VBox();
                        contentBox.setPrefWidth(360);
                        contentBox.setSpacing(10);
                        contentBox.getChildren().addAll(label, toggleSwitch);
                        alert.getDialogPane().setContent(contentBox);
                        alert.initOwner(getPlayButton().getScene().getWindow());
                        Optional<ButtonType> optionalButtonType = alert.showAndWait();

                        if(optionalButtonType.isPresent() && optionalButtonType.get() == ButtonType.YES)
                        {
                            MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.JMBE_LIBRARY));
                            return;
                        }
                    }

                    if(!getItem().processingProperty().get())
                    {
                        ThreadPool.CACHED.execute(() -> {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().start(getItem());
                            }
                            catch(ChannelException ce)
                            {
                                mLog.error("Error starting channel [" + getItem().getName() + "] - " + ce.getMessage());

                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ce.getMessage(), ButtonType.OK);
                                    alert.setTitle("Channel Play Error");
                                    alert.setHeaderText("Can't play channel");
                                    alert.initOwner((getPlayButton()).getScene().getWindow());
                                    alert.showAndWait();
                                });
                            }
                        });
                    }
                    else
                    {
                        ThreadPool.CACHED.execute(() -> {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().stop(getItem());
                            }
                            catch(ChannelException ce)
                            {
                                mLog.error("Error stopping channel [" + getItem().getName() + "] - " + ce.getMessage());

                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + ce.getMessage(), ButtonType.OK);
                                    alert.setTitle("Channel Stop Error");
                                    alert.setHeaderText("Can't stop channel");
                                    alert.initOwner((getPlayButton()).getScene().getWindow());
                                    alert.showAndWait();
                                });
                            }
                        });
                    }
                }
            });
        }

        return mPlayButton;
    }

    /**
     * Indicates if the decoder type for the channel configuration requires the JMBE library and if the
     * application is not currently setup for the JMBE library.
     */
    private boolean requiresJmbeLibrarySetup()
    {
        return getItem() != null &&
               getItem().getDecodeConfiguration().getDecoderType().providesMBEAudioFrames() &&
               !mUserPreferences.getJmbeLibraryPreference().hasJmbeLibraryPath();
    }

    /**
     * Toggles the text and graphic of the channel play button to reflect the playing state of the channel
     */
    private void setPlayButtonState(boolean playing)
    {
        if(playing)
        {
            getPlayButton().setText("Stop");
            getPlayButton().setGraphic(mStopGraphicNode);
        }
        else
        {
            getPlayButton().setText("Play");
            getPlayButton().setGraphic(mPlayGraphicNode);
        }
    }

    private ToggleSwitch getAutoStartSwitch()
    {
        if(mAutoStartSwitch == null)
        {
            mAutoStartSwitch = new ToggleSwitch("Auto-Start");
            mAutoStartSwitch.setDisable(true);
            mAutoStartSwitch.setDisable(true);
        }

        return mAutoStartSwitch;
    }

    private Spinner<Integer> getAutoStartOrderSpinner()
    {
        if(mAutoStartOrderSpinner == null)
        {
            mAutoStartOrderSpinner = new Spinner();
            mAutoStartOrderSpinner.setPrefWidth(100);
            mAutoStartOrderSpinner.setDisable(true);
            getAutoStartSwitch().selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    getAutoStartOrderSpinner().setDisable(!getAutoStartSwitch().selectedProperty().getValue());
                }
            });
            SpinnerValueFactory svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99);
            mAutoStartOrderSpinner.setValueFactory(svf);
            mAutoStartOrderSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            mAutoStartOrderSpinner.valueProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAutoStartOrderSpinner;
    }

    protected SplitPane getSplitPane()
    {
        if(mSplitPane == null)
        {
            mSplitPane = new SplitPane();
            mSplitPane.setOrientation(Orientation.HORIZONTAL);
            mSplitPane.getItems().addAll(getSidebarList(), getContentPane());
            mSplitPane.setDividerPositions(0.2);
            mSplitPane.setMaxWidth(Double.MAX_VALUE);
            mSplitPane.getStyleClass().add("invisible-divider");
        }
        return mSplitPane;
    }

    protected ListView<String> getSidebarList()
    {
        if(mSidebarList == null)
        {
            mSidebarList = new ListView<>();
            mSidebarList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && mConfigurationPanes.containsKey(newValue)) {
                    getContentPane().getChildren().setAll(mConfigurationPanes.get(newValue));
                }
            });
            mSidebarList.setMinWidth(150);
            mSidebarList.setPrefWidth(200);
        }
        return mSidebarList;
    }

    protected StackPane getContentPane()
    {
        if(mContentPane == null)
        {
            mContentPane = new StackPane();
            mContentPane.setPadding(new Insets(0, 0, 0, 10)); // Add some padding between list and content
        }
        return mContentPane;
    }

    protected void addConfigurationPane(String name, javafx.scene.Node content)
    {
        mConfigurationPanes.put(name, content);
        getSidebarList().getItems().add(name);
        if (getSidebarList().getSelectionModel().getSelectedItem() == null) {
            getSidebarList().getSelectionModel().select(name);
        }
    }



    private GridPane getTextFieldPane()
    {
        if(mTextFieldPane == null)
        {
            mTextFieldPane = new GridPane();
            mTextFieldPane.setVgap(10);
            mTextFieldPane.setHgap(10);

            ColumnConstraints cc0 = new ColumnConstraints();
            cc0.setMinWidth(Region.USE_PREF_SIZE);

            ColumnConstraints cc1 = new ColumnConstraints();
            cc1.setHgrow(Priority.ALWAYS);

            ColumnConstraints cc2 = new ColumnConstraints();
            cc2.setMinWidth(Region.USE_PREF_SIZE);

            ColumnConstraints cc3 = new ColumnConstraints();
            cc3.setMinWidth(Region.USE_PREF_SIZE);

            ColumnConstraints cc4 = new ColumnConstraints();
            cc4.setMinWidth(Region.USE_PREF_SIZE);


            ColumnConstraints cc5 = new ColumnConstraints();
            cc5.setMinWidth(Region.USE_PREF_SIZE);
            mTextFieldPane.getColumnConstraints().addAll(cc0, cc1, cc2, cc3, cc4, cc5);


            int row = 0;

            Label systemLabel = new Label("System");
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 0, row);
            mTextFieldPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getSystemField(), 1, row);
            GridPane.setHgrow(getSystemField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSystemField());

            // Auto-start removed from here and moved to actionBox



            Label siteLabel = new Label("Site");
            GridPane.setHalignment(siteLabel, HPos.RIGHT);
            GridPane.setConstraints(siteLabel, 0, ++row);
            mTextFieldPane.getChildren().add(siteLabel);

            GridPane.setConstraints(getSiteField(), 1, row);
            GridPane.setHgrow(getSiteField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getSiteField());

            // Auto-start order removed from here and moved to actionBox

            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, ++row);
            mTextFieldPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameField(), 1, row);
            GridPane.setHgrow(getNameField(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getNameField());

            Label aliasListLabel = new Label("Alias List");
            GridPane.setHalignment(aliasListLabel, HPos.RIGHT);
            GridPane.setConstraints(aliasListLabel, 2, row);
            mTextFieldPane.getChildren().add(aliasListLabel);

            GridPane.setConstraints(getAliasListComboBox(), 3, row);
            GridPane.setHgrow(getAliasListComboBox(), Priority.ALWAYS);
            mTextFieldPane.getChildren().add(getAliasListComboBox());

            GridPane.setConstraints(getNewAliasListButton(), 4, row);
            mTextFieldPane.getChildren().add(getNewAliasListButton());
        }

        return mTextFieldPane;
    }

    protected TextField getSystemField()
    {
        if(mSystemField == null)
        {
            mSystemField = new TextField();
            mSystemField.setDisable(true);
            mSystemField.setMaxWidth(Double.MAX_VALUE);
            mSystemField.textProperty().addListener(mEditorModificationListener);
        }

        return mSystemField;
    }

    protected TextField getSiteField()
    {
        if(mSiteField == null)
        {
            mSiteField = new TextField();
            mSiteField.setDisable(true);
            mSiteField.setMaxWidth(Double.MAX_VALUE);
            mSiteField.textProperty().addListener(mEditorModificationListener);
        }

        return mSiteField;
    }

    protected TextField getNameField()
    {
        if(mNameField == null)
        {
            mNameField = new TextField();
            mNameField.setDisable(true);
            mNameField.setMaxWidth(Double.MAX_VALUE);
            mNameField.textProperty().addListener(mEditorModificationListener);
        }

        return mNameField;
    }

    protected ComboBox<String> getAliasListComboBox()
    {
        if(mAliasListComboBox == null)
        {
            Predicate<String> filterPredicate = s -> !s.contentEquals(AliasModel.NO_ALIAS_LIST);
            FilteredList<String> filteredChannelList =
                new FilteredList<>(mPlaylistManager.getAliasModel().aliasListNames(), filterPredicate);
            mAliasListComboBox = new ComboBox<>(filteredChannelList);
            mAliasListComboBox.setPrefWidth(150);
            mAliasListComboBox.setDisable(true);
            mAliasListComboBox.setEditable(false);
            mAliasListComboBox.setMaxWidth(Double.MAX_VALUE);
            mAliasListComboBox.setOnAction(event -> modifiedProperty().set(true));
        }

        return mAliasListComboBox;
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.setDisable(true);
            mNewAliasListButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Create New Alias List");
                    dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                    dialog.setContentText("Name:");
                    dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                    Optional<String> result = dialog.showAndWait();

                    result.ifPresent(s -> {
                        String name = result.get();

                        if(name != null && !name.isEmpty())
                        {
                            name = name.trim();
                            mPlaylistManager.getAliasModel().addAliasList(name);
                            getAliasListComboBox().getSelectionModel().select(name);
                        }
                    });
                }
            });
        }

        return mNewAliasListButton;
    }



    /**
     * Gets the configured talkgroup value from the editor UI before saving.
     * Override in subclasses that support talkgroup assignment.
     * @return configured talkgroup, or null if not applicable or empty
     */
    protected Integer getConfiguredTalkgroup()
    {
        return null;
    }

    private Button getSaveButton()
    {
        if(mSaveButton == null)
        {
            mSaveButton = new Button("     Save     ");
            mSaveButton.getStyleClass().add("flat-button");
            mSaveButton.setMaxWidth(Double.MAX_VALUE);
            mSaveButton.disableProperty().bind(modifiedProperty().not());
            mSaveButton.setOnAction(event -> {
                Integer newTalkgroup = getConfiguredTalkgroup();
                if(newTalkgroup != null && newTalkgroup > 0)
                {
                    Channel conflictChannel = null;
                    for(Channel c : mPlaylistManager.getChannelModel().channelList())
                    {
                        if(c == getItem()) continue; // Skip current channel
                        io.github.dsheirer.module.decode.config.DecodeConfiguration dc = c.getDecodeConfiguration();
                        if(dc instanceof DecodeConfigP25)
                        {
                            if(newTalkgroup.equals(((DecodeConfigP25) dc).getTalkgroup())) conflictChannel = c;
                        }
                        else if(dc instanceof DecodeConfigAnalog)
                        {
                            if(newTalkgroup.equals(((DecodeConfigAnalog) dc).getTalkgroup())) conflictChannel = c;
                        }
                        if(conflictChannel != null) break;
                    }



                    if(conflictChannel != null)
                    {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Talkgroup " + newTalkgroup + " is already assigned to channel '" + conflictChannel.toString() + "'.\nPlease choose a different talkgroup to assign.", ButtonType.OK);
                        alert.setTitle("Talkgroup Conflict");



                        alert.setHeaderText("Cannot Save Channel Configuration");
                        alert.initOwner((getPlayButton()).getScene().getWindow());
                        alert.showAndWait();
                        return;
                    }
                }
                if(mFilterProcessor != null)
                {
                    mFilterProcessor.clearFilter();
                    save();
                    mFilterProcessor.restoreFilter();
                }
                else
                {
                    save();
                }

                if(getItem().isProcessing())
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Would you like to restart the channel?", ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Restart Channel?");
                    alert.setHeaderText("Channel configuration has changed");
                    alert.initOwner((getPlayButton()).getScene().getWindow());
                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        try
                        {
                            mPlaylistManager.getChannelProcessingManager().stop(getItem());
                            mPlaylistManager.getChannelProcessingManager().start(getItem());
                        }
                        catch(ChannelException se)
                        {
                            mLog.error("Error restarting channel", se);
                        }
                    }
                }
            });
        }

        return mSaveButton;
    }

    private Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.getStyleClass().add("flat-button");
            mResetButton.setMaxWidth(Double.MAX_VALUE);
            mResetButton.disableProperty().bind(modifiedProperty().not());
            mResetButton.setOnAction(event -> {
                modifiedProperty().set(false);
                setItem(getItem());
            });
        }

        return mResetButton;
    }


    /**
     * Simple string change listener that sets the editor modified flag to true any time text fields are edited.
     */
    public class EditorModificationListener implements ChangeListener<String>
    {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            if(getItem() != null) {
                modifiedProperty().set(true);
            }
        }
    }

    public class ChannelProcessingMonitor implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if(getItem() != null && newValue != null)
            {
                setPlayButtonState(newValue);
            }
        }
    }
}
