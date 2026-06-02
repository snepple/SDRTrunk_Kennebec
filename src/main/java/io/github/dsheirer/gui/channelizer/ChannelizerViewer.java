
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
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.LoggingTunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.test.TestTuner;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import java.util.ArrayList;
import java.util.Arrays;
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



public class ChannelizerViewer extends Stage
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_FFT_FRAME_RATE = 20;

    private SettingsManager mSettingsManager = new SettingsManager();
    private VBox mPrimaryPanel;
    private VBox mControlPanel;
    private Label mToneFrequencyLabel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mChannelPanel;
    private DiscreteIndexChannelArrayPanel mDiscreteIndexChannelPanel;
    private int mChannelCount;
    private int mChannelsPerRow;
    private long mBaseFrequency = 100000000;  //100 MHz
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT04096;
    private TestTuner mTestTuner;

    /**
     * GUI Test utility for researching polyphase channelizers.
     *
     * @param channelsPerRow
     */
    public ChannelizerViewer(int channelsPerRow)
    {
        mTestTuner = new TestTuner(new LoggingTunerErrorListener());
        mChannelCount = (int)(mTestTuner.getTunerController().getBandwidth() / CHANNEL_BANDWIDTH);
        mChannelsPerRow = channelsPerRow;

        init();
    }

    private void init()
    {
        setTitle("Polyphase Channelizer Viewer");
        setWidth(1200);
        setHeight(800);
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
            mPrimaryPanel.getChildren().add(getChannelArrayPanel());
            mPrimaryPanel.getChildren().add(getDiscreteIndexChannelPanel());
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
                });

            Spinner<Double> spinner = new Spinner<Double>(model);

            mControlPanel.getChildren().add(spinner);
            mControlPanel.getChildren().add(new Label("Hz"));

            mControlPanel.getChildren().add(new Label("Frequency:"));
            mToneFrequencyLabel = new Label(String.valueOf(getToneFrequency()));
            mControlPanel.getChildren().add(mToneFrequencyLabel);

            mControlPanel.getChildren().add(new Label("Channels: " + mChannelCount));
        }

        return mControlPanel;
    }

    private long getToneFrequency()
    {
        return mTestTuner.getTunerController().getFrequency() + mTestTuner.getTunerController().getToneFrequency();
    }

    private ChannelArrayPanel getChannelArrayPanel()
    {
        if(mChannelPanel == null)
        {
            mChannelPanel = new ChannelArrayPanel();
        }

        return mChannelPanel;
    }

    private DiscreteIndexChannelArrayPanel getDiscreteIndexChannelPanel()
    {
        if(mDiscreteIndexChannelPanel == null)
        {
            mDiscreteIndexChannelPanel = new DiscreteIndexChannelArrayPanel();
        }

        return mDiscreteIndexChannelPanel;
    }

    public class ChannelArrayPanel extends VBox
    {
        private final Logger mLog = LoggerFactory.getLogger(ChannelArrayPanel.class);

        public ChannelArrayPanel()
        {
            int bufferSize = CHANNEL_BANDWIDTH / CHANNEL_FFT_FRAME_RATE;
            if(bufferSize % 2 == 1)
            {
                bufferSize++;
            }

            init();
        }

        private void init()
        {
            

            double spectralBandwidth = mTestTuner.getTunerController().getSampleRate();
            double halfSpectralBandwidth = spectralBandwidth / 2.0;

            int channelToLog = -1;

            long baseFrequency = mBaseFrequency + (CHANNEL_BANDWIDTH / 2);

            for(int x = 0; x < mChannelCount; x++)
            {
                long frequency = baseFrequency + (x * CHANNEL_BANDWIDTH);

                mLog.debug("Channel " + x + "/" + mChannelCount + " Frequency: " + frequency);

                ChannelPanel channelPanel = new ChannelPanel(mSettingsManager, CHANNEL_BANDWIDTH * 2, frequency, CHANNEL_BANDWIDTH, (x == channelToLog));
                channelPanel.setDFTSize(mChannelPanelDFTSize);

                if(x % mChannelsPerRow == mChannelsPerRow - 1)
                {
        getChildren().add(channelPanel);
                }
                else
                {
        getChildren().add(channelPanel);
                }
            }
        }
    }

    public class DiscreteIndexChannelArrayPanel extends VBox
    {
        public DiscreteIndexChannelArrayPanel()
        {
            int bufferSize = CHANNEL_BANDWIDTH / CHANNEL_FFT_FRAME_RATE;
            if(bufferSize % 2 == 1)
            {
                bufferSize++;
            }

            init();
        }

        private void init()
        {
            

            for(int x = 0; x < mChannelCount; x++)
            {
                TunerChannel tunerChannel = new TunerChannel(100000000, 12500);
                TunerChannelSource source = mTestTuner.getChannelSourceManager().getSource(tunerChannel, null, "test");
                DiscreteChannelPanel channelPanel = new DiscreteChannelPanel(mSettingsManager, source, x);
                channelPanel.setDFTSize(mChannelPanelDFTSize);

                mLog.debug("Testing Channel [" + x + "] is set to [" + source.getTunerChannel().getFrequency() + "]");

                if(x % mChannelsPerRow == mChannelsPerRow - 1)
                {
        getChildren().add(channelPanel);
                }
                else
                {
        getChildren().add(channelPanel);
                }
            }
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
            mSpectrumPanel.setSampleSize(28);
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

    public class ChannelPanel extends VBox implements Listener<ComplexSamples>, ISourceEventProcessor
    {
        private TunerChannelSource mSource;
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private ToggleButton mLoggingButton;
        private boolean mLoggingEnabled;

        public ChannelPanel(SettingsManager settingsManager, double sampleRate, long frequency, int bandwidth, boolean enableLogging)
        {
            
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(32);
        getChildren().add(mSpectrumPanel);

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            TunerChannel tunerChannel = new TunerChannel(frequency, bandwidth);
            mSource = mTestTuner.getChannelSourceManager().getSource(tunerChannel, null, "test");

            if(mSource != null)
            {
                mSource.setListener(complexSamples -> mComplexDftProcessor.receive(new FloatNativeBuffer(complexSamples.toInterleaved())));

                mSource.start();
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for frequency: " + frequency);
            }

            if(mSource != null)
            {
        getChildren().add(new Label("Center:" + frequency));
            }
            else
            {
        getChildren().add(new Label("NO SRC:" + frequency));
            }

            mLoggingButton = new ToggleButton("Logging");
            mLoggingButton.setOnAction(e -> {mLoggingEnabled = mLoggingButton.isSelected();
            });
//            getChildren().add(mLoggingButton);

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
        public void receive(ComplexSamples complexSamples)
        {
            mComplexDftProcessor.receive(new FloatNativeBuffer(complexSamples.toInterleaved()));
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public class DiscreteChannelPanel extends VBox implements Listener<ComplexSamples>, ISourceEventProcessor
    {
        private final Logger mLog = LoggerFactory.getLogger(DiscreteChannelPanel.class);

        private TunerChannelSource mSource;
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private ToggleButton mLoggingButton;
        private boolean mLoggingEnabled;

        public DiscreteChannelPanel(SettingsManager settingsManager, TunerChannelSource source, int index)
        {
            mSource = source;
            
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(32);
        getChildren().add(mSpectrumPanel);
        getChildren().add(new Label("Index:" + index));

            mLoggingButton = new ToggleButton("Logging");
            mLoggingButton.setOnAction(e -> {mLoggingEnabled = mLoggingButton.isSelected();
            });
//            getChildren().add(mLoggingButton);

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            if(mSource != null)
            {
                mSource.setListener(new Listener<ComplexSamples>()
                {
                    @Override
                    public void receive(ComplexSamples complexSamples)
                    {
                        if(mLoggingEnabled)
                        {
                            mLog.debug("Samples:" + Arrays.toString(complexSamples.toInterleaved().samples()));
                        }

                        mComplexDftProcessor.receive(new FloatNativeBuffer(complexSamples.toInterleaved()));
                    }
                });

                mSource.start();
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for index: " + index);
            }
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
        public void receive(ComplexSamples complexSamples)
        {
            mComplexDftProcessor.receive(new FloatNativeBuffer(complexSamples.toInterleaved()));
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }


    public static void main(String[] args)
    {
        boolean useGUI = true;

        if(useGUI)
        {
            int channelsPerRow = 16;

            final ChannelizerViewer frame = new ChannelizerViewer(channelsPerRow);

            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    frame.show();
                }
            });
        }
        else
        {
            TestTuner tuner = new TestTuner(new LoggingTunerErrorListener());

            List<TunerChannel> tunerChannels = getTunerChannels(tuner);

            List<TunerChannelSource> sources = new ArrayList<>();

            int maxSourceCount = 30;
            int sourceCount = 0;
            for(TunerChannel tunerChannel : tunerChannels)
            {
                if(sourceCount < maxSourceCount)
                {
                    TunerChannelSource source = tuner.getChannelSourceManager().getSource(tunerChannel, null, "test");

                    if(source != null)
                    {
                        sources.add(source);
                        sourceCount++;
                    }
                    else
                    {
                        mLog.debug("Couldn't get source for: " + tunerChannel);
                    }
                }
            }

            mLog.debug("Starting [" + sources.size() + "] tuner channel sources");

            for(TunerChannelSource tunerChannelSource : sources)
            {
                tunerChannelSource.setListener(new Listener<ComplexSamples>()
                {
                    @Override
                    public void receive(ComplexSamples complexSamples)
                    {
                    }
                });

                tunerChannelSource.start();
            }

            while(true)
            {
                ;
            }
        }
    }
}