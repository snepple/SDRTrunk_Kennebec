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

package io.github.dsheirer.gui.playlist.alias.identifier;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.alias.id.nac.Nac;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import javafx.collections.FXCollections;
import java.util.HashSet;
import java.util.Set;
import javafx.util.StringConverter;
import javafx.scene.control.ComboBox;

/**
 * Editor for P25 Network Access Code (NAC) alias identifiers
 */
public class NacEditor extends IdentifierEditor<Nac>
{
    private static final Logger mLog = LoggerFactory.getLogger(NacEditor.class);
    private PlaylistManager mPlaylistManager;
    private ComboBox<IdentifierValue> mNacSpinner;

    /**
     * Constructs an instance
     */
    public NacEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("NAC (0-4095 / 0x000-0xFFF)");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getNacSpinner(), 1, 0);
        gridPane.getChildren().add(getNacSpinner());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Nac item)
    {
        super.setItem(item);
        if(item.isValid())
        {
            getNacSpinner().setValue(new IdentifierValue(item.getNacValue(), ""));
        }
        modifiedProperty().set(false);
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

    /**
     * Spinner for NAC value selection (0-4095)
     */
    private ComboBox<IdentifierValue> getNacSpinner()
    {
        if(mNacSpinner == null)
        {
            mNacSpinner = new ComboBox<>();
            mNacSpinner.setEditable(true);
            mNacSpinner.setCellFactory(new javafx.util.Callback<javafx.scene.control.ListView<IdentifierValue>, javafx.scene.control.ListCell<IdentifierValue>>() {
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
            mNacSpinner.setConverter(new StringConverter<IdentifierValue>() {
                @Override
                public String toString(IdentifierValue object) {
                    if (object == null || object.getValue() == null) return "";
                    return Integer.toHexString(object.getValue()).toUpperCase();
                }
                @Override
                public IdentifierValue fromString(String string) {
                    if (string == null || string.isEmpty()) return null;
                    try {
                        return new IdentifierValue(Integer.parseInt(string, 16), "");
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            });
            mNacSpinner.setPrefWidth(100);
            populateDropdowns();
            mNacSpinner.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
                if(getItem() != null && newValue != null && !newValue.isEmpty())
                {
                    try {
                        getItem().setNacValue(Integer.parseInt(newValue, 16));
                        modifiedProperty().set(true);
                    } catch (NumberFormatException e) {}
                }
            });
            mNacSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(getItem() != null)
                {
                    getItem().setNacValue(newValue != null && newValue.getValue() != null ? newValue.getValue() : 0);
                    modifiedProperty().set(true);
                }
            });
        }

        return mNacSpinner;
    }

    private void populateDropdowns()
    {
        Set<Integer> nacs = new HashSet<>();
        mNacSpinner.getItems().clear();
        if (mPlaylistManager == null || mPlaylistManager.getChannelModel() == null) return;
        for (Channel channel : mPlaylistManager.getChannelModel().getChannels()) {
            if (channel.getDecodeConfiguration() instanceof DecodeConfigP25Phase2) {
                DecodeConfigP25Phase2 p25 = (DecodeConfigP25Phase2) channel.getDecodeConfiguration();
                ScrambleParameters sp = p25.getScrambleParameters();
                if (sp != null) {
                    if (nacs.add(sp.getNAC())) {
                        mNacSpinner.getItems().add(new IdentifierValue(sp.getNAC(), channel.getName()));
                    }
                }
            }
            if (channel.getDecodeConfiguration() instanceof DecodeConfigP25) {
                DecodeConfigP25 p25 = (DecodeConfigP25) channel.getDecodeConfiguration();
                if (p25.getAllowedNACs() != null) {
                    for (Integer nac : p25.getAllowedNACs()) {
                        if (nacs.add(nac)) {
                            mNacSpinner.getItems().add(new IdentifierValue(nac, channel.getName()));
                        }
                    }
                }
            }
        }
    }
}
