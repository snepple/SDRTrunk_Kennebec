/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.tuner;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.preference.source.TunerPreference;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.VBox;



/**
 * Preference settings for channel event view
 */
public class TunerPreferenceEditor extends VBox
{
    private static final String HELP_TEXT_POLYPHASE = "Processes all channels from tuner.  This " +
        "channelizer is more efficient when decoding 3 or more channels.";
    private static final String HELP_TEXT_HETERODYNE = "Processes each channel on-demand.  This " +
        "channelizer may work better for computers with constrained resources when processing a small number of channels.";

    private TunerPreference mTunerPreference;
    private ChoiceBox<ChannelizerType> mChannelizerTypeChoiceBox;
    private Label mPolyphaseLabel;
    private Label mHelpTextPolyphaseLabel;
    private Label mHeterodyneLabel;
    private Label mHelpTextHeterodyneLabel;
    private ChoiceBox<RspDuoSelectionMode> mRspDuoTunerModeChoiceBox;

    public TunerPreferenceEditor(UserPreferences userPreferences)
    {
        mTunerPreference = userPreferences.getTunerPreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("Source Tuners");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        SettingsCard mainCard = new SettingsCard();

        VBox channelizerBox = new VBox(5);
        channelizerBox.getChildren().addAll(getChannelizerTypeChoiceBox());
        SettingsRow channelizerRow = new SettingsRow("Channelizer Type", channelizerBox);

        VBox polyphaseBox = new VBox(2);
        polyphaseBox.setPadding(new Insets(0, 0, 10, 30));
        getHelpTextPolyphaseLabel().getStyleClass().add("kennebec-secondary-text");
        polyphaseBox.getChildren().addAll(getPolyphaseLabel(), getHelpTextPolyphaseLabel());

        VBox heterodyneBox = new VBox(2);
        heterodyneBox.setPadding(new Insets(0, 0, 10, 30));
        getHelpTextHeterodyneLabel().getStyleClass().add("kennebec-secondary-text");
        heterodyneBox.getChildren().addAll(getHeterodyneLabel(), getHelpTextHeterodyneLabel());

        VBox modeBox = new VBox(5);
        modeBox.getChildren().add(getRspDuoTunerModeChoiceBox());
        SettingsRow modeRow = new SettingsRow("SDRPlay RSPduo Selection Mode", modeBox);

        mainCard.getChildren().addAll(channelizerRow, polyphaseBox, heterodyneBox, modeRow);

        getChildren().add(mainCard);
    }

    private ChoiceBox<ChannelizerType> getChannelizerTypeChoiceBox()
    {
        if(mChannelizerTypeChoiceBox == null)
        {
            mChannelizerTypeChoiceBox = new ChoiceBox<>();
            mChannelizerTypeChoiceBox.getItems().addAll(ChannelizerType.values());
            mChannelizerTypeChoiceBox.setTooltip(new Tooltip("Determines how the SDR hardware processes incoming radio signals across its tuned bandwidth. Polyphase is more efficient for decoding 3 or more channels, while Heterodyne processes each channel on-demand."));

            ChannelizerType current = mTunerPreference.getChannelizerType();
            mChannelizerTypeChoiceBox.getSelectionModel().select(current);

            mChannelizerTypeChoiceBox.setOnAction(event -> {
                ChannelizerType selected = mChannelizerTypeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerPreference.setChannelizerType(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getChannelizerTypeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mChannelizerTypeChoiceBox;
    }

    private Label getPolyphaseLabel()
    {
        if(mPolyphaseLabel == null)
        {
            mPolyphaseLabel = new Label("Polyphase (default)");
        }

        return mPolyphaseLabel;
    }

    private Label getHelpTextPolyphaseLabel()
    {
        if(mHelpTextPolyphaseLabel == null)
        {
            mHelpTextPolyphaseLabel = new Label(HELP_TEXT_POLYPHASE);
            mHelpTextPolyphaseLabel.setWrapText(true);
        }

        return mHelpTextPolyphaseLabel;
    }

    private Label getHeterodyneLabel()
    {
        if(mHeterodyneLabel == null)
        {
            mHeterodyneLabel = new Label("Heterodyne");
        }

        return mHeterodyneLabel;
    }

    private Label getHelpTextHeterodyneLabel()
    {
        if(mHelpTextHeterodyneLabel == null)
        {
            mHelpTextHeterodyneLabel = new Label(HELP_TEXT_HETERODYNE);
            mHelpTextHeterodyneLabel.setWrapText(true);
        }

        return mHelpTextHeterodyneLabel;
    }

    private ChoiceBox<RspDuoSelectionMode> getRspDuoTunerModeChoiceBox()
    {
        if(mRspDuoTunerModeChoiceBox == null)
        {
            mRspDuoTunerModeChoiceBox = new ChoiceBox<>();
            mRspDuoTunerModeChoiceBox.getItems().addAll(RspDuoSelectionMode.values());
            mRspDuoTunerModeChoiceBox.setTooltip(new Tooltip("Configures the dual-tuner behavior of the SDRPlay RSPduo hardware (e.g., Single Tuner vs. Dual Tuner mode)."));

            RspDuoSelectionMode current = mTunerPreference.getRspDuoTunerMode();
            mRspDuoTunerModeChoiceBox.getSelectionModel().select(current);

            mRspDuoTunerModeChoiceBox.setOnAction(event -> {
                RspDuoSelectionMode selected = mRspDuoTunerModeChoiceBox.getSelectionModel().getSelectedItem();
                mTunerPreference.setRspDuoTunerMode(selected);

                Label label = new Label("Please restart the application for this change to take effect");
                label.setWrapText(true);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.getDialogPane().setContent(label);
                alert.initOwner(((Node)getRspDuoTunerModeChoiceBox()).getScene().getWindow());
                alert.show();
            });
        }

        return mRspDuoTunerModeChoiceBox;
    }

}
