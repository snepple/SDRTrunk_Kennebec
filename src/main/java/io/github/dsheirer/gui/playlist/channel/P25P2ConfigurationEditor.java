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

import io.github.dsheirer.gui.control.IntegerTextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBox;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
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
import java.util.TreeSet;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P25 Phase 2 channel configuration editor
 */
public class P25P2ConfigurationEditor extends ChannelConfigurationEditor
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2ConfigurationEditor.class);
    private javafx.scene.Node mDecoderPane;
    private javafx.scene.Node mEventLogPane;
    private javafx.scene.Node mRecordPane;
    private javafx.scene.Node mSourcePane;
    private SourceConfigurationEditor mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ComboBox<Integer> mWacnComboBox;
    private ComboBox<Integer> mSystemComboBox;
    private ComboBox<Integer> mNacComboBox;
    private ToggleSwitch mIgnoreDataCallsButton;
    private ToggleSwitch mIgnoreUnaliasedTalkgroupsButton;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;

    /**
     * Constructs an instance
     * @param playlistManager
     * @param userPreferences
     */
    public P25P2ConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
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
        return DecoderType.P25_PHASE2;
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
            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            int row = 0;

            Label poolSizeLabel = new Label("Max Traffic Channels", createHelpIcon("Limits how many audio conversations can be processed at the same time. Higher numbers decode more calls simultaneously but require more CPU."));
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, row);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, row);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            GridPane.setConstraints(getIgnoreDataCallsButton(), 2, row);
            gridPane.getChildren().add(getIgnoreDataCallsButton());

            Label directionLabel = new Label("Ignore Data Calls", createHelpIcon("Skips processing data packets, focusing only on voice traffic."));
            GridPane.setHalignment(directionLabel, HPos.LEFT);
            GridPane.setConstraints(directionLabel, 3, row);
            gridPane.getChildren().add(directionLabel);

            GridPane.setConstraints(getIgnoreUnaliasedTalkgroupsButton(), 4, row);
            gridPane.getChildren().add(getIgnoreUnaliasedTalkgroupsButton());

            Label ignoreUnaliasedLabel = new Label("Ignore Unaliased TGs", createHelpIcon("Skips processing calls from talkgroups that have not been explicitly defined and named in your alias list."));
            GridPane.setHalignment(ignoreUnaliasedLabel, HPos.LEFT);
            GridPane.setConstraints(ignoreUnaliasedLabel, 5, row);
            gridPane.getChildren().add(ignoreUnaliasedLabel);

            Label wacnLabel = new Label("WACN", createHelpIcon("Wide Area Communication Network (WACN) identifier. Required for cross-system P25 calls where the raw ID alone is not unique."));
            GridPane.setHalignment(wacnLabel, HPos.RIGHT);
            GridPane.setConstraints(wacnLabel, 0, ++row);
            gridPane.getChildren().add(wacnLabel);

            GridPane.setConstraints(getWacnComboBox(), 1, row);
            gridPane.getChildren().add(getWacnComboBox());

            Label systemLabel = new Label("System", createHelpIcon("System Identifier. Combined with the WACN, uniquely identifies a P25 system."));
            GridPane.setHalignment(systemLabel, HPos.RIGHT);
            GridPane.setConstraints(systemLabel, 2, row);
            gridPane.getChildren().add(systemLabel);

            GridPane.setConstraints(getSystemComboBox(), 3, row);
            gridPane.getChildren().add(getSystemComboBox());

            Label nacLabel = new Label("NAC", createHelpIcon("Network Access Code (NAC). A unique code identifying a specific radio system to follow."));
            GridPane.setHalignment(nacLabel, HPos.RIGHT);
            GridPane.setConstraints(nacLabel, 4, row);
            gridPane.getChildren().add(nacLabel);

            GridPane.setConstraints(getNacComboBox(), 5, row);
            gridPane.getChildren().add(getNacComboBox());

            Label noteLabel = new Label("Note: WACN/System/NAC values are auto-detected (ie not required) from " +
                    "the control channel and are only required when decoding individual traffic channels");
            GridPane.setHalignment(noteLabel, HPos.LEFT);
            GridPane.setConstraints(noteLabel, 1, ++row, 6, 1);
            gridPane.getChildren().add(noteLabel);


            javafx.scene.control.ScrollPane mDecoderPaneSp = new javafx.scene.control.ScrollPane(gridPane);
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
            mSourceConfigurationEditor = new FrequencyEditor(mTunerManager);

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

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

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

    private ComboBox<Integer> getWacnComboBox()
    {
        if(mWacnComboBox == null)
        {
            mWacnComboBox = new ComboBox<>();
            mWacnComboBox.setEditable(true);
            mWacnComboBox.setDisable(true);
            mWacnComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mWacnComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mWacnComboBox;
    }

    private ComboBox<Integer> getSystemComboBox()
    {
        if(mSystemComboBox == null)
        {
            mSystemComboBox = new ComboBox<>();
            mSystemComboBox.setEditable(true);
            mSystemComboBox.setDisable(true);
            mSystemComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mSystemComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSystemComboBox;
    }

    private ComboBox<Integer> getNacComboBox()
    {
        if(mNacComboBox == null)
        {
            mNacComboBox = new ComboBox<>();
            mNacComboBox.setEditable(true);
            mNacComboBox.setDisable(true);
            mNacComboBox.setConverter(new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer object) { return object != null ? object.toString() : ""; }
                @Override
                public Integer fromString(String string) { try { return Integer.parseInt(string); } catch(NumberFormatException e) { return null; } }
            });
            mNacComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mNacComboBox;
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
        if(config instanceof DecodeConfigP25Phase2 decodeConfig)
        {
            getWacnComboBox().setDisable(false);
            getSystemComboBox().setDisable(false);
            getNacComboBox().setDisable(false);

            java.util.Set<Integer> knownWacns = new java.util.TreeSet<>();
            java.util.Set<Integer> knownSystems = new java.util.TreeSet<>();
            java.util.Set<Integer> knownNacs = new java.util.TreeSet<>();
            if(getPlaylistManager() != null && getPlaylistManager().getChannelModel() != null) {
                for(io.github.dsheirer.controller.channel.Channel channel : getPlaylistManager().getChannelModel().getChannels()) {
                    if(channel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2 p25) {
                        ScrambleParameters sp = p25.getScrambleParameters();
                        if(sp != null) {
                            knownWacns.add(sp.getWACN());
                            knownSystems.add(sp.getSystem());
                            knownNacs.add(sp.getNAC());
                        }
                    }
                }
            }
            getWacnComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownWacns));
            getSystemComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownSystems));
            getNacComboBox().setItems(javafx.collections.FXCollections.observableArrayList(knownNacs));

            ScrambleParameters scrambleParameters = decodeConfig.getScrambleParameters();

            if(scrambleParameters != null)
            {
                getWacnComboBox().setValue(scrambleParameters.getWACN());
                getWacnComboBox().getEditor().setText(String.format("%X", scrambleParameters.getWACN()));
                getSystemComboBox().setValue(scrambleParameters.getSystem());
                getSystemComboBox().getEditor().setText(String.format("%X", scrambleParameters.getSystem()));
                getNacComboBox().setValue(scrambleParameters.getNAC());
                getNacComboBox().getEditor().setText(String.format("%X", scrambleParameters.getNAC()));
            }
            else
            {
                getWacnComboBox().setValue(0);
                getWacnComboBox().getEditor().setText("0");
                getSystemComboBox().setValue(0);
                getSystemComboBox().getEditor().setText("0");
                getNacComboBox().setValue(0);
                getNacComboBox().getEditor().setText("0");
            }

            getIgnoreDataCallsButton().setDisable(false);
            getIgnoreDataCallsButton().setSelected(decodeConfig.getIgnoreDataCalls());
            getIgnoreUnaliasedTalkgroupsButton().setDisable(false);
            getIgnoreUnaliasedTalkgroupsButton().setSelected(decodeConfig.getIgnoreUnaliasedTalkgroups());
            getTrafficChannelPoolSizeSpinner().setDisable(false);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(decodeConfig.getTrafficChannelPoolSize());
        }
        else
        {
            getWacnComboBox().setValue(0);
                getWacnComboBox().getEditor().setText("0");
            getSystemComboBox().setValue(0);
                getSystemComboBox().getEditor().setText("0");
            getNacComboBox().setValue(0);
                getNacComboBox().getEditor().setText("0");
            getWacnComboBox().setDisable(true);
            getSystemComboBox().setDisable(true);
            getNacComboBox().setDisable(true);
            getIgnoreDataCallsButton().setDisable(true);
            getIgnoreUnaliasedTalkgroupsButton().setDisable(true);
            getTrafficChannelPoolSizeSpinner().setDisable(true);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigP25Phase2 config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigP25Phase2 p2)
        {
            config = p2;
        }
        else
        {
            config = new DecodeConfigP25Phase2();
        }

        config.setAutoDetectScrambleParameters(false);
        Integer wacnVal = getWacnComboBox().getValue(); if (wacnVal == null) { try { wacnVal = Integer.parseInt(getWacnComboBox().getEditor().getText(), 16); } catch(Exception e) {} } int wacn = wacnVal != null ? wacnVal : 0;
        Integer systemVal = getSystemComboBox().getValue(); if (systemVal == null) { try { systemVal = Integer.parseInt(getSystemComboBox().getEditor().getText(), 16); } catch(Exception e) {} } int system = systemVal != null ? systemVal : 0;
        Integer nacVal = getNacComboBox().getValue(); if (nacVal == null) { try { nacVal = Integer.parseInt(getNacComboBox().getEditor().getText(), 16); } catch(Exception e) {} } int nac = nacVal != null ? nacVal : 0;
        config.setScrambleParameters(new ScrambleParameters(wacn, system, nac));
        config.setIgnoreDataCalls(getIgnoreDataCallsButton().isSelected());
        config.setIgnoreUnaliasedTalkgroups(getIgnoreUnaliasedTalkgroupsButton().isSelected());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());

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
