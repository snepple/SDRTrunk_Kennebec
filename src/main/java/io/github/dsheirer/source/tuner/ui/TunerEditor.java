/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.ui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.gui.control.FrequencyTextField;
import io.github.dsheirer.gui.control.FrequencyControl;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.wave.IRecordingStatusListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.SwingUtils;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;


import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;








/**
 * Base tuner configuration editor.
 */
public abstract class TunerEditor<T extends Tuner,C extends TunerConfiguration> extends VBox
        implements IDiscoveredTunerStatusListener, Listener<TunerEvent>
{
    private Logger mLog = LoggerFactory.getLogger(TunerEditor.class);

    /**
     * Single background thread for applying blocking hardware (USB) control calls off the JavaFX
     * Application Thread, plus per-control debouncing so a rapid slider/combo drag coalesces into
     * a single device write instead of freezing the UI with one libusb transfer per pixel.
     */
    private final java.util.concurrent.ScheduledExecutorService mDeviceControlExecutor =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TunerEditor-DeviceControl");
                t.setDaemon(true);
                return t;
            });
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>>
            mPendingDeviceControls = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DEVICE_CONTROL_DEBOUNCE_MS = 120;

    private static final long DEFAULT_MINIMUM_FREQUENCY = 1;
    private static final long DEFAULT_MAXIMUM_FREQUENCY = 9_999_999_999l;
    private static final String BUTTON_STATUS_ENABLE = "Enable";
    private static final String BUTTON_STATUS_DISABLE = "Disable";
    private static final long serialVersionUID = 1L;
    private UserPreferences mUserPreferences;
    private TunerManager mTunerManager;
    private DiscoveredTuner mDiscoveredTuner;
    private C mTunerConfiguration;
    // private FrequencyAndCorrectionChangeListener...
    private FrequencyControl mFrequencyControl;
    private Spinner mFrequencyCorrectionSpinner;
    private Button mEnabledButton;
    private Button mViewSpectrumButton;
    private Button mNewSpectrumButton;
    private Button mRestartTunerButton;
    private Button mGainAdvisorButton;
    private ToggleButton mRecordButton;
    private ButtonPanel mButtonsPanel;
    private FrequencyPanel mFrequencyPanel;
    private Label mTunerIdLabel;
    private CheckBox mAutoPPMCheckBox;
    private CheckBox mAutoOptimizeSampleRateCheckBox;
    private Label mMeasuredPPMLabel;
    private Label mRecordingStatusLabel;
    private Label mTunerStatusLabel;
    private Label mTunerLockedStatusLabel;
    private FrequencyTextField mMinimumFrequencyTextField;
    private FrequencyTextField mMaximumFrequencyTextField;
    private Button mResetFrequenciesButton;
    private boolean mLoading = false;
    private TextField mFriendlyNameTextField;
    private Button mInfoConfigButton;

    /**
     * Constructs an instance
     * @param tunerManager for requesting configuration saves.
     */
    public TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        mDiscoveredTuner = discoveredTuner;

        if(mDiscoveredTuner != null && mDiscoveredTuner.hasTunerConfiguration())
        {
            mTunerConfiguration = (C)mDiscoveredTuner.getTunerConfiguration();
        }

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.addTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().addTunerEventListener(this);

                if(!mDiscoveredTuner.hasTunerConfiguration())
                {
                    mLog.warn("Tuner does not have a tuner configuration ....");
                }
            }
        }
    }

    /**
     * Minimum tunable frequency supported by the tuner.
     * @return minimum frequency hertz
     */
    public abstract long getMinimumTunableFrequency();

    /**
     * Maximum tunable frequency supported by the tuner.
     * @return maximum frequency hertz
     */
    public abstract long getMaximumTunableFrequency();

    /**
     * Current sample rate for the tuner.
     * @return sample rate in hertz.
     */
    public int getCurrentSampleRate()
    {
        if(hasTuner())
        {
            return (int)getTuner().getTunerController().getSampleRate();
        }

        return 0;
    }

    /**r
     * Indicates if the controls are currently being loaded with values.
     */
    protected boolean isLoading()
    {
        return mLoading;
    }

    /**
     * Changes the loading status
     */
    protected void setLoading(boolean loading)
    {
        mLoading = loading;
    }

    /**
     * Measured PPM value received from any decoders that may be running.
     */
    protected Label getMeasuredPPMLabel()
    {
        if(mMeasuredPPMLabel == null)
        {
            mMeasuredPPMLabel = new Label("");
            mMeasuredPPMLabel.setTooltip(new javafx.scene.control.Tooltip("Displays the measured frequency error and PPM when provided by compatible channel decoders"));
        }

        return mMeasuredPPMLabel;
    }

    /**
     * Tuner locked status label that can be turned on/off depending on tuner lock state.
     */
    protected Label getTunerLockedStatusLabel()
    {
        if(mTunerLockedStatusLabel == null)
        {
            mTunerLockedStatusLabel = new Label("Channel(s) active - frequency and sample rate controls are locked");
            mTunerLockedStatusLabel.setTooltip(new javafx.scene.control.Tooltip("Indicates that the tuner is providing channel(s) and you can't change the tuner frequency or sample rate"));
            mTunerLockedStatusLabel.setVisible(false);
        }

        return mTunerLockedStatusLabel;
    }

    /**
     * Tuner status label
     */
    protected Label getTunerStatusLabel()
    {
        if(mTunerStatusLabel == null)
        {
            mTunerStatusLabel = new Label(" ");
        }

        return mTunerStatusLabel;
    }


    /**
     * Label to display current file size for a wide-band recording
     */
    protected Label getRecordingStatusLabel()
    {
        if(mRecordingStatusLabel == null)
        {
            mRecordingStatusLabel = new Label(" ");
            mRecordingStatusLabel.setTooltip(new javafx.scene.control.Tooltip("Shows the status of the latest baseband recording when active"));
            mRecordingStatusLabel.setVisible(false);
        }

        return mRecordingStatusLabel;
    }

    /**
     * Check box for enable/disable automatic PPM adjustment from decoder(s) frequency error feedback.
     */
    /**
     * Check box for enable/disable auto optimizing sample rate.
     */
    protected CheckBox getAutoOptimizeSampleRateCheckBox()
    {
        if(mAutoOptimizeSampleRateCheckBox == null)
        {
            mAutoOptimizeSampleRateCheckBox = new CheckBox("Auto-Optimize Sample Rate");
            mAutoOptimizeSampleRateCheckBox.setTooltip(new javafx.scene.control.Tooltip("Automatically adjust the tuner sample rate and center frequency to fit active channels"));
            mAutoOptimizeSampleRateCheckBox.setOnAction(e ->
            {
                if(!isLoading())
                {
                    Tuner tuner = getTuner();
                    if(tuner != null)
                    {
                        boolean enabled = getAutoOptimizeSampleRateCheckBox().isSelected();
                        getConfiguration().setAutoOptimizeSampleRate(enabled);
                        save();
                    }
                }
            });
        }

        return mAutoOptimizeSampleRateCheckBox;
    }

    protected CheckBox getAutoPPMCheckBox()
    {
        if(mAutoPPMCheckBox == null)
        {
            mAutoPPMCheckBox = new CheckBox("Enable decoder(s) to auto-adjust PPM");
            mAutoPPMCheckBox.setTooltip(new javafx.scene.control.Tooltip("Allow decoders to measure channel frequency error and correct tuner PPM"));
            mAutoPPMCheckBox.setOnAction(e ->
            {
                if(!isLoading())
                {
                    Tuner tuner = getTuner();
                    if(tuner != null)
                    {
                        boolean enabled = getAutoPPMCheckBox().isSelected();
                        getTuner().getTunerController().getFrequencyErrorCorrectionManager().setEnabled(enabled);
                        save();
                    }
                }
            });
        }

        return mAutoPPMCheckBox;
    }

    /**
     * Label for displaying the tuner ID
     */
    /**
     * Helper to retrieve the USB Bus and Port, if available
     */
    protected String getUsbInfo()
    {
        if(getDiscoveredTuner() instanceof io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner usbTuner)
        {
            return " (USB Bus: " + usbTuner.getBus() + " Port: " + usbTuner.getPortAddress() + ")";
        }
        return "";
    }

    protected Label getTunerIdLabel()
    {
        if(mTunerIdLabel == null)
        {
            mTunerIdLabel = new Label(" ");
        }

        return mTunerIdLabel;
    }


    protected TextField getFriendlyNameTextField()
    {
        if(mFriendlyNameTextField == null)
        {
            mFriendlyNameTextField = new TextField();
            mFriendlyNameTextField.setTooltip(new javafx.scene.control.Tooltip("Enter a friendly name for this tuner"));

            if(getConfiguration() != null && getConfiguration().getFriendlyName() != null)
            {
                mFriendlyNameTextField.setText(getConfiguration().getFriendlyName());
            }

            mFriendlyNameTextField.textProperty().addListener((obs, oldV, newV) -> { if (getConfiguration() != null) getConfiguration().setFriendlyName(newV); });
            //Persist the friendly name when editing finishes (focus lost or Enter) so it survives restarts
            //and propagates to the tuner list / channel "Tuner" column.
            mFriendlyNameTextField.focusedProperty().addListener((obs, was, focused) -> { if(!focused) saveConfiguration(); });
            mFriendlyNameTextField.setOnAction(e -> saveConfiguration());
        }

        return mFriendlyNameTextField;
    }

    protected FrequencyPanel getFrequencyPanel()
    {
        if(mFrequencyPanel == null)
        {
            mFrequencyPanel = new FrequencyPanel();
            // mFrequencyPanel.setTooltip(new javafx.scene.control.Tooltip("Tuner frequency and PPM controls"));
        }

        return mFrequencyPanel;
    }

    protected Spinner getFrequencyCorrectionSpinner()
    {
        if(mFrequencyCorrectionSpinner == null)
        {
            SpinnerValueFactory model = new javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory(-1000.0, 1000.0, 0.0, 0.1);
            mFrequencyCorrectionSpinner = new Spinner(model);
            mFrequencyCorrectionSpinner.setTooltip(new javafx.scene.control.Tooltip("Adjust the PPM value to compensate for tuner frequency error"));
            mFrequencyCorrectionSpinner.setDisable(!(false));
            // Spinner.NumberEditor editor = ...
            // DecimalFormat format = editor.getFormat();
            // format.setMinimumFractionDigits(1);
            // editor.getTextField().setHorizontalAlignment...
            // mFrequencyCorrectionSpinner.valueProperty().addListener(mFrequencyAndCorrectionChangeListener);
        }

        return mFrequencyCorrectionSpinner;
    }


    protected Button getInfoConfigButton()
    {
        if(mInfoConfigButton == null)
        {
            mInfoConfigButton = new Button("Info/Config");
            mInfoConfigButton.setTooltip(new javafx.scene.control.Tooltip("View tuner information"));
            mInfoConfigButton.setOnAction(e -> {
                //Build the dialog content as real nodes.  Previously this VBox was passed to
                //setContentText(String.valueOf(panel)), which rendered the object's toString
                //("VBox@...") instead of the actual content.  (The friendly-name field now lives in the
                //tuner config area, so this dialog just shows tuner information.)
                VBox panel = new VBox(10);
                panel.setPadding(new Insets(10));

                String info = getTunerInfo();
                Label infoLabel = new Label(info != null && !info.isEmpty() ? info
                        : "No additional tuner information available.");
                infoLabel.setWrapText(true);
                panel.getChildren().add(infoLabel);

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                    alert.setTitle("Tuner Info");
                    alert.setHeaderText("Tuner Information");
                    alert.getDialogPane().setContent(panel);
                    alert.showAndWait();
                });
            });
        }
        return mInfoConfigButton;
    }

    protected String getTunerInfo() {
        return "";
    }

    protected ButtonPanel getButtonPanel()
    {
        if(mButtonsPanel == null)
        {
            mButtonsPanel = new ButtonPanel();
            // mButtonsPanel.setTooltip(new javafx.scene.control.Tooltip("Button controls for the selected tuner"));
        }

        return mButtonsPanel;
    }

    protected FrequencyControl getFrequencyControl()
    {
        if(mFrequencyControl == null)
        {
            mFrequencyControl = new FrequencyControl();
        }

        return mFrequencyControl;
    }

    /**
     * Minimum frequency value text field
     */
    protected FrequencyTextField getMinimumFrequencyTextField()
    {
        if(mMinimumFrequencyTextField == null)
        {
            mMinimumFrequencyTextField = new FrequencyTextField(DEFAULT_MINIMUM_FREQUENCY, DEFAULT_MAXIMUM_FREQUENCY,
                    getMinimumTunableFrequency());
            // mMinimumFrequencyTextField.setTooltip(new javafx.scene.control.Tooltip("Sets or changes the minimum frequency value that this tuner will support."));
            final long[] minExisting = new long[1];
            mMinimumFrequencyTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    minExisting[0] = getMinimumFrequencyTextField().getFrequency();
                } else {
                    if(!isLoading())
                    {
                        setLoading(true);

                        long minimum = getMinimumFrequencyTextField().getFrequency();
                        long maximum = getMaximumFrequencyTextField().getFrequency();

                        if(minimum < getMinimumTunableFrequency())
                        {
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Frequency value [" +
                                            getMinimumFrequencyTextField().getText() + "] is below the supported frequency range for this tuner")); alert.showAndWait(); });
                            getMinimumFrequencyTextField().setFrequency(minExisting[0]);
                            return;
                        }

                        if((minimum + getCurrentSampleRate()) > maximum)
                        {
                            long newMaximum = minimum + getCurrentSampleRate();

                            if(newMaximum <= getMaximumTunableFrequency())
                            {
                                maximum = newMaximum;
                                getMaximumFrequencyTextField().setFrequency(maximum);
                            }
                            else
                            {
                                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Frequency value [" +
                                                getMinimumFrequencyTextField().getText() + "] is invalid for current sample rate " +
                                                "and maximum supported frequency for this tuner")); alert.showAndWait(); });
                                getMinimumFrequencyTextField().setFrequency(minExisting[0]);
                                return;
                            }
                        }

                        if(hasTuner())
                        {
                            getTuner().getTunerController().setFrequencyExtents(minimum, maximum);
                        }

                        adjustFrequencyControl(minimum, maximum);
                        setLoading(false);
                        save();
                    }
                }
            });
        }

        return mMinimumFrequencyTextField;
    }

    /**
     * Adjusts the frequency control to be within the min-max range.
     * @param minimum frequency value.
     * @param maximum frequency value.
     */
    private void adjustFrequencyControl(long minimum, long maximum)
    {
        if(hasTuner())
        {
            try
            {
                if(getFrequencyControl().getFrequency() < minimum)
                {
                    getTuner().getTunerController().setFrequency(minimum);
                }
                else if(getFrequencyControl().getFrequency() > maximum)
                {
                    getTuner().getTunerController().setFrequency(maximum);
                }
            }
            catch(SourceException se)
            {
                mLog.error("Error adjusting frequency", se);
            }
        }
    }

    /**
     * Adjusts the minimum and maximum frequency values to ensure the gap is wide enough for the sample rate.
     * @param sampleRate to adjust for.
     */
    protected void adjustForSampleRate(int sampleRate)
    {
        long minimum = getMinimumFrequencyTextField().getFrequency();
        long maximum = getMaximumFrequencyTextField().getFrequency();

        if(maximum - minimum < sampleRate)
        {
            long newMaximum = minimum + sampleRate;

            if(newMaximum <= getMaximumTunableFrequency())
            {
                getMaximumFrequencyTextField().setFrequency(newMaximum);
            }
            else
            {
                long newMinimum = maximum - sampleRate;

                if(newMinimum >= getMinimumTunableFrequency())
                {
                    getMinimumFrequencyTextField().setFrequency(newMinimum);
                }
                else
                {
                    Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Unable to adjust tuner's " +
                            "minimum and maximum frequency values to accommodate new sample rate [" + sampleRate + "]")); alert.showAndWait(); });
                }
            }
        }
    }

    /**
     * Maximum frequency value text field
     */
    protected FrequencyTextField getMaximumFrequencyTextField()
    {
        if(mMaximumFrequencyTextField == null)
        {
            mMaximumFrequencyTextField = new FrequencyTextField(DEFAULT_MINIMUM_FREQUENCY, DEFAULT_MAXIMUM_FREQUENCY,
                    getMaximumTunableFrequency());
            // mMaximumFrequencyTextField.setTooltip(new javafx.scene.control.Tooltip("Sets or changes the maximum frequency value that this tuner will support."));
            final long[] maxExisting = new long[1];
            mMaximumFrequencyTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    maxExisting[0] = getMaximumFrequencyTextField().getFrequency();
                } else {
                    if(!isLoading())
                    {

                        setLoading(true);
                        long minimum = getMinimumFrequencyTextField().getFrequency();
                        long maximum = getMaximumFrequencyTextField().getFrequency();

                        if(maximum > getMaximumTunableFrequency())
                        {
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Frequency value [" +
                                    getMaximumFrequencyTextField().getText() + "] is above the supported frequency " +
                                    "range for this tuner")); alert.showAndWait(); });
                            getMaximumFrequencyTextField().setFrequency(maxExisting[0]);
                            return;
                        }

                        if((maximum - getCurrentSampleRate()) < minimum)
                        {
                            long newMinimum = maximum - getCurrentSampleRate();

                            if(newMinimum >= getMinimumTunableFrequency())
                            {
                                minimum = newMinimum;
                                getMinimumFrequencyTextField().setFrequency(minimum);
                            }
                            else
                            {
                                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Frequency value [" +
                                                getMaximumFrequencyTextField().getText() + "] is invalid for current sample rate " +
                                                "and minimum supported frequency for this tuner")); alert.showAndWait(); });
                                getMaximumFrequencyTextField().setFrequency(maxExisting[0]);
                                return;
                            }
                        }

                        if(hasTuner())
                        {
                            getTuner().getTunerController().setFrequencyExtents(minimum, maximum);
                        }

                        adjustFrequencyControl(minimum, maximum);
                        setLoading(false);
                        save();
                    }
                }
            });
        }

        return mMaximumFrequencyTextField;
    }

    /**
     * Resets the minimum and maximum frequency values.
     */
    protected Button getResetFrequenciesButton()
    {
        if(mResetFrequenciesButton == null)
        {
            mResetFrequenciesButton = new Button("Reset");
            mResetFrequenciesButton.setOnAction(e -> {

                long min = getMinimumTunableFrequency();
                long max = getMaximumTunableFrequency();
                getTuner().getTunerController().setFrequencyExtents(min, max);
                getMinimumFrequencyTextField().setFrequency(min);
                getMaximumFrequencyTextField().setFrequency(max);
                save();
            });
        }

        return mResetFrequenciesButton;
    }

    /**
     * Button requesting to show tuner in new spectral display
     */
    protected Button getNewSpectrumButton()
    {
        if(mNewSpectrumButton == null)
        {
            mNewSpectrumButton = new Button("New Spectrum Display");
            mNewSpectrumButton.setTooltip(new javafx.scene.control.Tooltip("Show this tuner in a new (separate) spectral display window"));
            mNewSpectrumButton.setOnAction(e ->
            {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                            TunerEvent.Event.REQUEST_NEW_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mNewSpectrumButton;
    }

    /**
     * Button requesting to show tuner in main spectral display
     */
    protected Button getViewSpectrumButton()
    {
        if(mViewSpectrumButton == null)
        {
            mViewSpectrumButton = new Button("View Spectrum");
            mViewSpectrumButton.setTooltip(new javafx.scene.control.Tooltip("Show this tuner in the spectral display"));
            mViewSpectrumButton.setOnAction(e ->
            {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    SystemProperties.getInstance().set(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true);

                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                            TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mViewSpectrumButton;
    }

    /**
     * Button to change enable, disable, or error restart status.
     */
    protected Button getEnabledButton()
    {
        if(mEnabledButton == null)
        {
            mEnabledButton = new Button(BUTTON_STATUS_ENABLE);
            mEnabledButton.setTooltip(new javafx.scene.control.Tooltip("Enable or disable the tuner for use by sdrtrunk"));
            mEnabledButton.setOnAction(e ->
            {
                switch(getEnabledButton().getText())
                {
                    case BUTTON_STATUS_DISABLE:
                        mLog.info("Disabling " + getDiscoveredTuner().getTunerClass() + " tuner");
                        getDiscoveredTuner().setEnabled(false);
                        break;
                    case BUTTON_STATUS_ENABLE:
                        if (getDiscoveredTuner() instanceof io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner usbTuner) {
                            if (BandwidthMonitor.willExceedThreshold(mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners(), usbTuner)) {
                                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.source.tuner.ui.USBAlertEvent("USB bus overload detected. Enabling this tuner will push the USB bus beyond the 30 MB/s soft ceiling. Please reduce the sample rate of specific tuners on this overloaded bus or move one or more tuners to a different physical USB port on your computer."));
                            } else {
                                mLog.info("Enabling " + getDiscoveredTuner().getTunerClass() + " tuner");
                                getDiscoveredTuner().setEnabled(true);
                            }
                        } else {
                            if (getDiscoveredTuner() instanceof io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner) {
                                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.source.tuner.ui.USBAlertEvent("Note: USB bus monitoring is not available for SDRplay devices."));
                            }
                            mLog.info("Enabling " + getDiscoveredTuner().getTunerClass() + " tuner");
                            getDiscoveredTuner().setEnabled(true);
                        }
                        break;
                    default:
                        mLog.info("None matched");
                }
            });
        }

        return mEnabledButton;
    }

    protected Button getRestartTunerButton()
    {
        if(mRestartTunerButton == null)
        {
            mRestartTunerButton = new Button("Restart Tuner");
            mRestartTunerButton.setVisible(false);
            mRestartTunerButton.setTooltip(new javafx.scene.control.Tooltip("Attempt to restart this tuner to recover from error condition"));
            mRestartTunerButton.setOnAction(e ->
            {
                if(!hasTuner() && getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
                {
                    mLog.info("Restarting " + getDiscoveredTuner().getTunerClass() + " tuner");
                    getDiscoveredTuner().restart();
                }
            });
        }

        return mRestartTunerButton;
    }

    /**
     * On-demand Adaptive Gain Advisor button.  Runs the advisor immediately and shows its gain
     * recommendation for the currently active channels (Gemini-assisted when AI is configured,
     * otherwise a local heuristic summary).
     */
    protected Button getGainAdvisorButton()
    {
        if(mGainAdvisorButton == null)
        {
            mGainAdvisorButton = new Button("Gain Advisor");
            mGainAdvisorButton.setTooltip(new javafx.scene.control.Tooltip(
                    "Run the Adaptive Gain Advisor now and show a tuner gain recommendation based on " +
                    "current I/Q signal levels across active channels."));
            mGainAdvisorButton.setOnAction(e ->
            {
                long minFreq = 0;
                long maxFreq = Long.MAX_VALUE;
                try
                {
                    if(hasTuner())
                    {
                        //Limit the advisor to channels within this tuner's currently tuned passband.
                        minFreq = getTuner().getTunerController().getMinTunedFrequency();
                        maxFreq = getTuner().getTunerController().getMaxTunedFrequency();
                    }
                }
                catch(Exception ex)
                {
                    minFreq = 0;
                    maxFreq = Long.MAX_VALUE;
                }

                mGainAdvisorButton.setDisable(true);
                io.github.dsheirer.source.tuner.manager.AdaptiveGainAdvisor.getInstance(mUserPreferences)
                        .requestManualConsultation(minFreq, maxFreq,
                                recommendation -> Platform.runLater(() -> {
                                    mGainAdvisorButton.setDisable(false);
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                                    alert.setTitle("Adaptive Gain Advisor");
                                    alert.setHeaderText("Gain recommendation");
                                    javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(recommendation);
                                    area.setEditable(false);
                                    area.setWrapText(true);
                                    area.setPrefSize(520, 220);
                                    alert.getDialogPane().setContent(area);
                                    alert.showAndWait();
                                }),
                                message -> Platform.runLater(() -> {
                                    mGainAdvisorButton.setDisable(false);
                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                    io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                                    alert.setTitle("Adaptive Gain Advisor");
                                    alert.setHeaderText("No recommendation available");
                                    alert.setContentText(message);
                                    alert.showAndWait();
                                }));
            });
        }

        return mGainAdvisorButton;
    }

    /**
     * Turns off the recorder, if it's currently recording.
     */
    protected void turnOffRecorder()
    {
        if(getRecordButton().isSelected())
        {
            getRecordButton().setSelected(false);
        }
    }

    /**
     * Records the tuner's wide-band sample stream.
     */
    protected ToggleButton getRecordButton()
    {
        if(mRecordButton == null)
        {
            mRecordButton = new ToggleButton("Record");
            mRecordButton.setTooltip(new javafx.scene.control.Tooltip("Create a baseband recording for this tuner"));
            mRecordButton.setDisable(!(false));
            mRecordButton.setOnAction(e ->
            {
                if(hasTuner())
                {
                    if(getRecordButton().isSelected())
                    {
                        getRecordingStatusLabel().setVisible(true);
                        getRecordingStatusLabel().setText(" ");
                        getTuner().getTunerController().startRecorder(mUserPreferences, new RecordingStatusListener(),
                                getDiscoveredTuner().getTunerClass().name());
                    }
                    else
                    {
                        getTuner().getTunerController().stopRecorder();
                    }
                }
            });
        }

        return mRecordButton;
    }

    protected abstract void save();

    /**
     * Access the tuner configuration
     */
    protected C getConfiguration()
    {
        return mTunerConfiguration;
    }

    /**
     * Indicates if this editor has a tuner configuration
     */
    protected boolean hasConfiguration()
    {
        return mTunerConfiguration != null;
    }

    /**
     * Discovered tuner for this editor
     */
    protected DiscoveredTuner getDiscoveredTuner()
    {
        return mDiscoveredTuner;
    }

    /**
     * Notification that the status of the discovered tuner has changed and that the editor implementation must
     * refresh the UI controls with the current tuner state.
     */
    protected abstract void tunerStatusUpdated();

    /**
     * Implements the tuner status listener interface to send notification to editor implementations that the
     * status of the discovered tuner has changed.
     * @param discoveredTuner that has a status change.
     * @param previous tuner status
     * @param current tuner status
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        SwingUtils.run(() ->
        {
            tunerStatusUpdated();

            if(current == TunerStatus.ENABLED)
            {
                if(hasTuner())
                {
                    getTuner().addTunerEventListener(this);
                }
            }
        });
    }

    @Override
    public void receive(TunerEvent tunerEvent)
    {
        switch(tunerEvent.getEvent())
        {
            //Note: called methods are responsible for executing on the swing thread.
            case UPDATE_MEASURED_FREQUENCY_ERROR -> getFrequencyPanel().updateFrequencyError();
            case UPDATE_FREQUENCY_ERROR -> getFrequencyPanel().updatePPM();
        }
    }

    /**
     * Prepare this editor for disposal
     */
    public void dispose()
    {
        mDeviceControlExecutor.shutdownNow();
        turnOffRecorder();

        getFrequencyControl().clearListeners();
        // getFrequencyCorrectionSpinner().removeChangeListener(mFrequencyAndCorrectionChangeListener);

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.removeTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().removeTunerEventListener(this);
            }
        }
    }

    /**
     * Indicates if the discovered tuner has a usable tuner
     */
    public boolean hasTuner()
    {
        return mDiscoveredTuner != null && mDiscoveredTuner.hasTuner();
    }

    /**
     * Access the usable tuner from the discovered tuner
     * Note: use hasTuner() to check before invoking this method.
     * @return tuner or null.
     */
    public T getTuner()
    {
        if(hasTuner())
        {
            return (T)mDiscoveredTuner.getTuner();
        }

        return null;
    }

    /**
     * Request to save the state of this configuration.
     */
    protected void saveConfiguration()
    {
        mTunerManager.saveConfigurations();
    }

    /**
     * Action that performs a blocking hardware control call.  Unlike {@link Runnable} its method
     * may throw a checked exception (device setters declare {@code UsbException},
     * {@code SDRPlayException}, etc.); {@link #applyDeviceControl} catches and reports it.
     */
    @FunctionalInterface
    protected interface DeviceAction
    {
        void run() throws Exception;
    }

    /**
     * Applies a blocking hardware (USB) control action on a background thread, debounced per
     * control key so rapid slider/combo changes during a drag coalesce into a single device
     * write.  This keeps the JavaFX Application Thread responsive — blocking libusb control
     * transfers must never run on the FX thread.
     *
     * Read JavaFX control values and call save() on the FX thread BEFORE invoking this; the
     * supplied action should only perform the device call and must not touch JavaFX controls.
     *
     * @param controlKey unique identifier for the control (e.g. "lna-gain"); successive calls
     *                   with the same key cancel any still-pending write for that control
     * @param deviceAction blocking device call to run on the background thread
     * @param errorMessage user-facing message logged and shown if the action throws
     */
    protected void applyDeviceControl(String controlKey, DeviceAction deviceAction, String errorMessage)
    {
        java.util.concurrent.ScheduledFuture<?> previous = mPendingDeviceControls.get(controlKey);

        if(previous != null)
        {
            previous.cancel(false);
        }

        try
        {
            java.util.concurrent.ScheduledFuture<?> future = mDeviceControlExecutor.schedule(() -> {
                mPendingDeviceControls.remove(controlKey);

                if(!hasTuner())
                {
                    return;
                }

                try
                {
                    deviceAction.run();
                }
                catch(Exception e)
                {
                    mLog.error(errorMessage, e);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                        alert.setContentText(errorMessage);
                        alert.showAndWait();
                    });
                }
            }, DEVICE_CONTROL_DEBOUNCE_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

            mPendingDeviceControls.put(controlKey, future);
        }
        catch(java.util.concurrent.RejectedExecutionException ree)
        {
            //Editor disposed - executor shut down; ignore the late control change.
        }
    }

    /**
     * Sets the lock state for the tuner so that the frequency and sample rate controls can be enabled/disabled.
     *
     * Note: implementing classes should invoke: getFrequencyPanel().updateControls() method.
     *
     * @param locked true if the tuner is locked.
     */
    public abstract void setTunerLockState(boolean locked);

    /**
     * Tuner buttons panel
     */
    public class ButtonPanel extends VBox
    {
        /**
         * Constructs an instance
         */
                public ButtonPanel()
        {
            setSpacing(6);

            //Friendly name lives here in the config area so it's discoverable; it appears in the tuner list
            //Name column and the channel "Tuner" column once set.
            HBox nameRow = new HBox(6);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            getFriendlyNameTextField().setPrefWidth(220);
            getFriendlyNameTextField().setPromptText("Friendly name (shown in tuner & channel lists)");
            nameRow.getChildren().addAll(new Label("Name:"), getFriendlyNameTextField());

            HBox row1 = new HBox(6);
            row1.setAlignment(Pos.CENTER_LEFT);
            row1.getChildren().addAll(getEnabledButton(), getRecordButton(), getViewSpectrumButton(), getNewSpectrumButton());

            HBox row2 = new HBox(6);
            row2.setAlignment(Pos.CENTER_LEFT);
            row2.getChildren().addAll(getInfoConfigButton(), getGainAdvisorButton(), getRestartTunerButton());

            getChildren().addAll(nameRow, row1, row2, getRecordingStatusLabel());
        }

        /**
         * Updates the state and text of the buttons based on the tuner status.
         */
        public void updateControls()
        {
            TunerStatus tunerStatus = getDiscoveredTuner().getTunerStatus();

            getRecordButton().setDisable(!(tunerStatus.isAvailable()) && getDiscoveredTuner().hasTuner());
            getRecordingStatusLabel().setText(" ");
            getViewSpectrumButton().setDisable(!(tunerStatus.isAvailable()) && getDiscoveredTuner().hasTuner());
            getNewSpectrumButton().setDisable(!(tunerStatus.isAvailable()) && getDiscoveredTuner().hasTuner());
            getRestartTunerButton().setVisible(tunerStatus == TunerStatus.ERROR);

            if(getDiscoveredTuner().isEnabled())
            {
                getEnabledButton().setText(BUTTON_STATUS_DISABLE);
            }
            else
            {
                getEnabledButton().setText(BUTTON_STATUS_ENABLE);
            }
        }
    }

    /**
     * Sub panel that displays frequency control, ppm spinner and control, and tuner locked status label.
     */
    public class FrequencyPanel extends VBox
    {
        public FrequencyPanel()
        {
            setSpacing(6);
            setPadding(new javafx.geometry.Insets(4, 0, 4, 0));

            getChildren().add(getFrequencyControl());

            // PPM row: label + help + spinner + measured value
            HBox ppmRow = new HBox(6);
            ppmRow.setAlignment(Pos.CENTER_LEFT);
            Button helpButton = createHelpIcon("?");
            helpButton.setTooltip(new javafx.scene.control.Tooltip("PPM (Parts Per Million): Adjusts your tuner to match the exact frequency. If your hardware gets warm and signals shift, adjust this until the signal is centered."));
            ppmRow.getChildren().addAll(new Label("PPM:"), helpButton, getFrequencyCorrectionSpinner(), getMeasuredPPMLabel());
            getChildren().add(ppmRow);

            HBox autoPpmBox = new HBox(4, getAutoPPMCheckBox(), createHelpIcon("?"));
            ((Button)autoPpmBox.getChildren().get(1)).setTooltip(new javafx.scene.control.Tooltip("Allow decoders to automatically measure channel frequency error and adjust the tuner PPM setting above."));
            autoPpmBox.setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(autoPpmBox, getAutoOptimizeSampleRateCheckBox());

            // Min / Max frequency range grid
            GridPane minMaxGrid = new GridPane();
            minMaxGrid.setHgap(8);
            minMaxGrid.setVgap(4);
            minMaxGrid.add(new Label("Min:"), 0, 0);
            minMaxGrid.add(getMinimumFrequencyTextField(), 1, 0);
            minMaxGrid.add(new Label("Max:"), 0, 1);
            minMaxGrid.add(getMaximumFrequencyTextField(), 1, 1);
            HBox resetBox = new HBox(getResetFrequenciesButton());
            minMaxGrid.add(resetBox, 1, 2);
            getChildren().add(minMaxGrid);

            getChildren().add(getTunerLockedStatusLabel());
        }

        /**
         * Update the state of the frequency panel controls
         */
        public void updateControls()
        {
            getFrequencyControl().clearListeners();
            // getFrequencyControl().addListener(mFrequencyAndCorrectionChangeListener);
            boolean hasTunerUnlocked = hasTuner() && !getTuner().getTunerController().isLockedSampleRate();
            getFrequencyControl().setDisable(!(hasTunerUnlocked));
            getMinimumFrequencyTextField().setDisable(!(hasTunerUnlocked));
            getMaximumFrequencyTextField().setDisable(!(hasTunerUnlocked));
            getResetFrequenciesButton().setDisable(!(hasTunerUnlocked));
            getTunerLockedStatusLabel().setVisible(hasTuner() && getTuner().getTunerController().isLockedSampleRate());
            getFrequencyCorrectionSpinner().setDisable(!(hasTuner()));
            getAutoPPMCheckBox().setDisable(!(hasTuner()));
            getAutoOptimizeSampleRateCheckBox().setDisable(!(hasTuner()) && getTuner().getTunerController() instanceof io.github.dsheirer.source.tuner.ISampleRateConfigurable);

            Tuner tuner = getTuner();

            if(tuner != null)
            {
                getFrequencyControl().setFrequency(tuner.getTunerController().getFrequency(), false);
                getMinimumFrequencyTextField().setFrequency(tuner.getTunerController().getMinimumFrequency());
                getMaximumFrequencyTextField().setFrequency(tuner.getTunerController().getMaximumFrequency());
                getFrequencyCorrectionSpinner().getValueFactory().setValue(tuner.getTunerController().getFrequencyCorrection());
                getAutoPPMCheckBox().setSelected(tuner.getTunerController().getFrequencyErrorCorrectionManager().isEnabled());
                getAutoOptimizeSampleRateCheckBox().setSelected(getConfiguration().isAutoOptimizeSampleRate());
                getFrequencyControl().addListener(getTuner().getTunerController());
                getTuner().getTunerController().addListener(getFrequencyControl());
                getMeasuredPPMLabel().setText(tuner.getTunerController().getMeasuredErrorStatus());
            }
            else
            {
                getFrequencyControl().setFrequency(0, false);
                getFrequencyCorrectionSpinner().getValueFactory().setValue(0);
                getAutoPPMCheckBox().setSelected(false);
                getAutoOptimizeSampleRateCheckBox().setSelected(false);
                getMeasuredPPMLabel().setText("");
            }
        }

        /**
         * Updates the measured frequency error label
         */
        public void updateFrequencyError()
        {
            SwingUtils.run(() ->
            {
                if(hasTuner())
                {
                    getMeasuredPPMLabel().setText(getTuner().getTunerController().getMeasuredErrorStatus());
                }
                else
                {
                    getMeasuredPPMLabel().setText("");
                }
            });
        }

        /**
         * Updates or refreshes the displayed tuner PPM setting.
         */
        public void updatePPM()
        {
            SwingUtils.run(() -> {
                setLoading(true);
                getFrequencyCorrectionSpinner().getValueFactory().setValue(getTuner().getTunerController().getFrequencyCorrection());
                setLoading(false);
            });
        }
    }

    /**
     * Implements status listener to receive updates for wide-band recordings
     */
    public class RecordingStatusListener implements IRecordingStatusListener
    {
        private DecimalFormat mSizeFormat = new DecimalFormat("0.0");

        @Override
        public void update(int fileCount, String file, long size)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Recording Size: ").append(humanReadableByteCount(size));
            sb.append(" File #").append(fileCount).append(": ").append(file);
            final String status = sb.toString();
            Platform.runLater(() -> getRecordingStatusLabel().setText(status));
        }

        public static String humanReadableByteCount(long bytes) {
            long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
            if (absB < 1024) {
                return bytes + " B";
            }
            long value = absB;
            CharacterIterator ci = new StringCharacterIterator("KMGTPE");
            for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
                value >>= 10;
                ci.next();
            }
            value *= Long.signum(bytes);
            return String.format("%.1f %cB", value / 1024.0, ci.current());
        }
    }

    /**
//      * Monitors the frequency correction spinner for changed value.
//      */
//     private class FrequencyAndCorrectionChangeListener implements ChangeListener, ISourceEventProcessor
//     {
//         //This monitors the frequency correction spinner, applies the changes to the tuner, and saves configuration.
//         // // @Override
//         public void stateChanged(ChangeEvent e)
//         {
//             final double value = (Double) getFrequencyCorrectionSpinner().getValue();
// 
//             if(hasTuner() && !isLoading())
//             {
//                 try
//                 {
//                     getTuner().getTunerController().setFrequencyCorrection(value);
//                 }
//                 catch(SourceException e1)
//                 {
//                     mLog.error("Error setting frequency correction value", e1);
//                 }
// 
//                 save();
//             }
//         }
// 
//         //This monitors the frequency control and saves configuration.
//         // @Override
//         // public void process(SourceEvent event) throws SourceException
//         // {
//             // if(hasTuner() && !isLoading())
//             // {
//                 save();
//             }
//         }
//     }
// 
    private Button createHelpIcon(String text) {
        Button button = new Button(text);
        button.setPadding(new javafx.geometry.Insets(0, 2, 0, 2));
        // button.setFocusPainted(false);
        // button.setContentAreaFilled(false);
        button.setBackground(javafx.scene.layout.Background.EMPTY);
        return button;
    }
}

// }
