/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import javafx.collections.FXCollections;
import java.util.HashSet;
import java.util.Set;
import javafx.util.StringConverter;
import javafx.scene.control.ComboBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import javafx.scene.paint.Color;

/**
 * Editor for P25 fully qualified talkgroup alias identifiers
 */
public class P25FullyQualifiedTalkgroupEditor extends IdentifierEditor<P25FullyQualifiedTalkgroup>
{
    private static final Logger mLog = LoggerFactory.getLogger(P25FullyQualifiedTalkgroupEditor.class);

    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private Label mProtocolLabel;
    private ComboBox<IdentifierValue> mWacnField;
    private ComboBox<IdentifierValue> mSystemField;
    private TextField mTalkgroupField;
    private TextFormatter<Integer> mWacnTextFormatter;
    private TextFormatter<Integer> mSystemTextFormatter;
    private TextFormatter<Integer> mTalkgroupTextFormatter;
    private WacnValueChangeListener mWacnValueChangeListener = new WacnValueChangeListener();
    private SystemValueChangeListener mSystemValueChangeListener = new SystemValueChangeListener();
    private TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();

    /**
     * Constructs an instance
     * @param userPreferences for determining user preferred talkgroup formats
     * @param playlistManager for access to channel configurations
     */
    public P25FullyQualifiedTalkgroupEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        GridPane.setConstraints(getProtocolLabel(), 0, 0);
        gridPane.getChildren().add(getProtocolLabel());

