/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.record;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.record.RecordPreference;
import io.github.dsheirer.record.RecordFormat;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preference settings for recording
 */
public class RecordPreferenceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(RecordPreferenceEditor.class);
    private RecordPreference mRecordPreference;
    private VBox mEditorPane;
    private ComboBox<RecordFormat> mRecordFormatComboBox;

    public RecordPreferenceEditor(UserPreferences userPreferences)
    {
        mRecordPreference = userPreferences.getRecordPreference();
        HBox.setHgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private VBox getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new VBox();
            mEditorPane.getStyleClass().add("hig-form-group");

            HBox row = new HBox();
            row.getStyleClass().add("hig-form-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label("Audio Recording Format:");
            label.getStyleClass().add("hig-form-label");

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(label, spacer, getRecordFormatComboBox());

            mEditorPane.getChildren().add(row);

            // Apply padding to match typical macOS form group usage
            HBox.setMargin(mEditorPane, new Insets(20, 20, 20, 20));
        }

        return mEditorPane;
    }

    private ComboBox<RecordFormat> getRecordFormatComboBox()
    {
        if(mRecordFormatComboBox == null)
        {
            mRecordFormatComboBox = new ComboBox<>();
            mRecordFormatComboBox.getItems().addAll(RecordFormat.values());
            mRecordFormatComboBox.getSelectionModel().select(mRecordPreference.getAudioRecordFormat());
            mRecordFormatComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<RecordFormat>()
            {
                @Override
                public void changed(ObservableValue<? extends RecordFormat> observable, RecordFormat oldValue, RecordFormat newValue)
                {
                    mRecordPreference.setAudioRecordFormat(newValue);
                }
            });
        }

        return mRecordFormatComboBox;
    }
}
