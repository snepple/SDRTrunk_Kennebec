


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

package io.github.dsheirer.gui.channel;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.geometry.Point2D;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelSource;
import io.github.dsheirer.gui.power.SignalPowerView;
import io.github.dsheirer.gui.squelch.NoiseSquelchView;
import io.github.dsheirer.gui.symbol.SymbolView;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.PrimaryDecoder;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamplesToNativeBufferModule;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.HalfBandTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.FrequencyOverlayPanel;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





import javafx.scene.layout.Pane;
import javafx.scene.Scene;







/**
 * Display for channel FFT and squelch details
 */
public class ChannelSpectrumPanel extends HBox implements Listener<ProcessingChain>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelSpectrumPanel.class);
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private final PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;
    private final ComplexSamplesToNativeBufferModule mSampleStreamTapModule = new ComplexSamplesToNativeBufferModule();
    private final ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
    private SpectrumPanel mSpectrumPanel;
    private final FrequencyOverlayPanel mFrequencyOverlayPanel;
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    // SpinnerNumberModel removed - replaced by IntegerSpinnerValueFactory below
    private final Label mEstimatedCarrierOffsetFrequencyTitleLabel;
    private final Label mEstimatedCarrierOffsetFrequencyValueLabel;
    private boolean mPanelVisible = false;
    private boolean mDftProcessing = false;
    private final NoiseSquelchView mNoiseSquelchView;
    private final SignalPowerView mSignalPowerView;
    private final SymbolView mSymbolView = new SymbolView();
    private final javafx.scene.layout.Pane mNoiseSquelchPanel;
    private final javafx.scene.layout.Pane mSymbolPanel;
    private javafx.scene.layout.StackPane mInspectorPanel;
    

    /**
     * Constructs an instance.
     */
    public ChannelSpectrumPanel(PlaylistManager playlistManager, SettingsManager settingsManager)
    {
        mPlaylistManager = playlistManager;
        mNoiseSquelchView = new NoiseSquelchView(mPlaylistManager);
        mSignalPowerView = new SignalPowerView(mPlaylistManager);
        // setLayout(new javafx.scene.layout.HBox(4));

        VBox fftPanel = new VBox();
        // fftPanel.setLayout(new javafx.scene.layout.HBox(4));

        HBox labelPanel = new HBox(5);
        labelPanel.setAlignment(Pos.CENTER_LEFT);
        labelPanel.setPadding(new Insets(2, 5, 2, 5));
        labelPanel.getChildren().add(new Label("Channel Spectrum"));

        mEstimatedCarrierOffsetFrequencyTitleLabel = new Label("Carrier Offset:");
        mEstimatedCarrierOffsetFrequencyTitleLabel.setTextFill(javafx.scene.paint.Color.rgb(142, 142, 147));
        mEstimatedCarrierOffsetFrequencyTitleLabel.setMinWidth(Region.USE_PREF_SIZE);
        labelPanel.getChildren().add(mEstimatedCarrierOffsetFrequencyTitleLabel);

        mEstimatedCarrierOffsetFrequencyValueLabel = new Label("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setFont(javafx.scene.text.Font.font(mEstimatedCarrierOffsetFrequencyValueLabel.getFont().getFamily(), javafx.scene.text.FontWeight.BOLD, mEstimatedCarrierOffsetFrequencyValueLabel.getFont().getSize()));
        mEstimatedCarrierOffsetFrequencyValueLabel.setDisable(true);
        mEstimatedCarrierOffsetFrequencyValueLabel.setPrefWidth(60);
        mEstimatedCarrierOffsetFrequencyValueLabel.setMinWidth(Region.USE_PREF_SIZE);
        labelPanel.getChildren().add(mEstimatedCarrierOffsetFrequencyValueLabel);

        javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory mNoiseFloorSpinnerValueFactory = new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(8, 36, 18, 1);
        Spinner<Integer> noiseFloorSpinner = new Spinner<>(mNoiseFloorSpinnerValueFactory);
        noiseFloorSpinner.setPrefWidth(70);
        noiseFloorSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal != null) {
                mSpectrumPanel.setSampleSize(newVal.doubleValue());
            }
        });
        labelPanel.getChildren().add(noiseFloorSpinner);
        labelPanel.getChildren().add(new Label("Noise Floor"));

        Button logIndexesButton = new Button("Log Settings");
        logIndexesButton.setTooltip(new javafx.scene.control.Tooltip("Log channel spectrum settings"));
        logIndexesButton.accessibleTextProperty().set("Log Spectrum Settings");
        logIndexesButton.accessibleHelpProperty().set("Logs the current configuration settings of the channel spectrum to the application log");
        logIndexesButton.setOnAction(e -> {
            if(mProcessingChain != null)
            {
                Source source = mProcessingChain.getSource();

                if(source instanceof PolyphaseChannelSource pcs)
                {
                    List<Integer> indexes = pcs.getOutputProcessorIndexes();
                    double sampleRate = pcs.getSampleRate();
                    long indexCenterFrequency = pcs.getIndexCenterFrequency();
                    long appliedFrequencyOffset = pcs.getFrequencyOffset();
                    long requestedCenterFrequency = pcs.getFrequency();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Polyphase Channel - BW: ").append(FREQUENCY_FORMAT.format(sampleRate / 1E6d));
                    sb.append(" Center/Requested/Mixer: ").append(FREQUENCY_FORMAT.format(indexCenterFrequency / 1E6d));
                    sb.append("/").append(FREQUENCY_FORMAT.format(requestedCenterFrequency / 1E6d));
                    sb.append("/").append(FREQUENCY_FORMAT.format(appliedFrequencyOffset / 1E6d));
                    sb.append(" Polyphase Indexes: ").append(indexes);
                    sb.append(" Tuner SR:").append(FREQUENCY_FORMAT.format(pcs.getTunerSampleRate() / 1E6d));
                    sb.append(" CF:").append(FREQUENCY_FORMAT.format(pcs.getTunerCenterFrequency() / 1E6d));
                    LOGGER.info(sb.toString());
                    LOGGER.info("Output Processor: " + pcs.getStateDescription());
                }
                else if(source instanceof HalfBandTunerChannelSource<?> hbtcs)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Heterodyne Channel - CF:").append(FREQUENCY_FORMAT.format(hbtcs.getFrequency() / 1E6d));
                    sb.append(" SR:").append(FREQUENCY_FORMAT.format(hbtcs.getSampleRate() / 1E6d));
                    sb.append(" Mixer:").append(FREQUENCY_FORMAT.format(hbtcs.getMixerFrequency() / 1E6d));
                    LOGGER.info(sb.toString());
                }
                else
                {
                    LOGGER.info("Unsupported channel type: " + (source != null ? source.getClass() : " null"));
                }
            }
        });
        //This is a debug button to log the current settings to the app log.
