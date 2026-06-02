/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyField;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR channel configuration editor
 */
public class DMRConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRConfigurationEditor.class);
    private javafx.scene.Node mDecoderPane;
    private javafx.scene.Node mEventLogPane;
    private javafx.scene.Node mRecordPane;
    private javafx.scene.Node mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ToggleSwitch mIgnoreDataCallsButton;
    private ToggleSwitch mIgnoreUnaliasedTalkgroupsButton;
    private ToggleSwitch mIgnoreCRCChecksumsButton;
    private ToggleSwitch mUseCompressedTalkgroupsToggle;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private TableView<TimeslotFrequency> mTimeslotFrequencyTable;
    private IntegerTextField mLogicalChannelNumberField;
    private FrequencyField mDownlinkFrequencyField;
//    private FrequencyField mUplinkFrequencyField;
    private Button mAddTimeslotFrequencyButton;
    private Button mDeleteTimeslotFrequencyButton;
    private Spinner<Integer> mChannelRotationDelaySpinner;

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public DMRConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                  UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        // Could not find name for getSourcePane()
        addConfigurationPane("Source", getSourcePane());
        // Could not find name for getDecoderPane()
        addConfigurationPane("Decoder", getDecoderPane());
        // Could not find name for getEventLogPane()
        addConfigurationPane("Logging", getEventLogPane());
        // Could not find name for getRecordPane()
        addConfigurationPane("Recording", getRecordPane());
        setupAlertsPane();
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    private javafx.scene.Node getSourcePane(){
        if(mSourcePane == null)
        {
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(getSourceConfigurationEditor());
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mSourcePane = sp;

        }

        return mSourcePane;
    }

    private javafx.scene.Node getDecoderPane(){
        if(mDecoderPane == null)
        {
            VBox content = new VBox(10);
            content.setPadding(new Insets(10, 10, 10, 10));

            SettingsCard trafficCard = new SettingsCard();
            trafficCard.getChildren().addAll(
                new SettingsRow("Max Traffic Channels", createHelpIcon("Limits how many audio conversations can be processed at the same time. Higher numbers decode more calls simultaneously but require more CPU."), getTrafficChannelPoolSizeSpinner()),
                new SettingsRow("Ignore Data Calls", createHelpIcon("Skips processing data packets, focusing only on voice traffic."), getIgnoreDataCallsButton()),
                new SettingsRow("Ignore Unaliased TGs", createHelpIcon("Skips processing calls from talkgroups that have not been explicitly defined and named in your alias list."), getIgnoreUnaliasedTalkgroupsButton()),
                new SettingsRow("Ignore CRC Checksums (RAS)", createHelpIcon("Skips CRC checks. Useful for decoding voice on systems using Restricted Access to System (RAS)."), getIgnoreCRCChecksumsButton()),
                new SettingsRow("Use Compressed Talkgroups", createHelpIcon("Use compressed talkgroup format. This is only for Hytera Tier-III Trunked Systems."), getUseCompressedTalkgroupsToggle())
            );
            content.getChildren().add(trafficCard);

            SettingsCard mapCard = new SettingsCard();
            
            Label timeslotTableLabel = new Label("Logical Channel Number (LCN) to Frequency Map. Required for: Connect Plus and Tier-III systems that don't use absolute frequencies.  LSN = Logical Slot Number");
            timeslotTableLabel.setWrapText(true);
            timeslotTableLabel.setPadding(new Insets(10, 10, 10, 10));

            VBox mapBox = new VBox(10);
            mapBox.setPadding(new Insets(10, 10, 10, 10));

            mapBox.getChildren().add(getTimeslotTable());

            HBox buttonsBox = new HBox(10);
            buttonsBox.setAlignment(Pos.CENTER);
            buttonsBox.getChildren().addAll(getAddTimeslotFrequencyButton(), getDeleteTimeslotFrequencyButton());
            mapBox.getChildren().add(buttonsBox);

            HBox editorBox = new HBox(5);
            editorBox.setAlignment(Pos.CENTER_LEFT);
            Label lcnLabel = new Label("LCN");
            Label downlinkLabel = new Label("Frequency (MHz)");
            downlinkLabel.setPadding(new Insets(0,0,0,5));
            editorBox.getChildren().addAll(lcnLabel, getLogicalChannelNumberField(), downlinkLabel, getDownlinkFrequencyField());
            mapBox.getChildren().add(editorBox);

            mapCard.getChildren().addAll(timeslotTableLabel, mapBox);
            content.getChildren().add(mapCard);

            javafx.scene.control.ScrollPane mDecoderPaneSp = new javafx.scene.control.ScrollPane(content);
            mDecoderPaneSp.setFitToWidth(true);
            mDecoderPaneSp.setFitToHeight(true);
            mDecoderPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mDecoderPane = mDecoderPaneSp;
        }

        return mDecoderPane;
    }

    private javafx.scene.Node getEventLogPane(){
        if(mEventLogPane == null)
        {
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(getEventLogConfigurationEditor());
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mEventLogPane = sp;

        }

        return mEventLogPane;
    }

    private javafx.scene.Node getRecordPane(){
        if(mRecordPane == null)
        {
            Label notice = new Label("Note: use aliases to control call audio recording");
            notice.setPadding(new Insets(10, 10, 0, 10));

            VBox vBox = new VBox();
            vBox.getChildren().addAll(getRecordConfigurationEditor(), notice);

            javafx.scene.control.ScrollPane mRecordPaneSp = new javafx.scene.control.ScrollPane(vBox);
            mRecordPaneSp.setFitToWidth(true);
            mRecordPaneSp.setFitToHeight(true);
            mRecordPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mRecordPane = mRecordPaneSp;
        }

        return mRecordPane;
    }

    private SourceConfigurationEditor getSourceConfigurationEditor()
    {
        if(mSourceConfigurationEditor == null)
        {
            mSourceConfigurationEditor = new FrequencyEditor(mTunerManager,
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_MINIMUM_MS,
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_MAXIMUM_MS,
                DecodeConfigDMR.CHANNEL_ROTATION_DELAY_DEFAULT_MS);

            //Add a listener so that we can push change notifications up to this editor
            mSourceConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSourceConfigurationEditor;
    }

    private EventLogConfigurationEditor getEventLogConfigurationEditor()
    {
        if(mEventLogConfigurationEditor == null)
        {
            List<EventLogType> types = new ArrayList<>();
            types.add(EventLogType.CALL_EVENT);
            types.add(EventLogType.DECODED_MESSAGE);
            types.add(EventLogType.TRAFFIC_CALL_EVENT);
            types.add(EventLogType.TRAFFIC_DECODED_MESSAGE);

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

    private TableView<TimeslotFrequency> getTimeslotTable()
    {
        if(mTimeslotFrequencyTable == null)
        {
            mTimeslotFrequencyTable = new TableView<>(FXCollections.observableArrayList(TimeslotFrequency.extractor()));
            mTimeslotFrequencyTable.setPrefHeight(100.0);

            TableColumn<TimeslotFrequency,Number> numberColumn = new TableColumn("LCN");
            numberColumn.setPrefWidth(75);
            numberColumn.setCellValueFactory(cellData -> cellData.getValue().getNumberProperty());
            mTimeslotFrequencyTable.getColumns().addAll(numberColumn);
            mTimeslotFrequencyTable.getSortOrder().add(numberColumn);

            TableColumn<TimeslotFrequency,Number> downlinkColumn = new TableColumn("Frequency (MHz)");
            downlinkColumn.setCellValueFactory(cellData -> cellData.getValue().getDownlinkMHz());
            downlinkColumn.setPrefWidth(150);
            mTimeslotFrequencyTable.getColumns().addAll(downlinkColumn);

            TableColumn<TimeslotFrequency,String> lsnColumn = new TableColumn("IDs (TS1/TS2)");
            lsnColumn.setPrefWidth(225);
            lsnColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            mTimeslotFrequencyTable.getColumns().addAll(lsnColumn);
            mTimeslotFrequencyTable.setPlaceholder(new Label("Click the New button to add a timeslot frequency"));

            mTimeslotFrequencyTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setTimeslot(newValue));
        }

        return mTimeslotFrequencyTable;
    }

    /**
     * Sets the specified timeslot frequency into the editor
     */
    private void setTimeslot(TimeslotFrequency timeslot)
    {
        //Preserve the current modified flag state since setting values in the editor will change it.
        boolean modified = modifiedProperty().get();

        getLogicalChannelNumberField().setDisable(timeslot == null);
        getDownlinkFrequencyField().setDisable(timeslot == null);
//        getUplinkFrequencyField().setDisable(timeslot == null);
        getDeleteTimeslotFrequencyButton().setDisable(timeslot == null);

        if(timeslot != null)
        {
            getLogicalChannelNumberField().set(timeslot.getNumber());
            getDownlinkFrequencyField().set(timeslot.getDownlinkFrequency());
//            getUplinkFrequencyField().set(timeslot.getUplinkFrequency());
        }
        else
        {
            getLogicalChannelNumberField().set(0);
            getDownlinkFrequencyField().set(0);
//            getUplinkFrequencyField().set(0);
        }

        modifiedProperty().set(modified);
    }

    private Button getAddTimeslotFrequencyButton()
    {
        if(mAddTimeslotFrequencyButton == null)
        {
            mAddTimeslotFrequencyButton = new Button("Add");
            mAddTimeslotFrequencyButton.setMaxWidth(Double.MAX_VALUE);
            mAddTimeslotFrequencyButton.setOnAction(event -> addTimeslot());
        }

        return mAddTimeslotFrequencyButton;
    }

    /**
     * Adds a new timeslot frequency value and makes a best guess of the next sequential LSN number
     */
    private void addTimeslot()
    {
        int lsn = 1;

        while(hasLSN(lsn) && lsn <= 64) //64 is an arbitrary value to keep it from going too high
        {
            lsn++;
        }

        TimeslotFrequency timeslotFrequency = new TimeslotFrequency();
        timeslotFrequency.setNumber(lsn);
        getTimeslotTable().getItems().add(timeslotFrequency);
        getTimeslotTable().scrollTo(timeslotFrequency);
        getTimeslotTable().getSelectionModel().select(timeslotFrequency);
        modifiedProperty().set(true);
    }

    /**
     * Searches the current timeslot frequency list to determine if the specified lsn is already listed
     */
    private boolean hasLSN(int lsn)
    {
        for(TimeslotFrequency timeslotFrequency: getTimeslotTable().getItems())
        {
            if(timeslotFrequency.getNumber() == lsn)
            {
                return true;
            }
        }

        return false;
    }

    private Button getDeleteTimeslotFrequencyButton()
    {
        if(mDeleteTimeslotFrequencyButton == null)
        {
            mDeleteTimeslotFrequencyButton = new Button("Delete");
            mDeleteTimeslotFrequencyButton.setDisable(true);
            mDeleteTimeslotFrequencyButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteTimeslotFrequencyButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    TimeslotFrequency selected = getTimeslotTable().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        getTimeslotTable().getItems().remove(selected);
                        modifiedProperty().set(true);
                    }
                }
            });
        }

        return mDeleteTimeslotFrequencyButton;
    }

    private IntegerTextField getLogicalChannelNumberField()
    {
        if(mLogicalChannelNumberField == null)
        {
            mLogicalChannelNumberField = new IntegerTextField();
            mLogicalChannelNumberField.setDisable(true);
            mLogicalChannelNumberField.setPrefWidth(65);
            mLogicalChannelNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
                TimeslotFrequency selected = getTimeslotTable().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Integer value = mLogicalChannelNumberField.get();

                    if(value != null)
                    {
                        selected.setNumber(value);
                    }
                }

                modifiedProperty().set(true);
            });
        }

        return mLogicalChannelNumberField;
    }

    private FrequencyField getDownlinkFrequencyField()
    {
        if(mDownlinkFrequencyField == null)
        {
            mDownlinkFrequencyField = new FrequencyField();
            mDownlinkFrequencyField.setDisable(true);
            mDownlinkFrequencyField.textProperty().addListener(new ChangeListener<String>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
                {
                    TimeslotFrequency selected = getTimeslotTable().getSelectionModel().getSelectedItem();

                    if(selected != null)
                    {
                        selected.setDownlinkFrequency(mDownlinkFrequencyField.get());
                    }

                    modifiedProperty().set(true);
                }
            });
        }

        return mDownlinkFrequencyField;
    }