        Label valueLabel = new Label("WACN", createHelpIcon("A unique code identifying a large regional radio system. This is usually provided by RadioReference and tells the software which network to follow."));
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 1, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getWacnField(), 2, 0);
        gridPane.getChildren().add(getWacnField());

        Label systemLabel = new Label("System", createHelpIcon("System Identifier. Combined with the WACN, uniquely identifies a P25 system."));
        GridPane.setHalignment(systemLabel, HPos.RIGHT);
        GridPane.setConstraints(systemLabel, 3, 0);
        gridPane.getChildren().add(systemLabel);

        GridPane.setConstraints(getSystemField(), 4, 0);
        gridPane.getChildren().add(getSystemField());

        Label radioLabel = new Label("Talkgroup", createHelpIcon("A unique ID identifying a group of radio users."));
        GridPane.setHalignment(radioLabel, HPos.RIGHT);
        GridPane.setConstraints(radioLabel, 5, 0);
        gridPane.getChildren().add(radioLabel);

        GridPane.setConstraints(getTalkgroupField(), 6, 0);
        gridPane.getChildren().add(getTalkgroupField());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(P25FullyQualifiedTalkgroup item)
    {
        super.setItem(item);

        P25FullyQualifiedTalkgroup fqt = getItem();

        getProtocolLabel().setDisable(fqt == null);
        getWacnField().setDisable(fqt == null);
        getSystemField().setDisable(fqt == null);
        getTalkgroupField().setDisable(fqt == null);

        if(fqt != null)
        {
            getProtocolLabel().setText(fqt.getProtocol().toString());
            updateTextFormatter();
        }
        else
        {
            getWacnField().setValue(null);
            getWacnField().getEditor().setText("");
            getSystemField().setValue(null);
            getSystemField().getEditor().setText("");
            getTalkgroupField().setText(null);
        }

        modifiedProperty().set(false);
    }

    private void updateTextFormatter()
    {
        if(mWacnTextFormatter != null)
        {
            mWacnTextFormatter.valueProperty().removeListener(mWacnValueChangeListener);
        }
        if(mSystemTextFormatter != null)
        {
            mSystemTextFormatter.valueProperty().removeListener(mSystemValueChangeListener);
        }
        if(mTalkgroupTextFormatter != null)
        {
            mTalkgroupTextFormatter.valueProperty().removeListener(mTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(P25FullyQualifiedTalkgroupEditor.this.getItem().getProtocol());

        if(format == IntegerFormat.DECIMAL && (mTalkgroupTextFormatter == null || !(mTalkgroupTextFormatter instanceof IntegerFormatter)))
        {
            mWacnTextFormatter = new IntegerFormatter(0,0xFFFFF);
            mSystemTextFormatter = new IntegerFormatter(0,0xFFF);
            mTalkgroupTextFormatter = new IntegerFormatter(0,0xFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - 1048575"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - 4095"));
            mTalkgroupField.setTooltip(new Tooltip("Format: 0 - 65535"));
        }
        else if(format == IntegerFormat.HEXADECIMAL && (mTalkgroupTextFormatter == null || !(mTalkgroupTextFormatter instanceof HexFormatter)))
        {
            mWacnTextFormatter = new HexFormatter(0,0xFFFFF);
            mSystemTextFormatter = new HexFormatter(0,0xFFF);
            mTalkgroupTextFormatter = new HexFormatter(0,0xFFFFFF);

            mWacnField.setTooltip(new Tooltip("Format: 0 - FFFFF"));
            mSystemField.setTooltip(new Tooltip("Format: 0 - FFF"));
            mTalkgroupField.setTooltip(new Tooltip("Format: 0 - FFFF"));
        }



        mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);

        if(getItem() != null) {
            mWacnTextFormatter.setValue(getItem().getWacn());
            mWacnField.setValue(new IdentifierValue(getItem().getWacn(), ""));
        } else {
            mWacnTextFormatter.setValue(null);
            mWacnField.setValue(null);
        }
        if(getItem() != null) {
            mSystemTextFormatter.setValue(getItem().getSystem());
            mSystemField.setValue(new IdentifierValue(getItem().getSystem(), ""));
        } else {
            mSystemTextFormatter.setValue(null);
            mSystemField.setValue(null);
        }
        mTalkgroupTextFormatter.setValue(getItem() != null ? getItem().getValue() : null);

        mWacnField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(getItem() != null && newValue != null && newValue.getValue() != null) {
                getItem().setWacn(newValue.getValue());
                modifiedProperty().set(true);
            }
        });
        mWacnTextFormatter.valueProperty().addListener(mWacnValueChangeListener);
        mSystemField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(getItem() != null && newValue != null && newValue.getValue() != null) {
                getItem().setSystem(newValue.getValue());
                modifiedProperty().set(true);
            }
        });
        mSystemTextFormatter.valueProperty().addListener(mSystemValueChangeListener);
        mTalkgroupTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
        populateDropdowns();
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    private Label getProtocolLabel()
    {
        if(mProtocolLabel == null)
        {
            mProtocolLabel = new Label();
        }

        return mProtocolLabel;
    }

    private ComboBox<IdentifierValue> getWacnField()
    {
        if(mWacnField == null)
        {
            mWacnField = new ComboBox<>();
            mWacnField.setEditable(true);
            mWacnField.getEditor().setTextFormatter(mWacnTextFormatter);
            mWacnField.setCellFactory(new javafx.util.Callback<javafx.scene.control.ListView<IdentifierValue>, javafx.scene.control.ListCell<IdentifierValue>>() {
                @Override
                public javafx.scene.control.ListCell<IdentifierValue> call(javafx.scene.control.ListView<IdentifierValue> param) {
                    return new javafx.scene.control.ListCell<IdentifierValue>() {
                        @Override
                        protected void updateItem(IdentifierValue item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                            } else {
                                String valStr = Integer.toHexString(item.getValue()).toUpperCase();
                                if (item.getLabel() != null && !item.getLabel().isEmpty()) {
                                    setText(valStr + " - " + item.getLabel());
                                } else {
                                    setText(valStr);
                                }
                            }
                        }
                    };
                }
            });
            mWacnField.setConverter(new StringConverter<IdentifierValue>() {
                @Override
                public String toString(IdentifierValue object) {
                    if (object == null || object.getValue() == null) return "";
                    return Integer.toHexString(object.getValue()).toUpperCase();
                }
                @Override
                public IdentifierValue fromString(String string) {
                    if (string == null || string.isEmpty()) return null;
                    return new IdentifierValue(mWacnTextFormatter.getValueConverter().fromString(string), "");
                }
            });

        }

        return mWacnField;
    }

    private ComboBox<IdentifierValue> getSystemField()
    {
        if(mSystemField == null)
        {
            mSystemField = new ComboBox<>();
            mSystemField.setEditable(true);
            mSystemField.getEditor().setTextFormatter(mSystemTextFormatter);
            mSystemField.setCellFactory(new javafx.util.Callback<javafx.scene.control.ListView<IdentifierValue>, javafx.scene.control.ListCell<IdentifierValue>>() {
                @Override
                public javafx.scene.control.ListCell<IdentifierValue> call(javafx.scene.control.ListView<IdentifierValue> param) {
                    return new javafx.scene.control.ListCell<IdentifierValue>() {
                        @Override
                        protected void updateItem(IdentifierValue item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                            } else {
                                String valStr = Integer.toHexString(item.getValue()).toUpperCase();
                                if (item.getLabel() != null && !item.getLabel().isEmpty()) {
                                    setText(valStr + " - " + item.getLabel());
                                } else {
                                    setText(valStr);
                                }
                            }
                        }
                    };
                }
            });
            mSystemField.setConverter(new StringConverter<IdentifierValue>() {
                @Override
                public String toString(IdentifierValue object) {
                    if (object == null || object.getValue() == null) return "";
                    return Integer.toHexString(object.getValue()).toUpperCase();
                }
                @Override
                public IdentifierValue fromString(String string) {
                    if (string == null || string.isEmpty()) return null;
                    return new IdentifierValue(mSystemTextFormatter.getValueConverter().fromString(string), "");
                }
            });

        }

        return mSystemField;
    }

    private TextField getTalkgroupField()
    {
        if(mTalkgroupField == null)
        {
            mTalkgroupField = new TextField();
            mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);
        }

        return mTalkgroupField;
    }

    public class WacnValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setWacn(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    public class SystemValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setSystem(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(getItem() != null)
            {
                getItem().setValue(newValue != null ? newValue : 0);
                modifiedProperty().set(true);
            }
        }
    }

    private void populateDropdowns()
    {
        Set<Integer> wacns = new HashSet<>();
        Set<Integer> systems = new HashSet<>();
        mWacnField.getItems().clear();
        mSystemField.getItems().clear();
        if (mPlaylistManager == null || mPlaylistManager.getChannelModel() == null) return;
        for (Channel channel : mPlaylistManager.getChannelModel().getChannels()) {
            if (channel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2) {
                DecodeConfigP25Phase2 p25 = (DecodeConfigP25Phase2) channel.getDecodeConfiguration();
                ScrambleParameters sp = p25.getScrambleParameters();
                if (sp != null) {
                    if (wacns.add(sp.getWACN())) {
                        mWacnField.getItems().add(new IdentifierValue(sp.getWACN(), channel.getName()));
                    }
                    if (systems.add(sp.getSystem())) {
                        mSystemField.getItems().add(new IdentifierValue(sp.getSystem(), channel.getName()));
                    }
                }
            }
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
