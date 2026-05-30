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
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;


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
    private ChoiceBox<RspDuoSelectionMode> mRspDuoTunerModeChoiceBox;

    public TunerPreferenceEditor(UserPreferences userPreferences)
    {
        mTunerPreference = userPreferences.getTunerPreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("Tuner Preferences");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        SettingsCard mainCard = new SettingsCard();

        Label channelizerLabel = new Label("Channelizer Type", createHelpIcon("Determines how the SDR hardware processes incoming radio signals across its tuned bandwidth. Polyphase is more efficient for decoding 3 or more channels, while Heterodyne processes each channel on-demand."));
        SettingsRow channelizerRow = new SettingsRow((Node) null, getChannelizerTypeChoiceBox());
        channelizerRow.getChildren().set(0, channelizerLabel);
        mainCard.getChildren().add(channelizerRow);

        Label rspDuoModeLabel = new Label("SDRPlay RSPduo Selection Mode", createHelpIcon("Configures the dual-tuner behavior of the SDRPlay RSPduo hardware (e.g., Single Tuner vs. Dual Tuner mode)."));
        SettingsRow rspDuoRow = new SettingsRow((Node) null, getRspDuoTunerModeChoiceBox());
        rspDuoRow.getChildren().set(0, rspDuoModeLabel);
        mainCard.getChildren().add(rspDuoRow);

        getChildren().add(mainCard);

        VBox helpTextContainer = new VBox();
        helpTextContainer.setSpacing(10);
        helpTextContainer.setPadding(new Insets(0, 10, 0, 10));

        Label polyphaseTitleLabel = new Label("Polyphase (default)");
        polyphaseTitleLabel.getStyleClass().add("kennebec-secondary-text");
        polyphaseTitleLabel.setStyle("-fx-font-weight: bold;");
        Label polyphaseDescLabel = new Label(HELP_TEXT_POLYPHASE);
        polyphaseDescLabel.getStyleClass().add("kennebec-secondary-text");
        polyphaseDescLabel.setWrapText(true);
        VBox polyphaseBox = new VBox(polyphaseTitleLabel, polyphaseDescLabel);
        polyphaseBox.setSpacing(2);

        Label heterodyneTitleLabel = new Label("Heterodyne");
        heterodyneTitleLabel.getStyleClass().add("kennebec-secondary-text");
        heterodyneTitleLabel.setStyle("-fx-font-weight: bold;");
        Label heterodyneDescLabel = new Label(HELP_TEXT_HETERODYNE);
        heterodyneDescLabel.getStyleClass().add("kennebec-secondary-text");
        heterodyneDescLabel.setWrapText(true);
        VBox heterodyneBox = new VBox(heterodyneTitleLabel, heterodyneDescLabel);
        heterodyneBox.setSpacing(2);

        helpTextContainer.getChildren().addAll(polyphaseBox, heterodyneBox);
        getChildren().add(helpTextContainer);
    }

    private ChoiceBox<ChannelizerType> getChannelizerTypeChoiceBox()
    {
        if(mChannelizerTypeChoiceBox == null)
        {
            mChannelizerTypeChoiceBox = new ChoiceBox<>();
            mChannelizerTypeChoiceBox.getItems().addAll(ChannelizerType.values());

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

    private ChoiceBox<RspDuoSelectionMode> getRspDuoTunerModeChoiceBox()
    {
        if(mRspDuoTunerModeChoiceBox == null)
        {
            mRspDuoTunerModeChoiceBox = new ChoiceBox<>();
            mRspDuoTunerModeChoiceBox.getItems().addAll(RspDuoSelectionMode.values());

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
