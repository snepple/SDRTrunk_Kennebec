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

package io.github.dsheirer.gui.preference.decoder;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import io.github.dsheirer.jmbe.JmbeCreator;
import io.github.dsheirer.jmbe.JmbeEditorRequest;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.jmbe.github.Version;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Preference settings for decoders
 */
public class JmbeLibraryPreferenceEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeLibraryPreferenceEditor.class);

    private static final String PATH_NOT_SET = "(not set)";
    private static final String CREATE_LIBRARY = "Create Library";
    private static final String CHECK_FOR_UPDATE = "Check For Library Update";
    private static final String LIBRARY_NOT_SETUP = "None.  Click Select or Create Library button to setup.";

    private UserPreferences mUserPreferences;
    private Label mJmbeVersionLabel;
    private Label mPathToJmbeLibraryLabel;
    private Button mSelectButton;
    private Button mResetButton;
    private Button mCreateButton;
    private HBox mButtonsBox;
    private CheckBox mAlertUserWhenMissingCheckBox;
    private CheckBox mUseBazinetaForkCheckBox;

    public JmbeLibraryPreferenceEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        //Register to receive directory preference update notifications so we can update the path labels
        MyEventBus.getGlobalEventBus().register(this);

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        // Section header
        Label headerLabel = new Label("JMBE Audio Library");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        // Library info card
        SettingsCard infoCard = new SettingsCard();
        infoCard.getChildren().add(new SettingsRow("Current Version", getJmbeVersionLabel()));
        infoCard.getChildren().add(new SettingsRow("File", getPathToJmbeLibraryLabel()));
        getChildren().add(infoCard);

        // Actions card with buttons and options
        SettingsCard actionsCard = new SettingsCard();
        actionsCard.getChildren().add(new SettingsRow("Library Actions", getCreateButton(), getSelectButton(), getResetButton()));
        actionsCard.getChildren().add(new SettingsRow("Alert When Missing", getAlertUserWhenMissingCheckBox()));
        actionsCard.getChildren().add(new SettingsRow("Use Bazineta Fork", getUseBazinetaForkCheckBox()));
        getChildren().add(actionsCard);
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    private CheckBox getAlertUserWhenMissingCheckBox()
    {
        if(mAlertUserWhenMissingCheckBox == null)
        {
            mAlertUserWhenMissingCheckBox = new CheckBox("Alert when decoder requires missing JMBE library");
            mAlertUserWhenMissingCheckBox.setTooltip(new Tooltip("Shows a warning notification if a digital voice channel needs the JMBE library but it is not installed."));
            mAlertUserWhenMissingCheckBox.setSelected(mUserPreferences.getJmbeLibraryPreference()
                .getAlertIfMissingLibraryRequired());
            mAlertUserWhenMissingCheckBox.setOnAction(event -> {
                boolean alert = mAlertUserWhenMissingCheckBox.isSelected();
                mUserPreferences.getJmbeLibraryPreference().setAlertIfMissingLibraryRequired(alert);
            });
        }

        return mAlertUserWhenMissingCheckBox;
    }
    private CheckBox getUseBazinetaForkCheckBox()
    {
        if(mUseBazinetaForkCheckBox == null)
        {
            mUseBazinetaForkCheckBox = new CheckBox("Use alternative 'bazineta' JMBE fork (optimized/cleaned up)");
            mUseBazinetaForkCheckBox.setTooltip(new Tooltip("Uses a highly optimized version of the JMBE library that performs better for SDRTrunk's specific needs."));
            mUseBazinetaForkCheckBox.setSelected(mUserPreferences.getJmbeLibraryPreference().getUseBazinetaFork());
            mUseBazinetaForkCheckBox.setOnAction(event -> {
                boolean useFork = mUseBazinetaForkCheckBox.isSelected();
                mUserPreferences.getJmbeLibraryPreference().setUseBazinetaFork(useFork);

                if (useFork) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "This fork is maintained for use with sdrtrunk and focuses on practical codec maintenance:\n\n" +
                            "• Lint and code quality — visibility narrowing, dead code removal, cleanup throughout the AMBE/IMBE codec paths\n" +
                            "• Removed unused code — debug wave-generation utilities, unused ambeplus package\n" +
                            "• Reduced allocation pressure — eliminated per-frame array allocations\n" +
                            "• Tuning - AMBE frames are no longer decoded twice\n" +
                            "• Bug fixes & Codec correctness fixes\n" +
                            "• Output calibration — final AMBE output is trimmed slightly before clipping\n" +
                            "• Voiced synthesis performance — replaced per-sample Math.cos() calls with incremental phasor rotation\n\n" +
                            "In short, this fork has been radically pruned to just what sdrtrunk requires and tuned to be faster than the original.", ButtonType.OK);
                    alert.setTitle("Bazineta JMBE Fork Details");
                    alert.setHeaderText("About the Bazineta JMBE Fork");
                    alert.initOwner(getCreateButton().getScene().getWindow());
                    alert.showAndWait();
                }
            });
        }

        return mUseBazinetaForkCheckBox;
    }

    private Button getCreateButton()
    {
        if(mCreateButton == null)
        {
            mCreateButton = new Button();
            mCreateButton.setText(mUserPreferences.getJmbeLibraryPreference()
                .getPathJmbeLibrary() != null ? CHECK_FOR_UPDATE : CREATE_LIBRARY);
            mCreateButton.setTooltip(new Tooltip("Downloads the latest source code and compiles a new JMBE library automatically."));
            mCreateButton.setOnAction(event -> checkForUpdatedLibrary());
        }

        return mCreateButton;
    }

    /**
     * Checks for availability of an updated JMBE library
     */
    private void checkForUpdatedLibrary()
    {
        ThreadPool.CACHED.execute(() -> {
            try
            {
                Version current = mUserPreferences.getJmbeLibraryPreference().getCurrentVersion();
                boolean useFork = mUserPreferences.getJmbeLibraryPreference().getUseBazinetaFork();
                String releasesUrl = useFork ? JmbeCreator.GITHUB_BAZINETA_JMBE_RELEASES_URL : JmbeCreator.GITHUB_JMBE_RELEASES_URL;
                final Release release = GitHub.getLatestRelease(releasesUrl);

                mLog.info("Checking for JMBE Library Updates (fork={}) ...", useFork);
                mLog.info("Current: " + (current != null ? current.toString() : "empty"));

                // For bazineta fork: if no release found (repo may not have tagged releases),
                // allow direct creation from master.zip
                if(useFork && release == null)
                {
                    mLog.info("Bazineta fork has no tagged releases - proceeding with direct build from master");
                    // Create a synthetic release for the bazineta fork
                    final Release syntheticRelease = new Release(Version.fromString("v1.0.9"), new com.google.gson.JsonObject());

                    Platform.runLater(() -> {
                        try
                        {
                            String content = "The bazineta JMBE fork will be downloaded and compiled from source. " +
                                "Would you like to proceed?";
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.YES, ButtonType.NO); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                            alert.setResizable(true);
                            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                            alert.setTitle("JMBE Library - Bazineta Fork");
                            alert.setHeaderText("Install bazineta JMBE fork");
                            alert.initOwner(getCreateButton().getScene().getWindow());
                            alert.showAndWait().ifPresent(buttonType -> {
                                if(buttonType == ButtonType.YES)
                                {
                                    MyEventBus.getGlobalEventBus().post(new JmbeEditorRequest(syntheticRelease, true));
                                }
                            });
                        }
                        catch(Throwable t)
                        {
                            mLog.error("Error during JavaFX portion of create library");
                        }
                    });
                    return;
                }

                final boolean canUpdate = (release != null) && ((current == null) ||
                    (release.getVersion().compareTo(current) > 0));

                if(release != null)
                {
                    mLog.info("Available: " + release.getVersion().toString());
                }

                if(canUpdate)
                {
                    mLog.info("JMBE Library update is available");
                }
                else
                {
                    mLog.info("No JMBE library update is available at this time");
                }

                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            if(canUpdate)
                            {
                                String content = "JMBE library version " + release.getVersion().toString() +
                                    " is available.  Would you like to download the latest source code and " +
                                    "create an updated JMBE library?";
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.YES, ButtonType.NO); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                                alert.setResizable(true);

                                //Workaroud for dialog sizing issue on Windows
                                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                                alert.setTitle("JMBE Library Update Check");
                                alert.setHeaderText("Update is available");
                                alert.initOwner(getCreateButton().getScene().getWindow());
                                alert.showAndWait().ifPresent(buttonType -> {
                                    if(buttonType == ButtonType.YES)
                                    {
                                        mLog.info("Posting JMBE editor request to event bus");
                                        MyEventBus.getGlobalEventBus().post(new JmbeEditorRequest(release, useFork));
                                    }
                                });
                            }
                            else
                            {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                    "No JMBE library update is available.", ButtonType.OK); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                                alert.setTitle("JMBE Library Update Check");
                                if(release != null)
                                {
                                    alert.setHeaderText("No update available");
                                }
                                else
                                {
                                    alert.setHeaderText("Unable to determine latest release version.  Check network connection");
                                }
                                alert.initOwner(getCreateButton().getScene().getWindow());
                                alert.showAndWait();
                            }
                        }
                        catch(Throwable t)
                        {
                            mLog.error("Error during JavaFX portion of create library");
                        }
                    }
                });
            }
            catch(Throwable t)
            {
                mLog.error("Error during create library", t);
            }
        });
    }

    private Label getJmbeVersionLabel()
    {
        if(mJmbeVersionLabel == null)
        {
            mJmbeVersionLabel = new Label();
            Version version = mUserPreferences.getJmbeLibraryPreference().getCurrentVersion();
            boolean isBazineta = mUserPreferences.getJmbeLibraryPreference().getUseBazinetaFork();

            if(version != null)
            {
                mJmbeVersionLabel.setText(version.toString() + (isBazineta ? " (Bazineta)" : ""));
            }
            else
            {
                mJmbeVersionLabel.setText(LIBRARY_NOT_SETUP);
            }
        }

        return mJmbeVersionLabel;
    }

    private Button getSelectButton()
    {
        if(mSelectButton == null)
        {
            mSelectButton = new Button("Select...");
            mSelectButton.setTooltip(new Tooltip("Browse your computer to select an existing compiled JMBE library file."));
            mSelectButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select JMBE Audio Library Location");

                    Path path = mUserPreferences.getDirectoryPreference().getDefaultJmbeDirectory();

                    if(Files.exists(path))
                    {
                        fileChooser.setInitialDirectory(path.toFile());
                    }

                    Stage stage = (Stage)getSelectButton().getScene().getWindow();
                    File selected = fileChooser.showOpenDialog(stage);

                    if(selected != null)
                    {
                        mUserPreferences.getJmbeLibraryPreference().setPathJmbeLibrary(selected.toPath());
                    }
                }
            });
        }

        return mSelectButton;
    }

    private Button getResetButton()
    {
        if(mResetButton == null)
        {
            mResetButton = new Button("Reset");
            mResetButton.setTooltip(new Tooltip("Clears the currently selected JMBE library."));
            mResetButton.setOnAction(event -> mUserPreferences.getJmbeLibraryPreference().resetPathJmbeLibrary());
        }

        return mResetButton;
    }

    private Label getPathToJmbeLibraryLabel()
    {
        if(mPathToJmbeLibraryLabel == null)
        {
            Path path = mUserPreferences.getJmbeLibraryPreference().getPathJmbeLibrary();
            mPathToJmbeLibraryLabel = new Label(path != null ? path.toString() : PATH_NOT_SET);
        }

        return mPathToJmbeLibraryLabel;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType != null && preferenceType == PreferenceType.JMBE_LIBRARY)
        {
            Path path = mUserPreferences.getJmbeLibraryPreference().getPathJmbeLibrary();
            getPathToJmbeLibraryLabel().setText(path != null ? path.toString() : PATH_NOT_SET);
            Version version = mUserPreferences.getJmbeLibraryPreference().getCurrentVersion();
            boolean isBazineta = mUserPreferences.getJmbeLibraryPreference().getUseBazinetaFork();
            getJmbeVersionLabel().setText(version != null ? version.toString() + (isBazineta ? " (Bazineta)" : "") : LIBRARY_NOT_SETUP);
            getCreateButton().setText(mUserPreferences.getJmbeLibraryPreference()
                .getPathJmbeLibrary() != null ? CHECK_FOR_UPDATE : CREATE_LIBRARY);
            getAlertUserWhenMissingCheckBox().setSelected(mUserPreferences.getJmbeLibraryPreference()
                .getAlertIfMissingLibraryRequired());
            getUseBazinetaForkCheckBox().setSelected(mUserPreferences.getJmbeLibraryPreference().getUseBazinetaFork());
        }
    }
}
