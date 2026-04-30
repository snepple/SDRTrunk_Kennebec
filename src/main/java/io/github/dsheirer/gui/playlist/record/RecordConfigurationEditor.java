/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.record;

import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import org.controlsfx.control.ToggleSwitch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Editor for event logging configuration objects using a VBox to visually display a vertical list of boolean toggle
 * switches to turn on/off event logging for a custom set of event log types.
 */
public class RecordConfigurationEditor extends Editor<RecordConfiguration>
{
    private List<RecorderControl> mControls = new ArrayList<>();

    public RecordConfigurationEditor(Collection<RecorderType> types)
    {
        setPadding(new Insets(20, 20, 20, 20));

        Label groupHeader = new Label("Recording Options");
        groupHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8E8E93; -fx-padding: 0 0 8 16;");

        VBox formGroup = new VBox();
        formGroup.getStyleClass().add("hig-form-group");

        int i = 0;
        for(RecorderType type: types)
        {
            RecorderControl control = new RecorderControl(type);
            mControls.add(control);
            formGroup.getChildren().add(control);

            if (i < types.size() - 1) {
                HBox divider = new HBox();
                divider.setStyle("-fx-border-color: transparent transparent #E5E5EA transparent; -fx-border-width: 0 0 1 0;");
                VBox.setMargin(divider, new Insets(0, 0, 0, 16));
                formGroup.getChildren().add(divider);
            }
            i++;
        }

        getChildren().addAll(groupHeader, formGroup);
    }

    @Override
    public void setItem(RecordConfiguration item)
    {
        if(item == null)
        {
            item = new RecordConfiguration();
        }

        super.setItem(item);

        for(RecorderControl control: mControls)
        {
            control.getToggleSwitch().setDisable(false);
            control.getToggleSwitch().setSelected(item.getRecorders().contains(control.getRecorderType()));
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        RecordConfiguration config = getItem();

        if(config == null)
        {
            config = new RecordConfiguration();
        }

        config.clearRecorders();

        for(RecorderControl control: mControls)
        {
            if(control.getToggleSwitch().isSelected())
            {
                config.addRecorder(control.getRecorderType());
            }
        }

        setItem(config);
    }

    @Override
    public void dispose()
    {

    }

    public class RecorderControl extends HBox
    {
        private RecorderType mRecorderType;
        private ToggleSwitch mToggleSwitch;

        public RecorderControl(RecorderType type)
        {
            mRecorderType = type;
            getStyleClass().add("hig-form-row");
            setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(mRecorderType.getDisplayString());
            label.getStyleClass().add("hig-form-label");

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            getChildren().addAll(label, spacer, getToggleSwitch());
        }

        private ToggleSwitch getToggleSwitch()
        {
            if(mToggleSwitch == null)
            {
                mToggleSwitch = new ToggleSwitch();
                mToggleSwitch.setDisable(true);
                mToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
            }

            return mToggleSwitch;
        }

        public RecorderType getRecorderType()
        {
            return mRecorderType;
        }
    }
}