//    private FrequencyField getUplinkFrequencyField()
//    {
//        if(mUplinkFrequencyField == null)
//        {
//            mUplinkFrequencyField = new FrequencyField();
//            mUplinkFrequencyField.setDisable(true);
//            mUplinkFrequencyField.textProperty().addListener(new ChangeListener<String>()
//            {
//                @Override
//                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
//                {
//                    TimeslotFrequency selected = getTimeslotTable().getSelectionModel().getSelectedItem();
//
//                    if(selected != null)
//                    {
//                        selected.setUplinkFrequency(mUplinkFrequencyField.get());
//                        int lsn = selected.getNumber();
//                        selected.setNumber(-1);
//                        selected.setNumber(lsn);
//                    }
//
//                    modifiedProperty().set(true);
//                }
//            });
//        }
//
//        return mUplinkFrequencyField;
//    }

    private ToggleSwitch getIgnoreDataCallsButton()
    {
        if(mIgnoreDataCallsButton == null)
        {
            mIgnoreDataCallsButton = new ToggleSwitch();
            mIgnoreDataCallsButton.setDisable(true);
            mIgnoreDataCallsButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIgnoreDataCallsButton;
    }

    private ToggleSwitch getIgnoreUnaliasedTalkgroupsButton()
    {
        if(mIgnoreUnaliasedTalkgroupsButton == null)
        {
            mIgnoreUnaliasedTalkgroupsButton = new ToggleSwitch();
            mIgnoreUnaliasedTalkgroupsButton.setDisable(true);
            mIgnoreUnaliasedTalkgroupsButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIgnoreUnaliasedTalkgroupsButton;
    }

    private ToggleSwitch getIgnoreCRCChecksumsButton()
    {
        if(mIgnoreCRCChecksumsButton == null)
        {
            mIgnoreCRCChecksumsButton = new ToggleSwitch();
            mIgnoreCRCChecksumsButton.setDisable(true);
            mIgnoreCRCChecksumsButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mIgnoreCRCChecksumsButton;
    }

    /**
     * Use compressed talkgroups toggle switch.  Let's the user select to turn on compressed talkgroups for Hytera
     * Tier-III systems.
     * @return toggle.
     */
    private ToggleSwitch getUseCompressedTalkgroupsToggle()
    {
        if(mUseCompressedTalkgroupsToggle == null)
        {
            mUseCompressedTalkgroupsToggle = new ToggleSwitch();
            mUseCompressedTalkgroupsToggle.setTooltip(new Tooltip("Use Compressed Talkgroups.  This is only for Hytera Tier-III Trunked Systems"));
            mUseCompressedTalkgroupsToggle.setDisable(true);
            mUseCompressedTalkgroupsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mUseCompressedTalkgroupsToggle;
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner();
            mTrafficChannelPoolSizeSpinner.setDisable(true);
            mTrafficChannelPoolSizeSpinner.setTooltip(
                new Tooltip("Maximum number of traffic channels that can be created by the decoder"));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
    }

    /**
     * Channel rotation monitor delay value.  This dictates how long the decoder will remain on each frequency before
     * rotating to the next frequency in the list
     * @return spinner
     */
    private Spinner<Integer> getChannelRotationDelaySpinner()
    {
        if(mChannelRotationDelaySpinner == null)
        {
            mChannelRotationDelaySpinner = new Spinner();
            mChannelRotationDelaySpinner.setDisable(true);
            mChannelRotationDelaySpinner.setTooltip(
                new Tooltip("Delay on each frequency before rotating to next when seeking to next active channel frequency"));
            mChannelRotationDelaySpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            SpinnerValueFactory<Integer> svf = new SpinnerValueFactory.IntegerSpinnerValueFactory(200, 2000, 200, 50);
            mChannelRotationDelaySpinner.setValueFactory(svf);
            mChannelRotationDelaySpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mChannelRotationDelaySpinner;
    }

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
            types.add(RecorderType.DEMODULATED_BIT_STREAM);
            types.add(RecorderType.MBE_CALL_SEQUENCE);
            types.add(RecorderType.TRAFFIC_BASEBAND);
            types.add(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
            types.add(RecorderType.TRAFFIC_MBE_CALL_SEQUENCE);
            mRecordConfigurationEditor = new RecordConfigurationEditor(types);
            mRecordConfigurationEditor.setDisable(true);
            mRecordConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordConfigurationEditor;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        getIgnoreCRCChecksumsButton().setDisable(config == null);
        getIgnoreDataCallsButton().setDisable(config == null);
        getIgnoreUnaliasedTalkgroupsButton().setDisable(config == null);
        getUseCompressedTalkgroupsToggle().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);
        getTimeslotTable().getItems().clear();
        getTimeslotTable().setDisable(config == null);
        getAddTimeslotFrequencyButton().setDisable(config == null);
        getDeleteTimeslotFrequencyButton().setDisable(true);
        getLogicalChannelNumberField().set(0);
        getLogicalChannelNumberField().setDisable(true);
        getDownlinkFrequencyField().set(0);
        getDownlinkFrequencyField().setDisable(true);
//        getUplinkFrequencyField().set(0);
//        getUplinkFrequencyField().setDisable(true);
        getChannelRotationDelaySpinner().setDisable(config == null);

        if(config instanceof DecodeConfigDMR)
        {
            DecodeConfigDMR decodeConfig = (DecodeConfigDMR)config;

            getIgnoreDataCallsButton().setSelected(decodeConfig.getIgnoreDataCalls());
            getIgnoreUnaliasedTalkgroupsButton().setSelected(decodeConfig.getIgnoreUnaliasedTalkgroups());
            getIgnoreCRCChecksumsButton().setSelected(decodeConfig.getIgnoreCRCChecksums());
            getUseCompressedTalkgroupsToggle().setSelected(decodeConfig.isUseCompressedTalkgroups());
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfig.getTrafficChannelPoolSize());

            for(TimeslotFrequency timeslotFrequency: decodeConfig.getTimeslotMap())
            {
                getTimeslotTable().getItems().add(timeslotFrequency.copy());
            }
        }
        else
        {
            getIgnoreCRCChecksumsButton().setSelected(false);
            getIgnoreDataCallsButton().setSelected(false);
            getIgnoreUnaliasedTalkgroupsButton().setSelected(false);
            getUseCompressedTalkgroupsToggle().setSelected(false);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(0);
            getChannelRotationDelaySpinner().getValueFactory().setValue(200);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigDMR config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigDMR)
        {
            config = (DecodeConfigDMR)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigDMR();
        }

        config.setIgnoreCRCChecksums(getIgnoreCRCChecksumsButton().isSelected());
        config.setIgnoreDataCalls(getIgnoreDataCallsButton().isSelected());
        config.setIgnoreUnaliasedTalkgroups(getIgnoreUnaliasedTalkgroupsButton().isSelected());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());
        config.setUseCompressedTalkgroups(getUseCompressedTalkgroupsToggle().isSelected());
        config.setTimeslotMap(new ArrayList<>(getTimeslotTable().getItems()));
        getItem().setDecodeConfiguration(config);
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
        getEventLogConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveEventLogConfiguration()
    {
        getEventLogConfigurationEditor().save();

        if(getEventLogConfigurationEditor().getItem().getLoggers().isEmpty())
        {
            getItem().setEventLogConfiguration(null);
        }
        else
        {
            getItem().setEventLogConfiguration(getEventLogConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config)
    {
        //no-op
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
        //no-op
    }

    @Override
    protected void setRecordConfiguration(RecordConfiguration config)
    {
        getRecordConfigurationEditor().setDisable(config == null);
        getRecordConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveRecordConfiguration()
    {
        getRecordConfigurationEditor().save();
        RecordConfiguration config = getRecordConfigurationEditor().getItem();
        getItem().setRecordConfiguration(config);
    }

    @Override
    protected void setSourceConfiguration(SourceConfiguration config)
    {
        getSourceConfigurationEditor().setSourceConfiguration(config);
    }

    @Override
    protected void saveSourceConfiguration()
    {
        getSourceConfigurationEditor().save();
        SourceConfiguration sourceConfiguration = getSourceConfigurationEditor().getSourceConfiguration();
        getItem().setSourceConfiguration(sourceConfiguration);
    }

    /**
     * Channel tuner channel source frequencies value factory
     */
    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<TimeslotFrequency, String>,
            ObservableValue<String>>
    {
        private SimpleStringProperty mFrequency = new SimpleStringProperty();
        private boolean mIsDownlink;

        public FrequencyCellValueFactory(boolean isDownlink)
        {
            mIsDownlink = isDownlink;
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TimeslotFrequency, String> param)
        {
            if(param.getValue() != null)
            {
                long frequency = (mIsDownlink ? param.getValue().getDownlinkFrequency() : param.getValue().getUplinkFrequency());
                mFrequency.set(String.valueOf(frequency / 1E6));
            }
            else
            {
                mFrequency.set(null);
            }

            return mFrequency;
        }
    }

    public class DownlinkPropertyValueFactory extends PropertyValueFactory<TimeslotFrequency,String>
    {
        private StringProperty mStringProperty = new SimpleStringProperty();

        public DownlinkPropertyValueFactory()
        {
            super("downlinkFrequency");
        }

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<TimeslotFrequency,String> param)
        {
            if(param.getValue() != null)
            {
                mStringProperty.set(String.valueOf(param.getValue().getDownlinkFrequency() / 1E6));
            }
            else
            {
                mStringProperty.setValue(null);
            }

            return mStringProperty;
        }
    }

    private Label createHelpIcon(String tooltipText) {
        IconNode iconNode = new IconNode(FontAwesome.INFO_CIRCLE);
        iconNode.setIconSize(14);
        iconNode.setFill(Color.GRAY);
        Label label = new Label("", iconNode);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400);
        label.setTooltip(tooltip);
        return label;
    }

}
