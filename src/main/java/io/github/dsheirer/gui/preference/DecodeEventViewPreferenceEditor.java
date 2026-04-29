/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.gui.preference;

import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Date;

/**
 * Preference settings for channel event view
 */
public class DecodeEventViewPreferenceEditor extends VBox
{
    private DecodeEventPreference mDecodeEventPreference;
    private ChoiceBox<DisplayableTimestamp> mTimestampFormatChoiceBox;

    public DecodeEventViewPreferenceEditor(UserPreferences userPreferences)
    {
        mDecodeEventPreference = userPreferences.getDecodeEventPreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("Channel Events");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        SettingsCard mainCard = new SettingsCard();
        SettingsRow row = new SettingsRow("Timestamp Format", getTimestampFormatChoiceBox());
        mainCard.getChildren().add(row);

        getChildren().add(mainCard);
    }

    private ChoiceBox<DisplayableTimestamp> getTimestampFormatChoiceBox()
    {
        if(mTimestampFormatChoiceBox == null)
        {
            mTimestampFormatChoiceBox = new ChoiceBox<>();

            for(TimestampFormat format: TimestampFormat.values())
            {
                mTimestampFormatChoiceBox.getItems().add(new DisplayableTimestamp(format));
            }

            TimestampFormat current = mDecodeEventPreference.getTimestampFormat();

            for(DisplayableTimestamp displayableTimestamp: mTimestampFormatChoiceBox.getItems())
            {
                if(displayableTimestamp.getTimestampFormat() == current)
                {
                    mTimestampFormatChoiceBox.getSelectionModel().select(displayableTimestamp);
                    continue;
                }
            }

            mTimestampFormatChoiceBox.setOnAction(event -> {
                DisplayableTimestamp selected = mTimestampFormatChoiceBox.getSelectionModel().getSelectedItem();
                mDecodeEventPreference.setTimestampFormat(selected.getTimestampFormat());
            });
        }

        return mTimestampFormatChoiceBox;
    }

    public class DisplayableTimestamp
    {
        private TimestampFormat mTimestampFormat;

        public DisplayableTimestamp(TimestampFormat timestampFormat)
        {
            mTimestampFormat = timestampFormat;
        }

        public TimestampFormat getTimestampFormat()
        {
            return mTimestampFormat;
        }

        public String toString()
        {
            return mTimestampFormat.getFormatter().format(new Date(System.currentTimeMillis()));
        }
    }
}
