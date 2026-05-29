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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.scene.Scene;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.embed.swing.SwingNode;

import javax.swing.JSpinner;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.CardLayout;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

/**
 * Display for channel FFT and squelch details
 */
public class ChannelSpectrumPanel extends javafx.embed.swing.JFXPanel implements Listener<ProcessingChain>
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
    private final SpinnerValueFactory.DoubleSpinnerValueFactory mNoiseFloorSpinnerModel;
    private final Label mEstimatedCarrierOffsetFrequencyTitleLabel;
    private final Label mEstimatedCarrierOffsetFrequencyValueLabel;
    private boolean mPanelVisible = false;
    private boolean mDftProcessing = false;
    private final NoiseSquelchView mNoiseSquelchView;
    private final SignalPowerView mSignalPowerView;

    private final SymbolView mSymbolView = new SymbolView();


    private StackPane mInspectorPanel;


    /**
     * Constructs an instance.
     */
    public ChannelSpectrumPanel(PlaylistManager playlistManager, SettingsManager settingsManager)
    {
        mPlaylistManager = playlistManager;
        mNoiseSquelchView = new NoiseSquelchView(mPlaylistManager);
        mSignalPowerView = new SignalPowerView(mPlaylistManager);


        VBox fftPanel = new VBox();
        HBox.setHgrow(fftPanel, Priority.ALWAYS);

        HBox labelPanel = new HBox(5);
        labelPanel.setPadding(new Insets(2));
        labelPanel.setAlignment(Pos.CENTER_LEFT);
        labelPanel.getChildren().add(new Label("Channel Spectrum    "));

        mEstimatedCarrierOffsetFrequencyTitleLabel = new Label("Carrier Offset:");
        mEstimatedCarrierOffsetFrequencyTitleLabel.setStyle("-fx-text-fill: #8e8e93;"); // HIG subtle gray
        labelPanel.getChildren().add(mEstimatedCarrierOffsetFrequencyTitleLabel);

        mEstimatedCarrierOffsetFrequencyValueLabel = new Label("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setStyle("-fx-font-weight: bold;");
        mEstimatedCarrierOffsetFrequencyValueLabel.setDisable(true);
        mEstimatedCarrierOffsetFrequencyValueLabel.setPrefWidth(60);
        labelPanel.getChildren().add(mEstimatedCarrierOffsetFrequencyValueLabel);

        // Add spacer to push the next elements to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        labelPanel.getChildren().add(spacer);

        mNoiseFloorSpinnerModel = new SpinnerValueFactory.DoubleSpinnerValueFactory(8.0, 36.0, 18.0, 1.0);
        mNoiseFloorSpinnerModel.valueProperty().addListener((obs, oldVal, newVal) -> {
            mSpectrumPanel.setSampleSize(newVal);
        });
        Spinner<Double> noiseFloorSpinner = new Spinner<>(mNoiseFloorSpinnerModel);
        labelPanel.getChildren().add(noiseFloorSpinner);
        labelPanel.getChildren().add(new Label("Noise Floor"));

        Button logIndexesButton = new Button("Log Settings");
        logIndexesButton.setTooltip(new Tooltip("Log channel spectrum settings"));
        // Accessible description
        logIndexesButton.setAccessibleText("Logs the current configuration settings of the channel spectrum to the application log");
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
//        labelPanel.getChildren().add(logIndexesButton);

        fftPanel.getChildren().add(labelPanel);

        mFrequencyOverlayPanel = new FrequencyOverlayPanel(settingsManager);
        mSpectrumPanel = new SpectrumPanel(settingsManager);
        mSpectrumPanel.setSampleSize(18.0);

        StackPane layeredPanel = new StackPane();
        VBox.setVgrow(layeredPanel, Priority.ALWAYS);

        MouseEventProcessor mouser = new MouseEventProcessor();
        mFrequencyOverlayPanel.addMouseListener(mouser);
        mFrequencyOverlayPanel.addMouseMotionListener(mouser);
        mFrequencyOverlayPanel.addMouseWheelListener(mouser);

        SwingNode frequencyNode = new SwingNode();
        javax.swing.SwingUtilities.invokeLater(() -> frequencyNode.setContent(mFrequencyOverlayPanel));

        // Use size listener for layered effect resizing, as StackPane doesn't perfectly propagate down to SwingNode without layout
        layeredPanel.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                mFrequencyOverlayPanel.setBounds(0, 0, (int)newVal.getWidth(), (int)newVal.getHeight());
            });
        });

        layeredPanel.getChildren().addAll(mSpectrumPanel, frequencyNode);
        fftPanel.getChildren().add(layeredPanel);

        mInspectorPanel = new StackPane();
        mInspectorPanel.setStyle("-fx-border-color: transparent transparent transparent #e0e0e0; -fx-border-width: 0 0 0 1;"); // Apple HIG subtle border
        HBox.setHgrow(mInspectorPanel, Priority.ALWAYS);

        // Add all views but hide them initially
        mNoiseSquelchView.setVisible(false);
        mSignalPowerView.setVisible(false);
        mSymbolView.setVisible(false);
        mInspectorPanel.getChildren().addAll(mNoiseSquelchView, mSignalPowerView, mSymbolView);

        HBox mainLayout = new HBox();
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Give 50% width preference to both sides
        fftPanel.prefWidthProperty().bind(mainLayout.widthProperty().divide(2));
        mInspectorPanel.prefWidthProperty().bind(mainLayout.widthProperty().divide(2));

        mainLayout.getChildren().addAll(fftPanel, mInspectorPanel);
                Platform.runLater(() -> {
            Scene scene = new Scene(mainLayout);
            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            this.setScene(scene);
        });

        mSampleStreamTapModule.setListener(mComplexDftProcessor);
        DFTResultsConverter DFTResultsConverter = new ComplexDecibelConverter();
        mComplexDftProcessor.addConverter(DFTResultsConverter);
        DFTResultsConverter.addListener(mSpectrumPanel);
        mSpectrumPanel.clearSpectrum();
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
        Platform.runLater(() -> {
            mEstimatedCarrierOffsetFrequencyValueLabel.setText("0 Hz");
            mEstimatedCarrierOffsetFrequencyValueLabel.setDisable(true);
        });
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

        //Invoking reset
        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);

            PrimaryDecoder primaryDecoder = mProcessingChain.getPrimaryDecoder();
            if(primaryDecoder instanceof NBFMDecoder nbfmDecoder)
            {
                setRightComponent(mNoiseSquelchView);
                mNoiseSquelchView.setController(nbfmDecoder);
            }
            else if(primaryDecoder instanceof AMDecoder)
            {
                setRightComponent(mSignalPowerView);
                mSignalPowerView.setProcessingChain(mProcessingChain);
            }
            else if(primaryDecoder instanceof FeedbackDecoder feedbackDecoder)
            {
                setRightComponent(mSymbolView);
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
        Platform.runLater(() -> {
            mNoiseSquelchView.setVisible(false);
            mSignalPowerView.setVisible(false);
            mSymbolView.setVisible(false);

            if (component != null) {
                component.setVisible(true);
                mInspectorPanel.setVisible(true);
            } else {
                mInspectorPanel.setVisible(false);
            }
        });
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
     * Mouse event handler for the spectral display panel.
     */
    public class MouseEventProcessor extends MouseInputAdapter
    {
        @Override public void mouseMoved(MouseEvent event)
        {
            update(event);
        }

        /**
         * Updates the cursor display while the mouse is performing actions
         */
        private void update(MouseEvent event)
        {
            mFrequencyOverlayPanel.setCursorLocation(event.getPoint());
        }

        @Override public void mouseEntered(MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(true);
        }

        @Override public void mouseExited(MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(false);
        }
    }
}
