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

package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;



import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;










/**
 * Dialog to select a recording and specify a center frequency for use in adding a Recording Tuner
 */
public class AddRecordingTunerDialog
{
    private final static Logger mLog = LoggerFactory.getLogger(AddRecordingTunerDialog.class);
    private static final String SELECT_A_FILE = "Please select a recording file";
    private static final String LAST_FILE_BROWSE_LOCATION_KEY = "AddRecordingTunerDialog.lastBrowseLocation";
    private UserPreferences mUserPreferences;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private TunerConfigurationManager mTunerConfigurationManager;

    private File mSelectedRecording;





    private static final Pattern TUNER_RECORDING_PATTERN = Pattern.compile(".*_(\\d*)_baseband_\\d{8}_\\d{6}\\.wav");


    private Stage mStage;
    private Button mSelectFileButton;
    private Label mRecordingFileLabel;
    private Label mFrequencyLabel;
    private TextField mFrequencyTextField;
    private Button mAddButton;
    private Button mCancelButton;

    public AddRecordingTunerDialog(UserPreferences userPreferences, DiscoveredTunerModel discoveredTunerModel,
                                   TunerConfigurationManager tunerConfigurationManager)
    {
        Validate.notNull(userPreferences, "UserPreferences cannot be null");
        Validate.notNull(discoveredTunerModel, "TunerModel cannot be null");
        Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");

        mDiscoveredTunerModel = discoveredTunerModel;
        mUserPreferences = userPreferences;
        mTunerConfigurationManager = tunerConfigurationManager;

        Platform.runLater(() -> {
            mStage = new Stage();
            mStage.setTitle("Select Recording File");

            VBox root = new VBox(10);
            root.setPadding(new Insets(10));

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            mSelectFileButton = new Button("Select ...");
            mRecordingFileLabel = new Label(SELECT_A_FILE);

            mSelectFileButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Recording File");

                String lastBrowsedDirectory = SystemProperties.getInstance().get(LAST_FILE_BROWSE_LOCATION_KEY, "");
                File browseDirectory;
                if(lastBrowsedDirectory != null && !lastBrowsedDirectory.isEmpty()) {
                    browseDirectory = new File(lastBrowsedDirectory);
                } else {
                    browseDirectory = mUserPreferences.getDirectoryPreference().getDefaultRecordingDirectory().toFile();
                }

                if (browseDirectory.exists() && browseDirectory.isDirectory()) {
                    fileChooser.setInitialDirectory(browseDirectory);
                }

                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Recordings (*.wav)", "*.wav"));

                File file = fileChooser.showOpenDialog(mStage);
                if (file != null) {
                    mSelectedRecording = file;
                    SystemProperties.getInstance().set(LAST_FILE_BROWSE_LOCATION_KEY, file.getParent());
                    mAddButton.setDisable(false);
                    mRecordingFileLabel.setText(file.getName());

                    if(mFrequencyTextField.getText() == null || mFrequencyTextField.getText().isEmpty()) {
                        Matcher m = TUNER_RECORDING_PATTERN.matcher(mSelectedRecording.getName());
                        if(m.matches()) {
                            mFrequencyTextField.setText(m.group(1));
                        }
                    }
                } else {
                    mSelectedRecording = null;
                    mRecordingFileLabel.setText(SELECT_A_FILE);
                    mAddButton.setDisable(true);
                }
            });

            grid.add(mSelectFileButton, 0, 0);
            grid.add(mRecordingFileLabel, 1, 0);

            mFrequencyLabel = new Label("Frequency (Hz):");
            mFrequencyTextField = new TextField("");

            grid.add(mFrequencyLabel, 0, 1);
            grid.add(mFrequencyTextField, 1, 1);

            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            mAddButton = new Button("Add");
            mAddButton.setDisable(true);
            mAddButton.setOnAction(e -> {
                if(mSelectedRecording == null) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please select a recording file");
                    alert.showAndWait();
                    return;
                }

                long frequency = getFrequency();

                if(frequency <= 0 || frequency > Integer.MAX_VALUE) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please provide a recording center frequency (1 Hz to 2.14 GHz)");
                    alert.showAndWait();
                    return;
                }

                mLog.info("Adding recording tuner = frequency [" + frequency +
                    "] recording [" + mSelectedRecording.getAbsolutePath() + "]");

                try {
                    RecordingTunerConfiguration config = RecordingTunerConfiguration.create();
                    config.setFrequency(frequency);
                    config.setPath(mSelectedRecording.getAbsolutePath());
                    mTunerConfigurationManager.addTunerConfiguration(config);
                    DiscoveredRecordingTuner discoveredRecordingTuner = new DiscoveredRecordingTuner(mUserPreferences, config);
                    mDiscoveredTunerModel.addDiscoveredTuner(discoveredRecordingTuner);
                } catch(Exception ex) {
                    mLog.error("Error adding recording tuner", ex);
                }

                mStage.close();
            });

            mCancelButton = new Button("Cancel");
            mCancelButton.setOnAction(e -> mStage.close());

            buttonBox.getChildren().addAll(mAddButton, mCancelButton);

            root.getChildren().addAll(grid, buttonBox);

            Scene scene = new Scene(root, 500, 150);
            mStage.setScene(scene);
        });
    }

    public void setVisible(boolean visible) {
        if (mStage != null) {
            Platform.runLater(() -> {
                if (visible) {
                    mStage.show();
                    mStage.toFront();
                } else {
                    mStage.hide();
                }
            });
        } else {
            // Stage might not be initialized yet, add a listener or wait
            Platform.runLater(() -> {
                if (mStage != null && visible) {
                    mStage.show();
                    mStage.toFront();
                }
            });
        }
    }

    private long getFrequency()
    {
        String text = mFrequencyTextField.getText();

        if(text != null && !text.isEmpty())
        {
            try
            {
                return Long.parseLong(text);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the frequency value
            }
        }

        return 0l;
    }
}
