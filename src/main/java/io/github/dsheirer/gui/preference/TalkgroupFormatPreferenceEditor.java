/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Set;

/**
 * Preference settings for channel event view
 */
public class TalkgroupFormatPreferenceEditor extends VBox
{
    private TalkgroupFormatPreference mTalkgroupFormatPreference;

    public TalkgroupFormatPreferenceEditor(UserPreferences userPreferences)
    {
        mTalkgroupFormatPreference = userPreferences.getTalkgroupFormatPreference();
        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("Talkgroup & Radio ID Formats");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        SettingsCard mainCard = new SettingsCard();

        for(Protocol protocol : Protocol.TALKGROUP_PROTOCOLS)
        {
            IntegerFormatEditor formatEditor = new IntegerFormatEditor(mTalkgroupFormatPreference, protocol,
                TalkgroupFormatPreference.getFormats(protocol));
            formatEditor.setPrefWidth(120);

            FixedWidthEditor fixedWidthEditor = new FixedWidthEditor(mTalkgroupFormatPreference, protocol);

            HBox controlsBox = new HBox(15);
            controlsBox.getChildren().addAll(fixedWidthEditor, formatEditor);
            controlsBox.setAlignment(Pos.CENTER_RIGHT);

            SettingsRow row = new SettingsRow(protocol.toString(), controlsBox);
            mainCard.getChildren().add(row);
        }

        getChildren().add(mainCard);
    }

    /**
     * Choice box control for tracking talkgroup format preference per protocol
     */
    public class IntegerFormatEditor extends ChoiceBox<IntegerFormat>
    {
        private TalkgroupFormatPreference mTalkgroupFormatPreference;
        private Protocol mProtocol;

        public IntegerFormatEditor(TalkgroupFormatPreference preference, Protocol protocol, Set<IntegerFormat> formats)
        {
            mTalkgroupFormatPreference = preference;
            mProtocol = protocol;
            setMaxWidth(Double.MAX_VALUE);
            getItems().addAll(formats);
            getSelectionModel().select(mTalkgroupFormatPreference.getTalkgroupFormat(mProtocol));
            setOnAction(event -> {
                IntegerFormat selected = getSelectionModel().getSelectedItem();
                mTalkgroupFormatPreference.setTalkgroupFormat(mProtocol, selected);
            });
        }
    }

    /**
     * Check box control for tracking fixed width preference per protocol
     */
    public class FixedWidthEditor extends CheckBox
    {
        private TalkgroupFormatPreference mTalkgroupFormatPreference;
        private Protocol mProtocol;

        public FixedWidthEditor(TalkgroupFormatPreference preference, Protocol protocol)
        {
            super("Fixed Width");
            mTalkgroupFormatPreference = preference;
            mProtocol = protocol;
            setSelected(mTalkgroupFormatPreference.isTalkgroupFixedWidth(mProtocol));
            setOnAction(event -> mTalkgroupFormatPreference.setTalkgroupFixedWidth(mProtocol, isSelected()));
        }
    }
}
