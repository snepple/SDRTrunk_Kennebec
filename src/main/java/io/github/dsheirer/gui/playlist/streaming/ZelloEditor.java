/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.audio.broadcast.zello.ZelloBroadcaster;
import io.github.dsheirer.audio.broadcast.zello.ZelloConfiguration;
import io.github.dsheirer.gui.control.IntegerTextField;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zello Work channel streaming configuration editor.
 *
 * Fields:
 * - Name: Display name for this stream configuration
 * - Network Name: Zello Work subdomain (e.g., "actionpage" for actionpage.zellowork.com)
 * - Channel: Zello channel name to stream audio to
 * - Username: Zello account username
 * - Password: Zello account password
 * - Max Recording Age: Maximum age of audio recording before it is discarded
 */
public class ZelloEditor extends AbstractBroadcastEditor<ZelloConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(ZelloEditor.class);

    private TextField mNetworkNameTextField;
    private TextField mChannelTextField;
    private TextField mUsernameTextField;
    private PasswordField mPasswordField;
    private TextField mUnmaskedPasswordField;
    private ToggleButton mShowPasswordButton;
    private IntegerTextField mMaxAgeTextField;
    private IntegerTextField mStreamGuardTextField;
    private IntegerTextField mPauseTimeTextField;
    private IntegerTextField mRelaxationTimeTextField;
    private Button mTestConnectionButton;
    private Label mTestConnectionResult;
    private GridPane mEditorPane;

    /**
     * Constructs an instance
     * @param playlistManager for accessing the broadcast model
     */
    public ZelloEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public void setItem(ZelloConfiguration item)
    {
        super.setItem(item);

        //Never leave the password revealed when loading/clearing a configuration (navigating away).
        getShowPasswordButton().setSelected(false);

        getNetworkNameTextField().setDisable(item == null);
        getChannelTextField().setDisable(item == null);
        getUsernameTextField().setDisable(item == null);
        getPasswordField().setDisable(item == null);
        getMaxAgeTextField().setDisable(item == null);
        getStreamGuardTextField().setDisable(item == null);
        getPauseTimeTextField().setDisable(item == null);
        getRelaxationTimeTextField().setDisable(item == null);
        getTestConnectionButton().setDisable(item == null);
        getTestConnectionResult().setText(null);

        if(item != null)
        {
            getNetworkNameTextField().setText(item.getNetworkName());
            getChannelTextField().setText(item.getChannel());
            getUsernameTextField().setText(item.getUsername());
            getPasswordField().setText(item.getPassword());
            getMaxAgeTextField().set((int)(item.getMaximumRecordingAge() / 1000));
            getStreamGuardTextField().set(item.getStreamGuardMs());
            getPauseTimeTextField().set(item.getPauseTimeMs());
            getRelaxationTimeTextField().set(item.getRelaxationTimeMs());
        }
        else
        {
            getNetworkNameTextField().setText(null);
            getChannelTextField().setText(null);
            getUsernameTextField().setText(null);
            getPasswordField().setText(null);
            getMaxAgeTextField().set(0);
            getStreamGuardTextField().set(0);
            getPauseTimeTextField().set(0);
            getRelaxationTimeTextField().set(700);
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
            //Trim the free-text identity fields. Stray leading/trailing whitespace (e.g. a trailing space in a
            //channel name) does not match the Zello channel and produces a runtime "[3003] channel is not ready"
            //rejection that is hard to diagnose, so normalize it here at save time.
            getItem().setNetworkName(trimToNull(getNetworkNameTextField().getText()));
            getItem().setChannel(trimToNull(getChannelTextField().getText()));
            getItem().setUsername(trimToNull(getUsernameTextField().getText()));
            getItem().setPassword(getPasswordField().getText());
            getItem().setMaximumRecordingAge(getMaxAgeTextField().get() * 1000);
            getItem().setStreamGuardMs(getStreamGuardTextField().get());
            getItem().setPauseTimeMs(getPauseTimeTextField().get());
            getItem().setRelaxationTimeMs(getRelaxationTimeTextField().get());
        }

        //Re-hide the password once the configuration is saved.
        getShowPasswordButton().setSelected(false);

        super.save();
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.ZELLO_WORK;
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


            // Row 1: Name (display name for this stream config)
            Label nameLabel = new Label("Name");
            GridPane.setHalignment(nameLabel, HPos.RIGHT);
            GridPane.setConstraints(nameLabel, 0, ++row);
            mEditorPane.getChildren().add(nameLabel);

            GridPane.setConstraints(getNameTextField(), 1, row);
            mEditorPane.getChildren().add(getNameTextField());

            // Row 2: Network Name
            Label networkLabel = new Label("Zello Work Network");
            GridPane.setHalignment(networkLabel, HPos.RIGHT);
            GridPane.setConstraints(networkLabel, 0, ++row);
            mEditorPane.getChildren().add(networkLabel);

            GridPane.setConstraints(getNetworkNameTextField(), 1, row);
            mEditorPane.getChildren().add(getNetworkNameTextField());

            Label networkHint = new Label(".zellowork.com");
            GridPane.setHalignment(networkHint, HPos.LEFT);
            GridPane.setConstraints(networkHint, 2, row);
            mEditorPane.getChildren().add(networkHint);

            // Row 3: Channel
            Label channelLabel = new Label("Channel");
            GridPane.setHalignment(channelLabel, HPos.RIGHT);
            GridPane.setConstraints(channelLabel, 0, ++row);
            mEditorPane.getChildren().add(channelLabel);

            GridPane.setConstraints(getChannelTextField(), 1, row);
            mEditorPane.getChildren().add(getChannelTextField());

            // Row 4: Username
            Label usernameLabel = new Label("Username");
            GridPane.setHalignment(usernameLabel, HPos.RIGHT);
            GridPane.setConstraints(usernameLabel, 0, ++row);
            mEditorPane.getChildren().add(usernameLabel);

            GridPane.setConstraints(getUsernameTextField(), 1, row);
            mEditorPane.getChildren().add(getUsernameTextField());

            // Row 5: Password
            Label passwordLabel = new Label("Password");
            GridPane.setHalignment(passwordLabel, HPos.RIGHT);
            GridPane.setConstraints(passwordLabel, 0, ++row);
            mEditorPane.getChildren().add(passwordLabel);

            StackPane passwordStack = new StackPane(getPasswordField(), getUnmaskedPasswordField());
            passwordStack.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(passwordStack, Priority.ALWAYS);
            HBox passwordBox = new HBox(4, passwordStack, getShowPasswordButton());
            passwordBox.setAlignment(Pos.CENTER_LEFT);
            GridPane.setConstraints(passwordBox, 1, row);
            mEditorPane.getChildren().add(passwordBox);

            // Row: Test Connection button + result message
            GridPane.setConstraints(getTestConnectionButton(), 1, ++row);
            mEditorPane.getChildren().add(getTestConnectionButton());

            GridPane.setConstraints(getTestConnectionResult(), 1, ++row, 2, 1);
            mEditorPane.getChildren().add(getTestConnectionResult());

            // Row 6: Max Recording Age
            Label maxAgeLabel = new Label("Max Recording Age (seconds)");
            GridPane.setHalignment(maxAgeLabel, HPos.RIGHT);
            GridPane.setConstraints(maxAgeLabel, 0, ++row);
            mEditorPane.getChildren().add(maxAgeLabel);

            GridPane.setConstraints(getMaxAgeTextField(), 1, row);
            mEditorPane.getChildren().add(getMaxAgeTextField());

            // Row 7: Stream Guard Timeout
            Label streamGuardLabel = new Label("Stream Guard (ms)");
            GridPane.setHalignment(streamGuardLabel, HPos.RIGHT);
            GridPane.setConstraints(streamGuardLabel, 0, ++row);
            mEditorPane.getChildren().add(streamGuardLabel);

            GridPane.setConstraints(getStreamGuardTextField(), 1, row);
            mEditorPane.getChildren().add(getStreamGuardTextField());

            Label streamGuardHint = new Label("Min gap between streams (0 = disabled)");
            GridPane.setConstraints(streamGuardHint, 2, row, 2, 1);
            mEditorPane.getChildren().add(streamGuardHint);

            // Row 8: Pause Time
            Label pauseTimeLabel = new Label("Pause Time (ms)");
            GridPane.setHalignment(pauseTimeLabel, HPos.RIGHT);
            GridPane.setConstraints(pauseTimeLabel, 0, ++row);
            mEditorPane.getChildren().add(pauseTimeLabel);

            GridPane.setConstraints(getPauseTimeTextField(), 1, row);
            mEditorPane.getChildren().add(getPauseTimeTextField());

            Label pauseTimeHint = new Label("Delay between transmissions (0 = off)");
            GridPane.setConstraints(pauseTimeHint, 2, row, 2, 1);
            mEditorPane.getChildren().add(pauseTimeHint);

            // Row 9: Relaxation Time
            Label relaxationLabel = new Label("Relaxation Time (ms)");
            GridPane.setHalignment(relaxationLabel, HPos.RIGHT);
            GridPane.setConstraints(relaxationLabel, 0, ++row);
            mEditorPane.getChildren().add(relaxationLabel);

            GridPane.setConstraints(getRelaxationTimeTextField(), 1, row);
            mEditorPane.getChildren().add(getRelaxationTimeTextField());

            Label relaxationHint = new Label("Hold-over before ending stream (0 = off)");
            GridPane.setConstraints(relaxationHint, 2, row, 2, 1);
            mEditorPane.getChildren().add(relaxationHint);
        }

        return mEditorPane;
    }

    private TextField getNetworkNameTextField()
    {
        if(mNetworkNameTextField == null)
        {
            mNetworkNameTextField = new TextField();
            mNetworkNameTextField.setDisable(true);
            mNetworkNameTextField.setPrefWidth(300);
            mNetworkNameTextField.setPromptText("e.g., actionpage");
            mNetworkNameTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mNetworkNameTextField;
    }

    private TextField getChannelTextField()
    {
        if(mChannelTextField == null)
        {
            mChannelTextField = new TextField();
            mChannelTextField.setDisable(true);
            mChannelTextField.setPrefWidth(300);
            mChannelTextField.setPromptText("Zello channel name");
            mChannelTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mChannelTextField;
    }

    private TextField getUsernameTextField()
    {
        if(mUsernameTextField == null)
        {
            mUsernameTextField = new TextField();
            mUsernameTextField.setDisable(true);
            mUsernameTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mUsernameTextField;
    }

    private PasswordField getPasswordField()
    {
        if(mPasswordField == null)
        {
            mPasswordField = new PasswordField();
            mPasswordField.setDisable(true);
            mPasswordField.textProperty().addListener(mEditorModificationListener);
            mPasswordField.setMaxWidth(Double.MAX_VALUE);
            //Masked field is shown unless the eye toggle is selected.
            mPasswordField.visibleProperty().bind(getShowPasswordButton().selectedProperty().not());
            mPasswordField.managedProperty().bind(getShowPasswordButton().selectedProperty().not());
        }
        return mPasswordField;
    }

    /**
     * Plain-text view of the password, shown in place of the masked field while the eye toggle is on.
     * Shares the password text so edits in either field stay in sync.
     */
    private TextField getUnmaskedPasswordField()
    {
        if(mUnmaskedPasswordField == null)
        {
            mUnmaskedPasswordField = new TextField();
            mUnmaskedPasswordField.setMaxWidth(Double.MAX_VALUE);
            mUnmaskedPasswordField.textProperty().bindBidirectional(getPasswordField().textProperty());
            mUnmaskedPasswordField.disableProperty().bind(getPasswordField().disableProperty());
            mUnmaskedPasswordField.visibleProperty().bind(getShowPasswordButton().selectedProperty());
            mUnmaskedPasswordField.managedProperty().bind(getShowPasswordButton().selectedProperty());
        }
        return mUnmaskedPasswordField;
    }

    /**
     * Eye toggle that reveals the password in plain text. It is reset (re-hidden) whenever a
     * configuration is loaded/cleared (setItem) or saved, so the password is never left exposed
     * after the user saves or navigates away.
     */
    private ToggleButton getShowPasswordButton()
    {
        if(mShowPasswordButton == null)
        {
            mShowPasswordButton = new ToggleButton();
            SVGPath eye = new SVGPath();
            eye.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5C21.27 7.61 17 4.5 " +
                    "12 4.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 " +
                    "3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
            eye.setScaleX(0.7);
            eye.setScaleY(0.7);
            mShowPasswordButton.setGraphic(eye);
            mShowPasswordButton.setFocusTraversable(false);
            mShowPasswordButton.setTooltip(new Tooltip("Show/hide the password"));
        }
        return mShowPasswordButton;
    }

    private IntegerTextField getMaxAgeTextField()
    {
        if(mMaxAgeTextField == null)
        {
            mMaxAgeTextField = new IntegerTextField();
            mMaxAgeTextField.setDisable(true);
            mMaxAgeTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mMaxAgeTextField;
    }

    private IntegerTextField getStreamGuardTextField()
    {
        if(mStreamGuardTextField == null)
        {
            mStreamGuardTextField = new IntegerTextField();
            mStreamGuardTextField.setDisable(true);
            mStreamGuardTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mStreamGuardTextField;
    }

    private IntegerTextField getPauseTimeTextField()
    {
        if(mPauseTimeTextField == null)
        {
            mPauseTimeTextField = new IntegerTextField();
            mPauseTimeTextField.setDisable(true);
            mPauseTimeTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mPauseTimeTextField;
    }

    private IntegerTextField getRelaxationTimeTextField()
    {
        if(mRelaxationTimeTextField == null)
        {
            mRelaxationTimeTextField = new IntegerTextField();
            mRelaxationTimeTextField.setDisable(true);
            mRelaxationTimeTextField.textProperty().addListener(mEditorModificationListener);
        }
        return mRelaxationTimeTextField;
    }

    /**
     * Label that shows the most recent Test Connection result.
     */
    private Label getTestConnectionResult()
    {
        if(mTestConnectionResult == null)
        {
            mTestConnectionResult = new Label();
            mTestConnectionResult.setWrapText(true);
            mTestConnectionResult.setMaxWidth(420);
        }
        return mTestConnectionResult;
    }

    /**
     * Button that validates the entered network/username/password and confirms the channel comes online, using the
     * same Zello Work logon path as live streaming.  Runs off the JavaFX thread; the result is shown in an adjacent
     * label.  This catches the common mistakes - wrong network/credentials, or a mistyped channel name (including
     * stray spaces) that otherwise surfaces only at runtime as a "[3003] channel is not ready" rejection.
     */
    private Button getTestConnectionButton()
    {
        if(mTestConnectionButton == null)
        {
            mTestConnectionButton = new Button("Test Connection");
            mTestConnectionButton.setDisable(true);
            mTestConnectionButton.setOnAction(e -> testConnection());
        }
        return mTestConnectionButton;
    }

    /**
     * Builds a throwaway configuration from the current (trimmed) field values and runs a Zello logon probe on a
     * background thread, then displays the outcome.
     */
    private void testConnection()
    {
        //Snapshot the current field values into a temporary configuration (trimmed, matching what save() stores).
        ZelloConfiguration test = new ZelloConfiguration();
        test.setNetworkName(trimToNull(getNetworkNameTextField().getText()));
        test.setChannel(trimToNull(getChannelTextField().getText()));
        test.setUsername(trimToNull(getUsernameTextField().getText()));
        test.setPassword(getPasswordField().getText());

        getTestConnectionButton().setDisable(true);
        getTestConnectionResult().setText("Testing…");

        ThreadPool.CACHED.submit(() -> {
            String result;
            try
            {
                result = ZelloBroadcaster.testConnection(test);
            }
            catch(Exception ex)
            {
                result = "Test failed: " + ex.getMessage();
            }

            final String message = result;
            Platform.runLater(() -> {
                getTestConnectionResult().setText(message);
                //Re-enable only if a configuration is still loaded in the editor.
                getTestConnectionButton().setDisable(getItem() == null);
            });
        });
    }

    /**
     * Trims the value and returns null when the result is empty, so blank fields are stored consistently as null.
     */
    private static String trimToNull(String value)
    {
        if(value == null)
        {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
