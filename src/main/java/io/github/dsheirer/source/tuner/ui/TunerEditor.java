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

import io.github.dsheirer.gui.playlist.source.FrequencyField;
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
import io.github.dsheirer.util.ThreadPool;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base tuner configuration editor.
 */
public abstract class TunerEditor<T extends Tuner,C extends TunerConfiguration> extends JFXPanel
        implements IDiscoveredTunerStatusListener, Listener<TunerEvent>, ITunerEditor
{
    private Logger mLog = LoggerFactory.getLogger(TunerEditor.class);
    private static final String BUTTON_STATUS_ENABLE = "Enable";
    private static final String BUTTON_STATUS_DISABLE = "Disable";
    private static final long serialVersionUID = 1L;
    private UserPreferences mUserPreferences;
    private TunerManager mTunerManager;
    private DiscoveredTuner mDiscoveredTuner;
    private C mTunerConfiguration;
    private FrequencyField mFrequencyControl;
    private Spinner<Double> mFrequencyCorrectionSpinner;
    private Button mEnabledButton;
    private Button mViewSpectrumButton;
    private Button mNewSpectrumButton;
    private Button mRestartTunerButton;
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
    private FrequencyField mMinimumFrequencyTextField;
    private FrequencyField mMaximumFrequencyTextField;
    private Button mResetFrequenciesButton;
    private boolean mLoading = false;
    private TextField mFriendlyNameTextField;
    private Button mInfoConfigButton;

    private long mMinimumFrequencyCache = 0;
    private long mMaximumFrequencyCache = 0;
    private long mFrequencyCache = 0;

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

    public abstract long getMinimumTunableFrequency();
    public abstract long getMaximumTunableFrequency();

    public int getCurrentSampleRate()
    {
        if(hasTuner())
        {
            return (int)getTuner().getTunerController().getSampleRate();
        }

        return 0;
    }

    protected boolean isLoading()
    {
        return mLoading;
    }

    protected void setLoading(boolean loading)
    {
        mLoading = loading;
    }

    protected Label getMeasuredPPMLabel()
    {
        if(mMeasuredPPMLabel == null)
        {
            mMeasuredPPMLabel = new Label("");
            mMeasuredPPMLabel.setTooltip(new Tooltip("Displays the measured frequency error and PPM when provided by compatible channel decoders"));
        }

        return mMeasuredPPMLabel;
    }

    protected Label getTunerLockedStatusLabel()
    {
        if(mTunerLockedStatusLabel == null)
        {
            mTunerLockedStatusLabel = new Label("Channel(s) active - frequency and sample rate controls are locked");
            mTunerLockedStatusLabel.setTooltip(new Tooltip("Indicates that the tuner is providing channel(s) and you can't " +
                    "change the tuner frequency or sample rate"));
            mTunerLockedStatusLabel.setVisible(false);
            mTunerLockedStatusLabel.setManaged(false);
            mTunerLockedStatusLabel.visibleProperty().addListener((obs, oldV, newV) -> mTunerLockedStatusLabel.setManaged(newV));
        }

        return mTunerLockedStatusLabel;
    }

    protected Label getTunerStatusLabel()
    {
        if(mTunerStatusLabel == null)
        {
            mTunerStatusLabel = new Label(" ");
        }

        return mTunerStatusLabel;
    }

    protected Label getRecordingStatusLabel()
    {
        if(mRecordingStatusLabel == null)
        {
            mRecordingStatusLabel = new Label(" ");
            mRecordingStatusLabel.setTooltip(new Tooltip("Shows the status of the latest baseband recording when active"));
            mRecordingStatusLabel.setVisible(false);
            mRecordingStatusLabel.setManaged(false);
            mRecordingStatusLabel.visibleProperty().addListener((obs, oldV, newV) -> mRecordingStatusLabel.setManaged(newV));
        }

        return mRecordingStatusLabel;
    }

    protected CheckBox getAutoOptimizeSampleRateCheckBox()
    {
        if(mAutoOptimizeSampleRateCheckBox == null)
        {
            mAutoOptimizeSampleRateCheckBox = new CheckBox("Auto-Optimize Sample Rate");
            mAutoOptimizeSampleRateCheckBox.setTooltip(new Tooltip("Automatically adjust the tuner sample rate and center frequency to fit active channels"));
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
            mAutoPPMCheckBox.setTooltip(new Tooltip("Allow decoders to measure channel frequency error and correct tuner PPM"));
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
            mFriendlyNameTextField.setPrefColumnCount(20);
            mFriendlyNameTextField.setTooltip(new Tooltip("Enter a friendly name for this tuner"));

            if(getConfiguration() != null && getConfiguration().getFriendlyName() != null)
            {
                mFriendlyNameTextField.setText(getConfiguration().getFriendlyName());
            }

            mFriendlyNameTextField.textProperty().addListener((obs, oldText, newText) -> {
                if(!mLoading && getConfiguration() != null) {
                    getConfiguration().setFriendlyName(newText);
                    mTunerManager.getTunerConfigurationManager().saveConfigurations();
                    if (getDiscoveredTuner() != null) {
                        io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent(getConfiguration(), io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent.Event.CHANGE));
                    }
                }
            });
        }

        return mFriendlyNameTextField;
    }

    protected FrequencyPanel getFrequencyPanel()
    {
        if(mFrequencyPanel == null)
        {
            mFrequencyPanel = new FrequencyPanel();
        }
        return mFrequencyPanel;
    }

    protected Spinner<Double> getFrequencyCorrectionSpinner()
    {
        if(mFrequencyCorrectionSpinner == null)
        {
            mFrequencyCorrectionSpinner = new Spinner<>();
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(-1000.0, 1000.0, 0.0, 0.1);
            mFrequencyCorrectionSpinner.setValueFactory(factory);
            mFrequencyCorrectionSpinner.setTooltip(new Tooltip("Adjust the PPM value to compensate for tuner frequency error"));
            mFrequencyCorrectionSpinner.setDisable(true);
            mFrequencyCorrectionSpinner.setEditable(true);

            mFrequencyCorrectionSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getTunerController().setFrequencyCorrection(newValue);
                    }
                    catch(SourceException e1)
                    {
                        mLog.error("Error setting frequency correction value", e1);
                    }
                    save();
                }
            });
        }

        return mFrequencyCorrectionSpinner;
    }

    protected Button getInfoConfigButton()
    {
        if(mInfoConfigButton == null)
        {
            mInfoConfigButton = new Button("Info/Config");
            mInfoConfigButton.setOnAction(e -> {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Tuner Info/Config");
                    alert.setHeaderText(null);
                    VBox vBox = new VBox(5);
                    String info = getTunerInfo();
                    if(info != null && !info.isEmpty()) {
                        vBox.getChildren().add(new Label(info));
                    }
                    vBox.getChildren().add(new Label("Friendly Name:"));
                    vBox.getChildren().add(getFriendlyNameTextField());
                    alert.getDialogPane().setContent(vBox);
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
        }
        return mButtonsPanel;
    }

    protected FrequencyField getFrequencyControl()
    {
        if(mFrequencyControl == null)
        {
            mFrequencyControl = new FrequencyField();
            mFrequencyControl.focusedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    mFrequencyCache = mFrequencyControl.get();
                } else if (!isLoading() && hasTuner()) {
                    long freq = mFrequencyControl.get();
                    if (freq != mFrequencyCache) {
                        try {
                            getTuner().getTunerController().setFrequency(freq);
                            save();
                        } catch (SourceException ex) {
                            mLog.error("Error setting tuner frequency", ex);
                            mFrequencyControl.set(mFrequencyCache);
                        }
                    }
                }
            });
        }

        return mFrequencyControl;
    }

    protected FrequencyField getMinimumFrequencyTextField()
    {
        if(mMinimumFrequencyTextField == null)
        {
            mMinimumFrequencyTextField = new FrequencyField();
            mMinimumFrequencyTextField.set(getMinimumTunableFrequency());
            mMinimumFrequencyTextField.setTooltip(new Tooltip("Sets or changes the minimum frequency value that this tuner will support."));
            mMinimumFrequencyTextField.focusedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    mMinimumFrequencyCache = mMinimumFrequencyTextField.get();
                } else {
                    if(!isLoading())
                    {
                        setLoading(true);

                        long minimum = mMinimumFrequencyTextField.get();
                        long maximum = getMaximumFrequencyTextField().get();

                        if(minimum < getMinimumTunableFrequency())
                        {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(AlertType.ERROR);
                                alert.setTitle("Invalid Frequency");
                                alert.setHeaderText(null);
                                alert.setContentText("Frequency value [" +
                                                mMinimumFrequencyTextField.getText() + "] is below the supported frequency range for this tuner");
                                alert.showAndWait();
                                mMinimumFrequencyTextField.set(mMinimumFrequencyCache);
                            });
                            return;
                        }

                        if((minimum + getCurrentSampleRate()) > maximum)
                        {
                            long newMaximum = minimum + getCurrentSampleRate();

                            if(newMaximum <= getMaximumTunableFrequency())
                            {
                                maximum = newMaximum;
                                final long finalMax = maximum;
                                getMaximumFrequencyTextField().set(finalMax);
                            }
                            else
                            {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(AlertType.ERROR);
                                    alert.setTitle("Invalid Frequency");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Frequency value [" +
                                                    mMinimumFrequencyTextField.getText() + "] is invalid for current sample rate " +
                                                    "and maximum supported frequency for this tuner");
                                    alert.showAndWait();
                                    mMinimumFrequencyTextField.set(mMinimumFrequencyCache);
                                });
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

    private void adjustFrequencyControl(long minimum, long maximum)
    {
        if(hasTuner())
        {
            try
            {
                if(getFrequencyControl().get() < minimum)
                {
                    getTuner().getTunerController().setFrequency(minimum);
                }
                else if(getFrequencyControl().get() > maximum)
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

    protected void adjustForSampleRate(int sampleRate)
    {
        long minimum = getMinimumFrequencyTextField().get();
        long maximum = getMaximumFrequencyTextField().get();

        if(maximum - minimum < sampleRate)
        {
            long newMaximum = minimum + sampleRate;

            if(newMaximum <= getMaximumTunableFrequency())
            {
                getMaximumFrequencyTextField().set(newMaximum);
            }
            else
            {
                long newMinimum = maximum - sampleRate;

                if(newMinimum >= getMinimumTunableFrequency())
                {
                    getMinimumFrequencyTextField().set(newMinimum);
                }
                else
                {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Frequency Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Unable to adjust tuner's " +
                                "minimum and maximum frequency values to accommodate new sample rate [" + sampleRate + "]");
                        alert.showAndWait();
                    });
                }
            }
        }
    }

    protected FrequencyField getMaximumFrequencyTextField()
    {
        if(mMaximumFrequencyTextField == null)
        {
            mMaximumFrequencyTextField = new FrequencyField();
            mMaximumFrequencyTextField.set(getMaximumTunableFrequency());
            mMaximumFrequencyTextField.setTooltip(new Tooltip("Sets or changes the maximum frequency value that this tuner will support."));
            mMaximumFrequencyTextField.focusedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    mMaximumFrequencyCache = mMaximumFrequencyTextField.get();
                } else {
                    if(!isLoading())
                    {
                        setLoading(true);
                        long minimum = getMinimumFrequencyTextField().get();
                        long maximum = mMaximumFrequencyTextField.get();

                        if(maximum > getMaximumTunableFrequency())
                        {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(AlertType.ERROR);
                                alert.setTitle("Invalid Frequency");
                                alert.setHeaderText(null);
                                alert.setContentText("Frequency value [" +
                                        mMaximumFrequencyTextField.getText() + "] is above the supported frequency " +
                                        "range for this tuner");
                                alert.showAndWait();
                                mMaximumFrequencyTextField.set(mMaximumFrequencyCache);
                            });
                            return;
                        }

                        if((maximum - getCurrentSampleRate()) < minimum)
                        {
                            long newMinimum = maximum - getCurrentSampleRate();

                            if(newMinimum >= getMinimumTunableFrequency())
                            {
                                minimum = newMinimum;
                                final long finalMin = minimum;
                                getMinimumFrequencyTextField().set(finalMin);
                            }
                            else
                            {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(AlertType.ERROR);
                                    alert.setTitle("Invalid Frequency");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Frequency value [" +
                                                    mMaximumFrequencyTextField.getText() + "] is invalid for current sample rate " +
                                                    "and minimum supported frequency for this tuner");
                                    alert.showAndWait();
                                    mMaximumFrequencyTextField.set(mMaximumFrequencyCache);
                                });
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

    protected Button getResetFrequenciesButton()
    {
        if(mResetFrequenciesButton == null)
        {
            mResetFrequenciesButton = new Button("Reset");
            mResetFrequenciesButton.setOnAction(e -> {
                long min = getMinimumTunableFrequency();
                long max = getMaximumTunableFrequency();
                getTuner().getTunerController().setFrequencyExtents(min, max);
                getMinimumFrequencyTextField().set(min);
                getMaximumFrequencyTextField().set(max);
                save();
            });
        }

        return mResetFrequenciesButton;
    }

    protected Button getNewSpectrumButton()
    {
        if(mNewSpectrumButton == null)
        {
            mNewSpectrumButton = new Button("New Spectrum Display");
            mNewSpectrumButton.setTooltip(new Tooltip("Show this tuner in a new (separate) spectral display window"));
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

    protected Button getViewSpectrumButton()
    {
        if(mViewSpectrumButton == null)
        {
            mViewSpectrumButton = new Button("View Spectrum");
            mViewSpectrumButton.setTooltip(new Tooltip("Show this tuner in the spectral display"));
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

    protected Button getEnabledButton()
    {
        if(mEnabledButton == null)
        {
            mEnabledButton = new Button(BUTTON_STATUS_ENABLE);
            mEnabledButton.setTooltip(new Tooltip("Enable or disable the tuner for use by sdrtrunk"));
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
                            if (io.github.dsheirer.source.tuner.ui.BandwidthMonitor.willExceedThreshold(mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners(), usbTuner)) {
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
            mRestartTunerButton.setManaged(false);
            mRestartTunerButton.visibleProperty().addListener((obs, oldV, newV) -> mRestartTunerButton.setManaged(newV));
            mRestartTunerButton.setTooltip(new Tooltip("Attempt to restart this tuner to recover from error condition"));
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

    protected void turnOffRecorder()
    {
        if(getRecordButton().isSelected())
        {
            getRecordButton().setSelected(false);
        }
    }

    protected ToggleButton getRecordButton()
    {
        if(mRecordButton == null)
        {
            mRecordButton = new ToggleButton("Record");
            mRecordButton.setTooltip(new Tooltip("Create a baseband recording for this tuner"));
            mRecordButton.setDisable(true);
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

    protected C getConfiguration()
    {
        return mTunerConfiguration;
    }

    protected boolean hasConfiguration()
    {
        return mTunerConfiguration != null;
    }

    protected DiscoveredTuner getDiscoveredTuner()
    {
        return mDiscoveredTuner;
    }

    protected abstract void tunerStatusUpdated();

    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        Platform.runLater(() ->
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
            case UPDATE_MEASURED_FREQUENCY_ERROR -> getFrequencyPanel().updateFrequencyError();
            case UPDATE_FREQUENCY_ERROR -> getFrequencyPanel().updatePPM();
        }
    }

    public void dispose()
    {
        Platform.runLater(this::turnOffRecorder);

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.removeTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().removeTunerEventListener(this);
            }
        }
    }

    public boolean hasTuner()
    {
        return mDiscoveredTuner != null && mDiscoveredTuner.hasTuner();
    }

    public T getTuner()
    {
        if(hasTuner())
        {
            return (T)mDiscoveredTuner.getTuner();
        }

        return null;
    }

    protected void saveConfiguration()
    {
        mTunerManager.saveConfigurations();
    }

    public abstract void setTunerLockState(boolean locked);

    public class ButtonPanel extends VBox
    {
        public ButtonPanel()
        {
            setSpacing(5);
            HBox row1 = new HBox(5);
            row1.getChildren().addAll(getEnabledButton(), getRecordButton(), getViewSpectrumButton(), getNewSpectrumButton());

            HBox row2 = new HBox(5);
            row2.getChildren().addAll(getInfoConfigButton(), getRestartTunerButton());

            getChildren().addAll(row1, row2, getRecordingStatusLabel());
        }

        public void updateControls()
        {
            TunerStatus tunerStatus = getDiscoveredTuner().getTunerStatus();

            getRecordButton().setDisable(!(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner()));
            getRecordingStatusLabel().setText(" ");
            getViewSpectrumButton().setDisable(!(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner()));
            getNewSpectrumButton().setDisable(!(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner()));
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

    public class FrequencyPanel extends VBox
    {
        public FrequencyPanel()
        {
            setSpacing(5);

            HBox topRow = new HBox(5, getFrequencyControl());
            topRow.setAlignment(Pos.CENTER_LEFT);

            HBox ppmRow = new HBox(5);
            ppmRow.setAlignment(Pos.CENTER_LEFT);
            ppmRow.getChildren().add(new Label("PPM:"));
            Button helpButton = createHelpIcon("?");
            helpButton.setTooltip(new Tooltip("PPM (Parts Per Million): Adjusts your tuner to match the exact frequency. If your hardware gets warm and signals shift, adjust this until the signal is centered."));
            ppmRow.getChildren().add(helpButton);
            ppmRow.getChildren().add(getFrequencyCorrectionSpinner());
            ppmRow.getChildren().add(getMeasuredPPMLabel());

            HBox minMaxRow = new HBox(5);
            minMaxRow.setAlignment(Pos.CENTER_LEFT);
            minMaxRow.getChildren().add(new Label("Min:"));
            minMaxRow.getChildren().add(getMinimumFrequencyTextField());
            minMaxRow.getChildren().add(new Label("Max:"));
            minMaxRow.getChildren().add(getMaximumFrequencyTextField());
            minMaxRow.getChildren().add(getResetFrequenciesButton());

            getChildren().addAll(topRow, ppmRow, getAutoPPMCheckBox(), getAutoOptimizeSampleRateCheckBox(), minMaxRow, getTunerLockedStatusLabel());
        }

        public void updateControls()
        {
            boolean hasTunerUnlocked = hasTuner() && !getTuner().getTunerController().isLockedSampleRate();
            getFrequencyControl().setDisable(!hasTunerUnlocked);
            getMinimumFrequencyTextField().setDisable(!hasTunerUnlocked);
            getMaximumFrequencyTextField().setDisable(!hasTunerUnlocked);
            getResetFrequenciesButton().setDisable(!hasTunerUnlocked);
            getTunerLockedStatusLabel().setVisible(hasTuner() && getTuner().getTunerController().isLockedSampleRate());
            getFrequencyCorrectionSpinner().setDisable(!hasTuner());
            getAutoPPMCheckBox().setDisable(!hasTuner());
            getAutoOptimizeSampleRateCheckBox().setDisable(!(hasTuner() && getTuner().getTunerController() instanceof io.github.dsheirer.source.tuner.ISampleRateConfigurable));

            Tuner tuner = getTuner();

            if(tuner != null)
            {
                getFrequencyControl().set(tuner.getTunerController().getFrequency());
                getMinimumFrequencyTextField().set(tuner.getTunerController().getMinimumFrequency());
                getMaximumFrequencyTextField().set(tuner.getTunerController().getMaximumFrequency());

                getFrequencyCorrectionSpinner().getValueFactory().setValue(tuner.getTunerController().getFrequencyCorrection());
                getAutoPPMCheckBox().setSelected(tuner.getTunerController().getFrequencyErrorCorrectionManager().isEnabled());
                getAutoOptimizeSampleRateCheckBox().setSelected(getConfiguration().isAutoOptimizeSampleRate());
                getMeasuredPPMLabel().setText(tuner.getTunerController().getMeasuredErrorStatus());
            }
            else
            {
                getFrequencyControl().set(0);
                getFrequencyCorrectionSpinner().getValueFactory().setValue(0.0);
                getAutoPPMCheckBox().setSelected(false);
                getAutoOptimizeSampleRateCheckBox().setSelected(false);
                getMeasuredPPMLabel().setText("");
            }
        }

        public void updateFrequencyError()
        {
            Platform.runLater(() ->
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

        public void updatePPM()
        {
            Platform.runLater(() -> {
                setLoading(true);
                getFrequencyCorrectionSpinner().getValueFactory().setValue(getTuner().getTunerController().getFrequencyCorrection());
                setLoading(false);
            });
        }
    }

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

    private Button createHelpIcon(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: transparent; -fx-padding: 0 2 0 2;");
        return button;
    }
}