//        labelPanel.add(mLogIndexesButton);

        fftPanel.getChildren().add(labelPanel);

        mFrequencyOverlayPanel = new FrequencyOverlayPanel(settingsManager);
        mSpectrumPanel = new SpectrumPanel(settingsManager);
        mSpectrumPanel.setSampleSize(18.0);

        /**
         * The layered pane holds the overlapping spectrum and channel panels
         * and manages the sizing of each panel with the resize listener
         */
        StackPane layeredPanel = new StackPane();
        

        /**
         * Create a mouse adapter to handle mouse events over the spectrum
         * and waterfall panels
         */
        MouseEventProcessor mouser = new MouseEventProcessor();

        mFrequencyOverlayPanel.setOnMouseEntered(mouser::mouseEntered);
        mFrequencyOverlayPanel.setOnMouseExited(mouser::mouseExited);
        mFrequencyOverlayPanel.setOnMouseMoved(mouser::mouseMoved);
        

        //Add the spectrum and channel panels to the layered panel
        layeredPanel.getChildren().add(mSpectrumPanel);
        layeredPanel.getChildren().add(mFrequencyOverlayPanel);

        VBox.setVgrow(layeredPanel, Priority.ALWAYS);
        fftPanel.getChildren().add(layeredPanel);

        mNoiseSquelchPanel = new javafx.scene.layout.Pane();
        mSymbolPanel = new javafx.scene.layout.Pane();

        //Add the JavaFX views directly as children
        mNoiseSquelchPanel.getChildren().add(mNoiseSquelchView);
        mNoiseSquelchView.prefWidthProperty().bind(mNoiseSquelchPanel.widthProperty());
        mNoiseSquelchView.prefHeightProperty().bind(mNoiseSquelchPanel.heightProperty());

        mSymbolPanel.getChildren().add(mSymbolView);
        mSymbolView.prefWidthProperty().bind(mSymbolPanel.widthProperty());
        mSymbolView.prefHeightProperty().bind(mSymbolPanel.heightProperty());

        mInspectorPanel = new javafx.scene.layout.StackPane();

        mInspectorPanel.getChildren().add(mNoiseSquelchPanel); mNoiseSquelchPanel.setVisible(false);
        mInspectorPanel.getChildren().add(mSignalPowerView); mSignalPowerView.setVisible(false);
        mInspectorPanel.getChildren().add(mSymbolPanel); mSymbolPanel.setVisible(false);

        // Side-by-side layout: FFT on the left, inspector on the right
        HBox.setHgrow(fftPanel, Priority.ALWAYS);
        HBox.setHgrow(mInspectorPanel, Priority.ALWAYS);
        fftPanel.setMinWidth(200);
        fftPanel.setPrefWidth(400);
        mInspectorPanel.setMinWidth(300);
        mInspectorPanel.setPrefWidth(400);
        getChildren().add(fftPanel);
        getChildren().add(mInspectorPanel);

        mSampleStreamTapModule.setListener(mComplexDftProcessor);
        DFTResultsConverter DFTResultsConverter = new ComplexDecibelConverter();
        mComplexDftProcessor.addConverter(DFTResultsConverter);
        DFTResultsConverter.addListener(mSpectrumPanel);
        mSpectrumPanel.clearSpectrum();
        installWindowVisibilityListeners();
    }


    /**
     * Wires up window visibility listeners to pause ALL rendering and FFT computation
     * when the window is minimized or hidden. This protects audio DSP thread performance.
     */
    private void installWindowVisibilityListeners() {
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldWindow, newWindow) -> {
                    if (newWindow instanceof javafx.stage.Stage stage) {
                        stage.iconifiedProperty().addListener((iObs, wasIconified, isIconified) -> {
                            if (isIconified) {
                                mSpectrumPanel.setRenderingEnabled(false);
                                mComplexDftProcessor.stop();
                            } else {
                                mSpectrumPanel.setRenderingEnabled(true);
                                updateFFTProcessing();
                            }
                        });
                        stage.showingProperty().addListener((sObs, wasShowing, isShowing) -> {
                            if (!isShowing) {
                                mSpectrumPanel.setRenderingEnabled(false);
                                mComplexDftProcessor.stop();
                            } else {
                                mSpectrumPanel.setRenderingEnabled(true);
                                updateFFTProcessing();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Signals this panel to indicate if this panel is visible to turn on the FFT processor when the panel is visible
     * and turn off the FFT processor when it's not.
     *
     * Note: this method is intended to be called by the Swing event thread to ensure that only a single thread is
     * invoking either this method, or the receive() method, since there is no thread synchronization between these
     * two methods and they each depend on stable access to the mPanelVisible variable.
     *
     * @param visible true to indicate that this panel is showing/visible.
     */
    public void setPanelVisible(boolean visible)
    {
        mPanelVisible = visible;
        updateFFTProcessing();
        mNoiseSquelchView.setShowing(visible);
        mSymbolView.setShowing(visible);
    }

    /**
     * Updates processing state for the DFT processor.  Turns on DFT processing when we have a processing chain and
     * when the user has this tab selected and visible.  Otherwise, turns off DFT processing.
     */
    private void updateFFTProcessing()
    {
        if(mPanelVisible && mProcessingChain != null)
        {
            startDftProcessing();
        }
        else
        {
            stopDftProcessing();
        }
    }

    /**
     * Starts DFT processing
     */
    private void startDftProcessing()
    {
        if(!mDftProcessing)
        {
            mDftProcessing = true;
            mSampleStreamTapModule.setListener(mComplexDftProcessor);
            mComplexDftProcessor.start();
        }
    }

    /**
     * Stops DFT processing
     */
    private void stopDftProcessing()
    {
        if(mDftProcessing)
        {
            mSampleStreamTapModule.removeListener();
            mComplexDftProcessor.stop();
            mSpectrumPanel.clearSpectrum();
            mDftProcessing = false;
        }
    }

    /**
     * Updates the CarrierOffsetProcessor's current carrier offset tracking frequency
     * @param carrierOffsetFrequency that is currently measured/estimated.
     */
    private void updateEstimatedCarrierOffsetFrequency(long carrierOffsetFrequency)
    {
        //Note: we flip the sign on the error measurement because the value represents the amount of offset the PLL
        //has to apply to move the signal to center/baseband
        Platform.runLater(() -> {
            mEstimatedCarrierOffsetFrequencyValueLabel.setText(carrierOffsetFrequency + " Hz");
            mEstimatedCarrierOffsetFrequencyValueLabel.setDisable(false);
        });

        mFrequencyOverlayPanel.setEstimatedCarrierOffsetFrequency(carrierOffsetFrequency);
    }

    private void broadcast(SourceEvent sourceEvent)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.  Note: this must be called on the Swing
     * dispatch thread because it directly invokes swing components.
     */
    private void reset()
    {
        mEstimatedCarrierOffsetFrequencyValueLabel.setText("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setDisable(true);
        mFrequencyOverlayPanel.process(SourceEvent.frequencyChange(null, 0));
        mFrequencyOverlayPanel.process(SourceEvent.sampleRateChange(0));
        mFrequencyOverlayPanel.setEstimatedCarrierOffsetFrequency(0);
        mFrequencyOverlayPanel.setChannelBandwidth(0);
    }

    /**
     * Receive notifications of request to provide display of processing chain details.
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        //Disconnect the previous processing chain.
        if(mProcessingChain != null)
        {
            mNoiseSquelchView.setController(null);
            mSignalPowerView.setProcessingChain(null);
            mSymbolView.removeSymbolProvider();
            mSymbolView.setProtocol("");
            mProcessingChain.removeSourceEventListener(mSourceEventProcessor);
            mProcessingChain.removeModule(mSampleStreamTapModule);
        }

        //Invoking reset - we're on the Swing dispatch thread here
        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);

            PrimaryDecoder primaryDecoder = mProcessingChain.getPrimaryDecoder();
            if(primaryDecoder instanceof NBFMDecoder nbfmDecoder)
            {
                setRightComponent(mNoiseSquelchPanel);
                mNoiseSquelchView.setController(nbfmDecoder);
            }
            else if(primaryDecoder instanceof AMDecoder)
            {
                setRightComponent(mSignalPowerView);
                mSignalPowerView.setProcessingChain(mProcessingChain);
            }
            else if(primaryDecoder instanceof FeedbackDecoder feedbackDecoder)
            {
                setRightComponent(mSymbolPanel);
                mSymbolView.setSymbolProvider(feedbackDecoder);
                mSymbolView.setProtocol(feedbackDecoder.getProtocolDescription());
            }

            mProcessingChain.addModule(mSampleStreamTapModule);
            Source source = mProcessingChain.getSource();

            if(source instanceof TunerChannelSource tcs)
            {
                mFrequencyOverlayPanel.process(SourceEvent.frequencyChange(null, tcs.getFrequency()));
                mFrequencyOverlayPanel.process(SourceEvent.sampleRateChange(tcs.getSampleRate()));
            }

            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null)
            {
                List<TunerChannel> tunerChannels = channel.getTunerChannels();

                if(!tunerChannels.isEmpty())
                {
                    mFrequencyOverlayPanel.setChannelBandwidth(tunerChannels.getFirst().getBandwidth());
                }
            }
        }

        updateFFTProcessing();
    }

    /**
     * Shows the component on the right side of the split pane.
     * @param component to show.
     */
    private void setRightComponent(Node component)
    {
        if (component == mNoiseSquelchPanel) {
            mNoiseSquelchPanel.setVisible(true); mSignalPowerView.setVisible(false); mSymbolPanel.setVisible(false);
            mInspectorPanel.setVisible(true);
        } else if (component == mSignalPowerView) {
            mNoiseSquelchPanel.setVisible(false); mSignalPowerView.setVisible(true); mSymbolPanel.setVisible(false);
            mInspectorPanel.setVisible(true);
        } else if (component == mSymbolPanel) {
            mNoiseSquelchPanel.setVisible(false); mSignalPowerView.setVisible(false); mSymbolPanel.setVisible(true);
            mInspectorPanel.setVisible(true);
        } else {
            mInspectorPanel.setVisible(false);
        }
    }

    /**
     * Processor for source event stream to capture power level and squelch related source events.
     */
    private class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED)
            {
                updateEstimatedCarrierOffsetFrequency(sourceEvent.getValue().longValue());
            }

            mSignalPowerView.receive(sourceEvent);
        }
    }

    /**
     * Monitors the sizing of the layered pane and resizes the spectrum and
     * channel panels whenever the layered pane is resized
     */
    public class ResizeListener
    {
        private void resize() {
            // Nothing to do since stack pane manages it
        }
    }

    /**
     * Mouse event handler for the spectral display panel.
     */
    public class MouseEventProcessor
    {
        public void mouseMoved(javafx.scene.input.MouseEvent event)
        {
            update(event);
        }

        /**
         * Updates the cursor display while the mouse is performing actions
         */
        private void update(javafx.scene.input.MouseEvent event)
        {
            mFrequencyOverlayPanel.setCursorLocation(new Point2D(event.getX(), event.getY()));
        }

        public void mouseEntered(javafx.scene.input.MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(true);
        }

        public void mouseExited(javafx.scene.input.MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(false);
        }
    }
}
