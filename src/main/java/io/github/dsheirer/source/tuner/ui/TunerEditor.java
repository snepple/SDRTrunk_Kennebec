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

import io.github.dsheirer.gui.control.FrequencyTextField;
import io.github.dsheirer.gui.control.JFrequencyControl;
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
import java.awt.EventQueue;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Base tuner configuration editor.
 */
public abstract class TunerEditor<T extends Tuner,C extends TunerConfiguration> extends javafx.scene.layout.VBox
        implements IDiscoveredTunerStatusListener, Listener<TunerEvent> {
    protected javafx.embed.swing.SwingNode wrapSwingNode(javax.swing.JComponent component) {
        javafx.embed.swing.SwingNode node = new javafx.embed.swing.SwingNode();
        javax.swing.SwingUtilities.invokeLater(() -> node.setContent(component));
        javafx.scene.layout.VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
        return node;
    }

    private javax.swing.JPanel swingCompatPanel;

    protected void setLayout(java.awt.LayoutManager layout) {
        if (swingCompatPanel == null) {
            swingCompatPanel = new javax.swing.JPanel();
            javafx.embed.swing.SwingNode swingNode = new javafx.embed.swing.SwingNode();
            javax.swing.SwingUtilities.invokeLater(() -> swingNode.setContent(swingCompatPanel));
            javafx.scene.layout.VBox.setVgrow(swingNode, javafx.scene.layout.Priority.ALWAYS);
            this.getChildren().add(swingNode);
        }
        swingCompatPanel.setLayout(layout);
    }


    protected void add(javafx.scene.Node node, Object constraints) {
        if (swingCompatPanel != null) {
            javafx.embed.swing.JFXPanel jfxPanel = new javafx.embed.swing.JFXPanel();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(node);
                jfxPanel.setScene(new javafx.scene.Scene(root));
            });
            swingCompatPanel.add(jfxPanel, constraints);
        }
    }

    protected void add(javafx.scene.Node node) {
        if (swingCompatPanel != null) {
            javafx.embed.swing.JFXPanel jfxPanel = new javafx.embed.swing.JFXPanel();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(node);
                jfxPanel.setScene(new javafx.scene.Scene(root));
            });
            swingCompatPanel.add(jfxPanel);
        }
    }

    protected void add(java.awt.Component comp) {
        if (swingCompatPanel != null) {
            swingCompatPanel.add(comp);
        }
    }

    protected void add(java.awt.Component comp, Object constraints) {
        if (swingCompatPanel != null) {
            swingCompatPanel.add(comp, constraints);
        }
    }

    private Logger mLog = LoggerFactory.getLogger(TunerEditor.class);
    private static final long DEFAULT_MINIMUM_FREQUENCY = 1;
    private static final long DEFAULT_MAXIMUM_FREQUENCY = 9_999_999_999l;
    private static final String BUTTON_STATUS_ENABLE = "Enable";
    private static final String BUTTON_STATUS_DISABLE = "Disable";
    private static final long serialVersionUID = 1L;
    private UserPreferences mUserPreferences;
    private TunerManager mTunerManager;
    private DiscoveredTuner mDiscoveredTuner;
    private C mTunerConfiguration;
    private FrequencyAndCorrectionChangeListener mFrequencyAndCorrectionChangeListener = new FrequencyAndCorrectionChangeListener();
    private JFrequencyControl mFrequencyControl;
    private JSpinner mFrequencyCorrectionSpinner;
    private JButton mEnabledButton;
    private JButton mViewSpectrumButton;
    private JButton mNewSpectrumButton;
    private JButton mRestartTunerButton;
    private JToggleButton mRecordButton;
    private ButtonPanel mButtonsPanel;
    private FrequencyPanel mFrequencyPanel;
    private JLabel mTunerIdLabel;
    private JCheckBox mAutoPPMCheckBox;
    private JCheckBox mAutoOptimizeSampleRateCheckBox;
    private JLabel mMeasuredPPMLabel;
    private JLabel mRecordingStatusLabel;
    private JLabel mTunerStatusLabel;
    private JLabel mTunerLockedStatusLabel;
    private FrequencyTextField mMinimumFrequencyTextField;
    private FrequencyTextField mMaximumFrequencyTextField;
    private JButton mResetFrequenciesButton;
    private boolean mLoading = false;
    private JTextField mFriendlyNameTextField;
    private JButton mInfoConfigButton;

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
    protected JLabel getMeasuredPPMLabel()
    {
        if(mMeasuredPPMLabel == null)
        {
            mMeasuredPPMLabel = new JLabel("");
            mMeasuredPPMLabel.setToolTipText("Displays the measured frequency error and PPM when provided by compatible channel decoders");
        }

        return mMeasuredPPMLabel;
    }

    /**
     * Tuner locked status label that can be turned on/off depending on tuner lock state.
     */
    protected JLabel getTunerLockedStatusLabel()
    {
        if(mTunerLockedStatusLabel == null)
        {
            mTunerLockedStatusLabel = new JLabel("Channel(s) active - frequency and sample rate controls are locked");
            mTunerLockedStatusLabel.setToolTipText("Indicates that the tuner is providing channel(s) and you can't " +
                    "change the tuner frequency or sample rate");
            mTunerLockedStatusLabel.setVisible(false);
        }

        return mTunerLockedStatusLabel;
    }

    /**
     * Tuner status label
     */
    protected JLabel getTunerStatusLabel()
    {
        if(mTunerStatusLabel == null)
        {
            mTunerStatusLabel = new JLabel(" ");
        }

        return mTunerStatusLabel;
    }


    /**
     * Label to display current file size for a wide-band recording
     */
    protected JLabel getRecordingStatusLabel()
    {
        if(mRecordingStatusLabel == null)
        {
            mRecordingStatusLabel = new JLabel(" ");
            mRecordingStatusLabel.setToolTipText("Shows the status of the latest baseband recording when active");
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
    protected JCheckBox getAutoOptimizeSampleRateCheckBox()
    {
        if(mAutoOptimizeSampleRateCheckBox == null)
        {
            mAutoOptimizeSampleRateCheckBox = new JCheckBox("Auto-Optimize Sample Rate");
            mAutoOptimizeSampleRateCheckBox.setToolTipText("Automatically adjust the tuner sample rate and center frequency to fit active channels");
            mAutoOptimizeSampleRateCheckBox.addActionListener(e ->
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

    protected JCheckBox getAutoPPMCheckBox()
    {
        if(mAutoPPMCheckBox == null)
        {
            mAutoPPMCheckBox = new JCheckBox("Enable decoder(s) to auto-adjust PPM");
            mAutoPPMCheckBox.setToolTipText("Allow decoders to measure channel frequency error and correct tuner PPM");
            mAutoPPMCheckBox.addActionListener(e ->
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

    protected JLabel getTunerIdLabel()
    {
        if(mTunerIdLabel == null)
        {
            mTunerIdLabel = new JLabel(" ");
        }

        return mTunerIdLabel;
    }


    protected JTextField getFriendlyNameTextField()
    {
        if(mFriendlyNameTextField == null)
        {
            mFriendlyNameTextField = new JTextField(20);
            mFriendlyNameTextField.setToolTipText("Enter a friendly name for this tuner");

            if(getConfiguration() != null && getConfiguration().getFriendlyName() != null)
            {
                mFriendlyNameTextField.setText(getConfiguration().getFriendlyName());
            }

            mFriendlyNameTextField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { updateName(); }
                @Override
                public void removeUpdate(DocumentEvent e) { updateName(); }
                @Override
                public void changedUpdate(DocumentEvent e) { updateName(); }

                private void updateName() {
                    if(!mLoading && getConfiguration() != null) {
                        getConfiguration().setFriendlyName(mFriendlyNameTextField.getText());
                        mTunerManager.getTunerConfigurationManager().saveConfigurations();
                        // Trigger UI update
                        if (getDiscoveredTuner() != null) {
                            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent(getConfiguration(), io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent.Event.CHANGE));
                        }
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
            javafx.application.Platform.runLater(() -> javafx.scene.control.Tooltip.install(mFrequencyPanel, new javafx.scene.control.Tooltip("Tuner frequency and PPM controls")));
        }

        return mFrequencyPanel;
    }

    protected JSpinner getFrequencyCorrectionSpinner()
    {
        if(mFrequencyCorrectionSpinner == null)
        {
            SpinnerModel model = new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1);
            mFrequencyCorrectionSpinner = new JSpinner(model);
            mFrequencyCorrectionSpinner.setToolTipText("Adjust the PPM value to compensate for tuner frequency error");
            mFrequencyCorrectionSpinner.setEnabled(false);
            JSpinner.NumberEditor editor = (JSpinner.NumberEditor) mFrequencyCorrectionSpinner.getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(1);
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            mFrequencyCorrectionSpinner.addChangeListener(mFrequencyAndCorrectionChangeListener);
        }

        return mFrequencyCorrectionSpinner;
    }


    protected JButton getInfoConfigButton()
    {
        if(mInfoConfigButton == null)
        {
            mInfoConfigButton = new JButton("Info/Config");
            mInfoConfigButton.addActionListener(e -> {
                JPanel panel = new JPanel(new MigLayout("insets 0", "[][grow,fill]", ""));
                String info = getTunerInfo();
                if(info != null && !info.isEmpty()) {
                    panel.add(new JLabel(info), "span, wrap");
                }
                panel.add(new JLabel("Friendly Name:"));
                panel.add(getFriendlyNameTextField());
                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setContentText(String.valueOf(panel)); alert.showAndWait(); });
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
            javafx.application.Platform.runLater(() -> javafx.scene.control.Tooltip.install(mButtonsPanel, new javafx.scene.control.Tooltip("Button controls for the selected tuner")));
        }

        return mButtonsPanel;
    }

    protected JFrequencyControl getFrequencyControl()
    {
        if(mFrequencyControl == null)
        {
            mFrequencyControl = new JFrequencyControl();
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
            mMinimumFrequencyTextField.setToolTipText("Sets or changes the minimum frequency value that this tuner will support.");
            mMinimumFrequencyTextField.addFocusListener(new FocusListener()
            {
                private long mExistingFrequency;

                @Override
                public void focusGained(FocusEvent e)
                {
                    mExistingFrequency = getMinimumFrequencyTextField().getFrequency();
                }

                @Override
                public void focusLost(FocusEvent e)
                {
                    if(!isLoading())
                    {
                        setLoading(true);

                        long minimum = getMinimumFrequencyTextField().getFrequency();
                        long maximum = getMaximumFrequencyTextField().getFrequency();

                        if(minimum < getMinimumTunableFrequency())
                        {
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setContentText(String.valueOf("Frequency value [" +
                                            getMinimumFrequencyTextField().getText() + "] is below the supported frequency range for this tuner")); alert.showAndWait(); });
                            getMinimumFrequencyTextField().setFrequency(mExistingFrequency);
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
                                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setContentText(String.valueOf("Frequency value [" +
                                                getMinimumFrequencyTextField().getText() + "] is invalid for current sample rate " +
                                                "and maximum supported frequency for this tuner")); alert.showAndWait(); });
                                getMinimumFrequencyTextField().setFrequency(mExistingFrequency);
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
                    Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setContentText(String.valueOf("Unable to adjust tuner's " +
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
            mMaximumFrequencyTextField.setToolTipText("Sets or changes the maximum frequency value that this tuner will support.");
            mMaximumFrequencyTextField.addFocusListener(new FocusListener()
            {
                private long mExistingFrequency;

                @Override
                public void focusGained(FocusEvent e)
                {
                    mExistingFrequency = getMaximumFrequencyTextField().getFrequency();
                }

                @Override
                public void focusLost(FocusEvent e)
                {
                    if(!isLoading())
                    {

                        setLoading(true);
                        long minimum = getMinimumFrequencyTextField().getFrequency();
                        long maximum = getMaximumFrequencyTextField().getFrequency();

                        if(maximum > getMaximumTunableFrequency())
                        {
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setContentText(String.valueOf("Frequency value [" +
                                    getMaximumFrequencyTextField().getText() + "] is above the supported frequency " +
                                    "range for this tuner")); alert.showAndWait(); });
                            getMaximumFrequencyTextField().setFrequency(mExistingFrequency);
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
                                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); alert.setContentText(String.valueOf("Frequency value [" +
                                                getMaximumFrequencyTextField().getText() + "] is invalid for current sample rate " +
                                                "and minimum supported frequency for this tuner")); alert.showAndWait(); });
                                getMaximumFrequencyTextField().setFrequency(mExistingFrequency);
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
    protected JButton getResetFrequenciesButton()
    {
        if(mResetFrequenciesButton == null)
        {
            mResetFrequenciesButton = new JButton("Reset");
            mResetFrequenciesButton.addActionListener(e -> {

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
    protected JButton getNewSpectrumButton()
    {
        if(mNewSpectrumButton == null)
        {
            mNewSpectrumButton = new JButton("New Spectrum Display");
            mNewSpectrumButton.setToolTipText("Show this tuner in a new (separate) spectral display window");
            mNewSpectrumButton.addActionListener(e ->
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
    protected JButton getViewSpectrumButton()
    {
        if(mViewSpectrumButton == null)
        {
            mViewSpectrumButton = new JButton("View Spectrum");
            mViewSpectrumButton.setToolTipText("Show this tuner in the spectral display");
            mViewSpectrumButton.addActionListener(e ->
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
    protected JButton getEnabledButton()
    {
        if(mEnabledButton == null)
        {
            mEnabledButton = new JButton(BUTTON_STATUS_ENABLE);
            mEnabledButton.setToolTipText("Enable or disable the tuner for use by sdrtrunk");
            mEnabledButton.addActionListener(e ->
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

    protected JButton getRestartTunerButton()
    {
        if(mRestartTunerButton == null)
        {
            mRestartTunerButton = new JButton("Restart Tuner");
            mRestartTunerButton.setVisible(false);
            mRestartTunerButton.setToolTipText("Attempt to restart this tuner to recover from error condition");
            mRestartTunerButton.addActionListener(e ->
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
    protected JToggleButton getRecordButton()
    {
        if(mRecordButton == null)
        {
            mRecordButton = new JToggleButton("Record");
            mRecordButton.setToolTipText("Create a baseband recording for this tuner");
            mRecordButton.setEnabled(false);
            mRecordButton.addActionListener(e ->
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
        turnOffRecorder();

        getFrequencyControl().clearListeners();
        getFrequencyCorrectionSpinner().removeChangeListener(mFrequencyAndCorrectionChangeListener);

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
    public class ButtonPanel extends javafx.scene.layout.VBox
    {
        /**
         * Constructs an instance
         */
        public ButtonPanel()
        {
            this.setSpacing(5);

            javafx.scene.layout.HBox row1 = new javafx.scene.layout.HBox(5);
            row1.getChildren().add(wrapSwingNode(getEnabledButton()));
            row1.getChildren().add(wrapSwingNode(getRecordButton()));
            row1.getChildren().add(wrapSwingNode(getViewSpectrumButton()));
            row1.getChildren().add(wrapSwingNode(getNewSpectrumButton()));

            javafx.scene.layout.HBox row2 = new javafx.scene.layout.HBox(5);
            row2.getChildren().add(wrapSwingNode(getInfoConfigButton()));
            row2.getChildren().add(wrapSwingNode(getRestartTunerButton()));

            this.getChildren().add(row1);
            this.getChildren().add(row2);
            this.getChildren().add(wrapSwingNode(getRecordingStatusLabel()));
        }

        /**
         * Updates the state and text of the buttons based on the tuner status.
         */
        public void updateControls()
        {
            TunerStatus tunerStatus = getDiscoveredTuner().getTunerStatus();

            getRecordButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
            getRecordingStatusLabel().setText(" ");
            getViewSpectrumButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
            getNewSpectrumButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
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
    public class FrequencyPanel extends javafx.scene.layout.VBox
    {
        public FrequencyPanel()
        {
            this.setSpacing(5);
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(5);
            grid.setVgap(5);

            grid.add(wrapSwingNode(getFrequencyControl()), 0, 0, 1, 2);
            grid.add(wrapSwingNode(new JLabel("PPM:")), 1, 0);

            JButton helpButton = createHelpIcon("?");
            helpButton.setToolTipText("<html><b>PPM (Parts Per Million):</b> Adjusts your tuner to match the exact frequency.<br>If your hardware gets warm and signals shift, adjust this until the signal is centered.</html>");
            grid.add(wrapSwingNode(helpButton), 2, 0);

            grid.add(wrapSwingNode(getFrequencyCorrectionSpinner()), 3, 0);
            grid.add(wrapSwingNode(getMeasuredPPMLabel()), 4, 0);
            grid.add(wrapSwingNode(getAutoPPMCheckBox()), 1, 1, 4, 1);

            this.getChildren().add(grid);
            this.getChildren().add(wrapSwingNode(getAutoOptimizeSampleRateCheckBox()));

            javafx.scene.layout.HBox minMaxPanel = new javafx.scene.layout.HBox(5);
            minMaxPanel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            minMaxPanel.getChildren().add(wrapSwingNode(new JLabel("Min:")));
            minMaxPanel.getChildren().add(getMinimumFrequencyTextField());
            minMaxPanel.getChildren().add(wrapSwingNode(new JLabel("Max:")));
            minMaxPanel.getChildren().add(getMaximumFrequencyTextField());
            minMaxPanel.getChildren().add(wrapSwingNode(getResetFrequenciesButton()));

            this.getChildren().add(minMaxPanel);
            this.getChildren().add(wrapSwingNode(getTunerLockedStatusLabel()));
        }

        /**
         * Update the state of the frequency panel controls
         */
        public void updateControls()
        {
            getFrequencyControl().clearListeners();
            getFrequencyControl().addListener(mFrequencyAndCorrectionChangeListener);
            boolean hasTunerUnlocked = hasTuner() && !getTuner().getTunerController().isLockedSampleRate();
            getFrequencyControl().setEnabled(hasTunerUnlocked);
            getMinimumFrequencyTextField().setEnabled(hasTunerUnlocked);
            getMaximumFrequencyTextField().setEnabled(hasTunerUnlocked);
            getResetFrequenciesButton().setEnabled(hasTunerUnlocked);
            getTunerLockedStatusLabel().setVisible(hasTuner() && getTuner().getTunerController().isLockedSampleRate());
            getFrequencyCorrectionSpinner().setEnabled(hasTuner());
            getAutoPPMCheckBox().setEnabled(hasTuner());
            getAutoOptimizeSampleRateCheckBox().setEnabled(hasTuner() && getTuner().getTunerController() instanceof io.github.dsheirer.source.tuner.ISampleRateConfigurable);

            Tuner tuner = getTuner();

            if(tuner != null)
            {
                getFrequencyControl().setFrequency(tuner.getTunerController().getFrequency(), false);
                getMinimumFrequencyTextField().setFrequency(tuner.getTunerController().getMinimumFrequency());
                getMaximumFrequencyTextField().setFrequency(tuner.getTunerController().getMaximumFrequency());
                getFrequencyCorrectionSpinner().setValue(tuner.getTunerController().getFrequencyCorrection());
                getAutoPPMCheckBox().setSelected(tuner.getTunerController().getFrequencyErrorCorrectionManager().isEnabled());
                getAutoOptimizeSampleRateCheckBox().setSelected(getConfiguration().isAutoOptimizeSampleRate());
                getFrequencyControl().addListener(getTuner().getTunerController());
                getTuner().getTunerController().addListener(getFrequencyControl());
                getMeasuredPPMLabel().setText(tuner.getTunerController().getMeasuredErrorStatus());
            }
            else
            {
                getFrequencyControl().setFrequency(0, false);
                getFrequencyCorrectionSpinner().setValue(0);
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
                getFrequencyCorrectionSpinner().setValue(getTuner().getTunerController().getFrequencyCorrection());
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
            EventQueue.invokeLater(() -> getRecordingStatusLabel().setText(status));
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
     * Monitors the frequency correction spinner for changed value.
     */
    private class FrequencyAndCorrectionChangeListener implements ChangeListener, ISourceEventProcessor
    {
        //This monitors the frequency correction spinner, applies the changes to the tuner, and saves configuration.
        @Override
        public void stateChanged(ChangeEvent e)
        {
            final double value = ((SpinnerNumberModel) getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();

            if(hasTuner() && !isLoading())
            {
                try
                {
                    getTuner().getTunerController().setFrequencyCorrection(value);
                }
                catch(SourceException e1)
                {
                    mLog.error("Error setting frequency correction value", e1);
                }

                save();
            }
        }

        //This monitors the frequency control and saves configuration.
        @Override
        public void process(SourceEvent event) throws SourceException
        {
            if(hasTuner() && !isLoading())
            {
                save();
            }
        }
    }

    private JButton createHelpIcon(String text) {
        JButton button = new JButton(text);
        button.setMargin(new java.awt.Insets(0, 2, 0, 2));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        return button;
    }
}
