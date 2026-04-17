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
package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.iamresponding.IAmRespondingConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IAmRespondingEditor extends AbstractBroadcastEditor<IAmRespondingConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(IAmRespondingEditor.class);

    private GridPane mEditorPane;
    private TextField mHostTextField;
    private IntegerTextField mPortTextField;

    public IAmRespondingEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(IAmRespondingConfiguration config)
    {
        super.setItem(config);

        if(config != null)
        {
            getHostTextField().setText(config.getHost() != null ? config.getHost() : "");
            getPortTextField().set(config.getPort());

            getHostTextField().setDisable(false);
            getPortTextField().setDisable(false);
        }
        else
        {
            getHostTextField().setText("");
            getPortTextField().set(0);

            getHostTextField().setDisable(true);
            getPortTextField().setDisable(true);
        }

        modifiedProperty().set(false);
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void save()
    {
        if(getItem() != null)
        {
            getItem().setHost(getHostTextField().getText());
            getItem().setPort(getPortTextField().get());
        }

        super.save();
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.IAMRESPONDING;
    }

    @Override
    protected GridPane getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new GridPane();
            mEditorPane.setPadding(new Insets(10, 5, 10, 10));
            mEditorPane.setVgap(10);
            mEditorPane.setHgap(5);

            int row = 0;

            // Row 0: Format and Enabled
            Label formatLabel = new Label("Format");
            GridPane.setHalignment(formatLabel, HPos.RIGHT);
            GridPane.setConstraints(formatLabel, 0, row);
            mEditorPane.getChildren().add(formatLabel);

            GridPane.setConstraints(getFormatField(), 1, row);
            mEditorPane.getChildren().add(getFormatField());

            Label enabledLabel = new Label("Enabled");
            GridPane.setHalignment(enabledLabel, HPos.RIGHT);
            GridPane.setConstraints(enabledLabel, 2, row);
            mEditorPane.getChildren().add(enabledLabel);

            GridPane.setConstraints(getEnabledSwitch(), 3, row);
            mEditorPane.getChildren().add(getEnabledSwitch());

            // Row 1: Name
            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, ++row);
            mEditorPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameTextField(), 1, row);
            mEditorPane.getChildren().add(getNameTextField());

            // Row 2: Host (IP Address)
            Label hostLabel = new Label("Destination IP");
            GridPane.setHalignment(hostLabel, HPos.RIGHT);
            GridPane.setConstraints(hostLabel, 0, ++row);
            mEditorPane.getChildren().add(hostLabel);

            GridPane.setConstraints(getHostTextField(), 1, row);
            mEditorPane.getChildren().add(getHostTextField());

            // Row 3: Port (UDP)
            Label portLabel = new Label("UDP Port");
            GridPane.setHalignment(portLabel, HPos.RIGHT);
            GridPane.setConstraints(portLabel, 0, ++row);
            mEditorPane.getChildren().add(portLabel);

            GridPane.setConstraints(getPortTextField(), 1, row);
            mEditorPane.getChildren().add(getPortTextField());
        }

        return mEditorPane;
    }

    private TextField getHostTextField()
    {
        if(mHostTextField == null)
        {
            mHostTextField = new TextField();
            mHostTextField.setDisable(true);
            mHostTextField.setPrefWidth(200);
            mHostTextField.setPromptText("e.g. 192.168.1.100");
            mHostTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mHostTextField;
    }

    private IntegerTextField getPortTextField()
    {
        if(mPortTextField == null)
        {
            mPortTextField = new IntegerTextField();
            mPortTextField.setDisable(true);
            mPortTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mPortTextField;
    }
}
