
/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.gui.channelizer;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelSource;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.LoggingTunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.test.TestTuner;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;

public class ChannelizerViewer2 extends Stage
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer2.class);

    private static final int CHANNEL_BANDWIDTH = 12500;

    private SettingsManager mSettingsManager = new SettingsManager();
    private VBox mPrimaryPanel;
    private VBox mControlPanel;
    private Button mTopFrameAddChannelButton;
    private Button mBottomFrameAddChannelButton;
    private Label mToneFrequencyLabel;
    private Label mCenterFrequencyLabel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mTopChannelArrayPanel;
    private ChannelArrayPanel mBottomChannelArrayPanel;
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT04096;
    private TestTuner mTestTuner = new TestTuner(new LoggingTunerErrorListener());

    /**
     * GUI Test utility for researching polyphase channelizers.
     */
    public ChannelizerViewer2()
    {
        init();
    }

    private void init()
    {
        setTitle("Polyphase Channelizer Viewer");
        setWidth(1200);
        setHeight(700);
        setOnCloseRequest(event -> System.exit(0));

        VBox vbox = new VBox(getPrimaryPanel());
VBox.setVgrow(vbox, Priority.ALWAYS);

        Scene scene = new Scene(vbox);
        // setScene(scene);
    }

    private VBox getPrimaryPanel()
    {
        if(mPrimaryPanel == null)
        {
            mPrimaryPanel = new VBox();
                        mPrimaryPanel.getChildren().add(getSpectrumPanel());
            mPrimaryPanel.getChildren().add(getControlPanel());
            mPrimaryPanel.getChildren().add(getTopChannelArrayPanel());
            mPrimaryPanel.getChildren().add(getBottomChannelArrayPanel());
        }

        return mPrimaryPanel;
    }

    private PrimarySpectrumPanel getSpectrumPanel()
    {
        if(mPrimarySpectrumPanel == null)
        {
            mPrimarySpectrumPanel = new PrimarySpectrumPanel(mSettingsManager,
                mTestTuner.getTunerController().getSampleRate());
            mPrimarySpectrumPanel.setPrefSize(1200, 200);
            mPrimarySpectrumPanel.setDFTSize(mMainPanelDFTSize);
            mTestTuner.getTunerController().addBufferListener(mPrimarySpectrumPanel);
        }

        return mPrimarySpectrumPanel;
    }

    private VBox getControlPanel()
    {
        if(mControlPanel == null)
        {
            mControlPanel = new VBox();
            
            mControlPanel.getChildren().add(new Label("Tone:"));
            long minimumFrequency = -(long)mTestTuner.getTunerController().getSampleRate() / 2 + 1;
            long maximumFrequency = (long)mTestTuner.getTunerController().getSampleRate() / 2 - 1;
            long toneFrequency = 0;

            SpinnerValueFactory.DoubleSpinnerValueFactory model = new SpinnerValueFactory.DoubleSpinnerValueFactory(minimumFrequency, maximumFrequency, toneFrequency, 100);

            model.valueProperty().addListener((obs, oldVal, newVal) -> {
                    long tf = model.getValue().longValue();
                    mTestTuner.getTunerController().setToneFrequency(tf);
                    mToneFrequencyLabel.setText(String.valueOf(getToneFrequency()));
                    mCenterFrequencyLabel.setText(String.valueOf(getCenterFrequency()));
                });

            Spinner<Double> spinner = new Spinner<Double>(model);

            mControlPanel.getChildren().add(spinner);
            mControlPanel.getChildren().add(new Label("Hz"));

            mControlPanel.getChildren().add(new Label("Tone Frequency:"));
            mToneFrequencyLabel = new Label(String.valueOf(getToneFrequency()));
            mControlPanel.getChildren().add(mToneFrequencyLabel);

            mControlPanel.getChildren().add(new Label("Center Frequency:"));
            mCenterFrequencyLabel = new Label(String.valueOf(getCenterFrequency()));
            mControlPanel.getChildren().add(mCenterFrequencyLabel);

            mControlPanel.getChildren().add(getBottomFrameAddChannelButton());
            mControlPanel.getChildren().add(getTopFrameAddChannelButton());
        }

        return mControlPanel;
    }

    private Button getTopFrameAddChannelButton()
    {
        if(mTopFrameAddChannelButton == null)
        {
            mTopFrameAddChannelButton = new Button("Top - Add Channel");

            mTopFrameAddChannelButton.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setHeaderText("Frequency?");
                    Optional<String> result = dialog.showAndWait();
                    String value = result.orElse(null);

                    if(value != null && !value.isEmpty())
                    {
                        try
                        {
                            int frequency = Integer.parseInt(value);

                            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);

                            getTopChannelArrayPanel().addChannel(tunerChannel);
                        }
                        catch(Exception e1)
                        {
                            mLog.error("Can't parse frequency from value: " + value, e1);
                        }
                    }
            });
        }

        return mTopFrameAddChannelButton;
    }

    private Button getBottomFrameAddChannelButton()
    {
        if(mBottomFrameAddChannelButton == null)
        {
            mBottomFrameAddChannelButton = new Button("Bottom - Add Channel");
            mBottomFrameAddChannelButton.setTooltip(new javafx.scene.control.Tooltip("Add a channel to the bottom frame"));
            mBottomFrameAddChannelButton.accessibleTextProperty().set("Add Channel to Bottom Frame");
            mBottomFrameAddChannelButton.accessibleHelpProperty().set("Prompts for a frequency and adds a new channel to the bottom frame");

            mBottomFrameAddChannelButton.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setHeaderText("Frequency?");
                    Optional<String> result = dialog.showAndWait();
                    String value = result.orElse(null);

                    if(value != null && !value.isEmpty())
                    {
                        try
                        {
                            int frequency = Integer.parseInt(value);

                            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);

                            getBottomChannelArrayPanel().addChannel(tunerChannel);
                        }
                        catch(Exception e1)
                        {
                            mLog.error("Can't parse frequency from value: " + value, e1);
                        }
                    }
            });
        }

        return mBottomFrameAddChannelButton;
    }

    private long getToneFrequency()
    {
        return mTestTuner.getTunerController().getFrequency() + mTestTuner.getTunerController().getToneFrequency();
    }

    private long getCenterFrequency()
    {
        return mTestTuner.getTunerController().getFrequency();
    }

    private ChannelArrayPanel getTopChannelArrayPanel()
    {
        if(mTopChannelArrayPanel == null)
        {
            mTopChannelArrayPanel = new ChannelArrayPanel();
        }

        return mTopChannelArrayPanel;
    }

    private ChannelArrayPanel getBottomChannelArrayPanel()
    {
        if(mBottomChannelArrayPanel == null)
        {
            mBottomChannelArrayPanel = new ChannelArrayPanel();
        }

        return mBottomChannelArrayPanel;
    }

    public class ChannelArrayPanel extends VBox
    {
        public ChannelArrayPanel()
        {
            init();
        }

        private void init()
        {
            
        }

        public void addChannel(TunerChannel tunerChannel)
        {
            ChannelPanel channelPanel = new ChannelPanel(mSettingsManager, CHANNEL_BANDWIDTH * 2,
                tunerChannel.getFrequency(), CHANNEL_BANDWIDTH);
            channelPanel.setDFTSize(mChannelPanelDFTSize);
        getChildren().add(channelPanel);

            /* validate(); */
            requestLayout();
        }
    }

    /**
     * Returns a list of tuner channels that will fit within the tuner's bandwidth, minus a half channel each at the
     * lower and upper ends of the spectrum.
     *
     * @param tuner to create channels for
     * @return list of contiguous channels filling the tuner bandwidth
     */
    public static List<TunerChannel> getTunerChannels(Tuner tuner)
    {
        List<TunerChannel> tunerChannels = new ArrayList<>();

        long baseFrequency = tuner.getTunerController().getFrequency();
        baseFrequency -= tuner.getTunerController().getSampleRate() / 2;
        baseFrequency += (CHANNEL_BANDWIDTH / 2);

        int channelCount = (int)(tuner.getTunerController().getSampleRate() / CHANNEL_BANDWIDTH) - 1;

        for(int x = 0; x < channelCount; x++)
        {
            long frequency = baseFrequency + (x * CHANNEL_BANDWIDTH);
            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);
            tunerChannels.add(tunerChannel);
        }

        return tunerChannels;
    }

    public class PrimarySpectrumPanel extends VBox implements Listener<INativeBuffer>, ISourceEventProcessor
    {
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public PrimarySpectrumPanel(SettingsManager settingsManager, double sampleRate)
        {
            
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
        getChildren().add(mSpectrumPanel);

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDecibelConverter.addListener(mSpectrumPanel);
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mComplexDftProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            mComplexDftProcessor.receive(nativeBuffer);
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public class ChannelPanel extends VBox implements Listener<INativeBuffer>, ISourceEventProcessor
    {
        private TunerChannelSource mSource;
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private ToggleButton mLoggingButton;
        private boolean mLoggingEnabled;

        public ChannelPanel(SettingsManager settingsManager, double sampleRate, long frequency, int bandwidth)
        {
            
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
        getChildren().add(mSpectrumPanel);

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            TunerChannel tunerChannel = new TunerChannel(frequency, bandwidth);
            mSource = mTestTuner.getChannelSourceManager().getSource(tunerChannel,
                    new ChannelSpecification(50000, 12500, 6000, 7000), "test");

            if(mSource != null)
            {
                mLog.debug("Channel: " + mSource.getTunerChannel() + " Rate:" + mSource.getSampleRate());
                mSource.setListener((Listener<ComplexSamples>) complexSamples ->
                {
                    mComplexDftProcessor.receive(new FloatNativeBuffer(complexSamples.toInterleaved()));
                });

                if(mSource instanceof PolyphaseChannelSource pcs)
                {                    List<Integer> indexes = pcs.getOutputProcessorIndexes();
                    double sRate = pcs.getSampleRate();
                    long indexCenterFrequency = pcs.getIndexCenterFrequency();
                    long appliedFrequencyOffset = pcs.getFrequencyOffset();
                    long requestedCenterFrequency = pcs.getFrequency();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Polyphase Channel - Bandwidth: ").append(sRate);
                    sb.append(" Channel/Requested/Offset: ").append(indexCenterFrequency);
                    sb.append("/").append(requestedCenterFrequency);
                    sb.append("/").append(appliedFrequencyOffset);
                    sb.append(" Indexes: ").append(indexes);
                    sb.append(" Tuner SR:").append(pcs.getTunerSampleRate());
                    sb.append(" Tuner CF:").append(pcs.getTunerCenterFrequency());
                    mLog.info(sb.toString());


                }

                mSource.start();
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for frequency: " + frequency);
            }

            if(mSource != null)
            {
                int half = (int)(sampleRate / 2.0f);
        getChildren().add(new Label("Min:" + (frequency - half)));
        getChildren().add(new Label("Center:" + frequency));
        getChildren().add(new Label("Max:" + (frequency + half)));
            }
            else
            {
        getChildren().add(new Label("NO SRC:" + frequency));
            }

            mLoggingButton = new ToggleButton("Logging");
            mLoggingButton.setOnAction(e -> mLoggingEnabled = mLoggingButton.isSelected());
        }

        public TunerChannelSource getSource()
        {
            return mSource;
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mComplexDftProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            mComplexDftProcessor.receive(nativeBuffer);
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public static void main(String[] args)
    {
        final ChannelizerViewer2 frame = new ChannelizerViewer2();

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                frame.show();
            }
        });
    }
}