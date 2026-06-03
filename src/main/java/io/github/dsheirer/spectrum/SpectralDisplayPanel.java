


/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.spectrum;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;

import javafx.scene.control.ScrollPane;

import javafx.scene.control.Menu;
import javafx.scene.control.Slider;

import javafx.scene.control.ContextMenu;

import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.Button;
import javafx.scene.control.Label;


import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.ColorSettingMenuItem;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.spectrum.OverlayPanel.ChannelDisplay;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import io.github.dsheirer.spectrum.menu.AveragingItem;
import io.github.dsheirer.spectrum.menu.DFTSizeItem;
import io.github.dsheirer.spectrum.menu.FFTWindowTypeItem;
import io.github.dsheirer.spectrum.menu.FrameRateItem;
import io.github.dsheirer.spectrum.menu.SmoothingItem;
import io.github.dsheirer.spectrum.menu.SmoothingTypeItem;
import javafx.scene.Node;

import javafx.scene.input.ScrollEvent;
import java.util.ArrayList;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;







import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.geometry.Orientation;
import javafx.application.Platform;












public class SpectralDisplayPanel extends javafx.scene.layout.StackPane
        implements Listener<INativeBuffer>, ISourceEventProcessor, IDFTWidthChangeProcessor
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(SpectralDisplayPanel.class);

    public static final String FFT_SIZE_PROPERTY = "spectral.display.dft.size";
    public static final String SPECTRAL_DISPLAY_ENABLED = "spectral.display.enabled";
    public static final int NO_ZOOM = 0;
    public static final int MAX_ZOOM = 6;

    private DFTSize mDFTSize = DFTSize.FFT04096;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    private ScrollPane mScrollPane;
    private StackPane mLayeredPanel;
    private SpectrumPanel mSpectrumPanel;
    private WaterfallPanel mWaterfallPanel;
    private OverlayPanel mOverlayPanel;
    private ComplexDftProcessor mComplexDftProcessor;
    private DFTResultsConverter mDFTConverter;
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;
    private SettingsManager mSettingsManager;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private Tuner mTuner;
    private io.github.dsheirer.preference.UserPreferences mUserPreferences;

    /**
     * Spectral Display Panel provides a frequency component display with a
     * historical waterfall display and a transparent overlay to show frequency,
     * cursor and channel information.
     * <p>
     * Mouse scrolling and zooming are supported and the waterfall display can
     * be paused.
     * <p>
     * Complex sample buffers are processed by a DFTProcessor and the output of
     * the DFT is translated to decibels for display in the spectrum and
     * waterfall components.
     */
    public SpectralDisplayPanel(PlaylistManager playlistManager, SettingsManager settingsManager, DiscoveredTunerModel discoveredTunerModel)
    {
        mChannelModel = playlistManager.getChannelModel();
        mChannelProcessingManager = playlistManager.getChannelProcessingManager();
        mSettingsManager = settingsManager;
        mDiscoveredTunerModel = discoveredTunerModel;
        mUserPreferences = playlistManager.getUserPreferences();

        mSpectrumPanel = new SpectrumPanel(mSettingsManager);
        mOverlayPanel = new OverlayPanel(mSettingsManager, mChannelModel, mChannelProcessingManager);
        mWaterfallPanel = new WaterfallPanel(mSettingsManager);

        init();

        loadSettings();
    }

    private void loadSettings()
    {
        SystemProperties properties = SystemProperties.getInstance();

        String rawSize = properties.get(FFT_SIZE_PROPERTY, DFTSize.FFT04096.name());

        DFTSize size = null;

        if(rawSize != null)
        {
            try
            {
                size = DFTSize.valueOf(rawSize);
            }
            catch(Exception e)
            {
                //Do nothing
            }
        }

        if(size == null)
        {
            size = DFTSize.FFT04096;
        }

        setDFTSize(size, false);
    }

    public void dispose()
    {
        /* De-register from receiving samples when the window closes */
        clearTuner();

        /* Unregister from EventBus to prevent memory leak */
        MyEventBus.getGlobalEventBus().unregister(this);

        mSettingsManager = null;

        mComplexDftProcessor.dispose();
        mComplexDftProcessor = null;

        mDFTConverter.dispose();
        mDFTConverter = null;

        mSpectrumPanel.dispose();
        mSpectrumPanel = null;

        mWaterfallPanel.dispose();
        mWaterfallPanel = null;

        mOverlayPanel.dispose();
        mOverlayPanel = null;

        mTuner = null;
    }

    /**
     * Queues an FFT size change request.  The scheduled executor will apply
     * the change when it runs.
     */
    public void setDFTSize(DFTSize size, boolean save)
    {
        mComplexDftProcessor.setDFTSize(size);
        mOverlayPanel.setDFTSize(size);
        mDFTSize = size;

        if(save)
        {
            SystemProperties.getInstance().set(FFT_SIZE_PROPERTY, size.name());
        }

        setZoom(0, 0, 0);
    }

    public void setDFTSize(DFTSize size)
    {
        setDFTSize(size, true);
    }

    @Override public DFTSize getDFTSize()
    {
        return mDFTSize;
    }

    public int getZoom()
    {
        return mZoom;
    }

    /**
     * Sets the current zoom level which will be 2 to the power of zoom (2^zoom)
     * <p>
     * 0 	No Zoom
     * 1	2x Zoom
     * 2	4x Zoom
     * 3	8x Zoom
     * 4	16x Zoom
     * 5	32x Zoom
     * 6    64x Zoom
     *
     * @param zoom         level, 0 - 6.
     * @param frequency    under the mouse to maintain while zooming
     * @param windowOffset where to maintain the frequency under the mouse
     */
    public void setZoom(int zoom, long frequency, double windowOffset)
    {
        if(zoom < NO_ZOOM)
        {
            zoom = NO_ZOOM;
        }
        else if(zoom > MAX_ZOOM)
        {
            zoom = MAX_ZOOM;
        }

        if(zoom != mZoom)
        {
            mZoom = zoom;

            //Calculate the bin offset that would place the reference frequency
            //at the left edge of the zoom window.
            double binOffsetToFrequency = getBinOffset(frequency);

            //Calculate the bin offset into the newly sized zoom window that
            //would place the frequency in the same proportional window location
            //that it was in the previous zoom size
            double windowBinOffset = (double)getZoomWindowSizeInBins() * windowOffset;

            //Set the overall offset to place the reference frequency in the
            //same location in the newly zoomed window
            double offset = binOffsetToFrequency - windowBinOffset;

            mSpectrumPanel.setZoom(mZoom);
            mOverlayPanel.setZoom(mZoom);
            mWaterfallPanel.setZoom(mZoom);

            setZoomWindowOffset(offset);
        }
    }

    /**
     * Sets the offset (in DFT bins) to the first bin that the zoom window displays
     */
    public void setZoomWindowOffset(double offset)
    {
        if(offset < 0)
        {
            offset = 0;
        }

        if(offset > (mDFTSize.getSize() - getZoomWindowSizeInBins()))
        {
            offset = mDFTSize.getSize() - getZoomWindowSizeInBins();
        }

        mDFTZoomWindowOffset = (int)offset;

        mSpectrumPanel.setZoomWindowOffset(mDFTZoomWindowOffset);
        mOverlayPanel.setZoomWindowOffset(mDFTZoomWindowOffset);
        mWaterfallPanel.setZoomWindowOffset(mDFTZoomWindowOffset);
    }

    /**
     * Calculates the size of the current zoom window in DFT bins
     */
    private int getZoomWindowSizeInBins()
    {
        return mDFTSize.getSize() / SpectrumUtils.getZoomMultiplier(mZoom);
    }

    

    /**
     * Calculates the overall offset of the frequency from the current minimum
     * frequency in terms of total FFT width
     *
     * @param frequency
     * @return
     */
    private double getBinOffset(long frequency)
    {
        double offset = 0.0;

        if(mOverlayPanel.containsFrequency(frequency))
        {
            offset = (double)mDFTSize.getSize() * ((double)(frequency - mOverlayPanel.getMinFrequency())
                    / (double)mOverlayPanel.getBandwidth());
        }

        return offset;
    }

    /**
     * Overrides Region method to return false, since we have overlapping
     * panels with the spectrum and channel panels
     */
    public boolean isOptimizedDrawingEnabled()
    {
        return false;
    }

    private void init()
    {
        mLayeredPanel = new StackPane();
        mLayeredPanel.getChildren().addAll(mSpectrumPanel, mOverlayPanel);
        mLayeredPanel.setMinHeight(0);
        mWaterfallPanel.setMinHeight(0);
        
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(mLayeredPanel, mWaterfallPanel);
        splitPane.setDividerPositions(0.5);

        getChildren().add(splitPane);

        MouseEventProcessor mouser = new MouseEventProcessor();
        
        mOverlayPanel.setOnMousePressed(mouser::mousePressedFX);
        mOverlayPanel.setOnMouseDragged(mouser::mouseDraggedFX);
        mOverlayPanel.setOnMouseMoved(mouser::mouseMovedFX);
        mOverlayPanel.setOnScroll(mouser::mouseScrollFX);
        mOverlayPanel.setOnMouseEntered(mouser::mouseEnteredFX);
        mOverlayPanel.setOnMouseExited(mouser::mouseExitedFX);
        mOverlayPanel.setOnMouseClicked(mouser::mouseClickedFX);

        mWaterfallPanel.setOnMousePressed(mouser::mousePressedFX);
        mWaterfallPanel.setOnMouseDragged(mouser::mouseDraggedFX);
        mWaterfallPanel.setOnMouseMoved(mouser::mouseMovedFX);
        mWaterfallPanel.setOnScroll(mouser::mouseScrollFX);
        mWaterfallPanel.setOnMouseEntered(mouser::mouseEnteredFX);
        mWaterfallPanel.setOnMouseExited(mouser::mouseExitedFX);
        mWaterfallPanel.setOnMouseClicked(mouser::mouseClickedFX);

        mComplexDftProcessor = new ComplexDftProcessor();
        mDFTConverter = new ComplexDecibelConverter();
        mComplexDftProcessor.addConverter(mDFTConverter);

        mDFTConverter.addListener((DFTResultsListener)mSpectrumPanel);
        mDFTConverter.addListener((DFTResultsListener)mWaterfallPanel);

        MyEventBus.getGlobalEventBus().register(this);
    }

    @com.google.common.eventbus.Subscribe
    public void onRemoteDesktopMode(io.github.dsheirer.eventbus.RemoteDesktopModeEvent event) {
        javafx.application.Platform.runLater(() -> {
            boolean active = event.isActive();
            if (active && mUserPreferences.getApplicationPreference().isRemoteAccessOptimization()) {
                // Throttle FFT framerate to 1 FPS
                mComplexDftProcessor.setFrameRate(1);
                // Disable animations globally via CSS/system properties or setting
                System.setProperty("javafx.animation.fullspeed", "false"); // Note: there's no native global toggle, but we can set properties that affect our code or we can just toggle CSS
                System.setProperty("prism.lcdtext", "false"); // Disable sub-pixel LCD text antialiasing
                
                mLog.info("Applied Remote Desktop optimizations (FFT 1 FPS, animations disabled).");
            } else {
                mComplexDftProcessor.setFrameRate(30); // Or whatever the default is
                System.setProperty("prism.lcdtext", "true");
                mLog.info("Restored standard display performance settings.");
            }
        });
    }

    public void process(SourceEvent event)
    {
        mOverlayPanel.process(event);
    }

    /**
     * Complex sample buffer receive method
     */
    @Override public void receive(INativeBuffer nativeBuffer)
    {
        mComplexDftProcessor.receive(nativeBuffer);
    }

    /**
     * Responds to tuner event by deregistering from the current
     * complex sample buffer source and registering with the tuner argument.
     */
    public void showTuner(Tuner tuner)
    {
        clearTuner();

        mComplexDftProcessor.clearBuffer();

        mComplexDftProcessor.start();

        mTuner = tuner;

        if(mTuner != null)
        {
            //Register the dft processor to receive samples from the tuner
            mTuner.getTunerController().addBufferListener(mComplexDftProcessor);

            //Verify that the tuner is still non-null, in case it encountered an error on starting sample stream
            if(mTuner != null)
            {
                //Register to receive frequency change events
                mTuner.getTunerController().addListener(this);

                mSpectrumPanel.setSampleSize(mTuner.getSampleSize());

                //Fire frequency and sample rate change events so that the spectrum
                //and overlay panels can synchronize
                process(SourceEvent.frequencyChange(null, mTuner.getTunerController().getFrequency()));
                process(SourceEvent.sampleRateChange(mTuner.getTunerController().getSampleRate()));
            }
        }
    }

    /**
     * Tuner de-selection cleanup method
     */
    public void clearTuner()
    {
        if(mTuner != null)
        {
            //Deregister for frequency change events from the tuner
            mTuner.getTunerController().removeListener(SpectralDisplayPanel.this);

            //Deregister the dft processor from receiving samples
            mTuner.getTunerController().removeBufferListener(mComplexDftProcessor);
            mTuner = null;
        }

        mComplexDftProcessor.stop();
        mComplexDftProcessor.clearBuffer();
        mSpectrumPanel.clearSpectrum();
        mWaterfallPanel.clearWaterfall();
    }

    /**
     * Resumes the DFT processor (if a tuner is set).
     */
    public void start() {
        if (mTuner != null) {
            mComplexDftProcessor.start();
        }
    }

    /**
     * Stops the DFT processor.
     */
    public void stop() {
        mComplexDftProcessor.stop();
    }

    /**
     * Currently displayed tuner
     */
    public Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Monitors the sizing of the layered pane and resizes the spectrum and
     * channel panels whenever the layered pane is resized
     */
    public class MouseEventProcessor 
    {
        private int mDFTZoomWindowOffsetAtDragStart = 0;
        private int mDragStartX = 0;
        private double mPixelsPerBin;

        public MouseEventProcessor()
        {
        }

        public void mouseScrollFX(javafx.scene.input.ScrollEvent e) {
            int zoom = mZoom + (int)Math.signum(e.getDeltaY());
            long frequency = mOverlayPanel.getFrequencyFromAxis(e.getX());
            double windowOffset = e.getX() / getWidth();
            setZoom(zoom, frequency, windowOffset);
        }

        public void mouseMovedFX(javafx.scene.input.MouseEvent event) {
            updateFX(event);
        }

        public void mouseDraggedFX(javafx.scene.input.MouseEvent event) {
            updateFX(event);
            int dragDistance = mDragStartX - (int)event.getX();
            double binDistance = (double)dragDistance / mPixelsPerBin;
            int offset = (int)(mDFTZoomWindowOffsetAtDragStart + binDistance);
            if(offset < 0) offset = 0;
            int maxOffset = mDFTSize.getSize() - (mDFTSize.getSize() / SpectrumUtils.getZoomMultiplier(mZoom));
            if(offset > maxOffset) offset = maxOffset;
            setZoomWindowOffset(offset);
        }

        public void mousePressedFX(javafx.scene.input.MouseEvent e) {
            mDragStartX = (int)e.getX();
            mDFTZoomWindowOffsetAtDragStart = mDFTZoomWindowOffset;
            mPixelsPerBin = (double)((javafx.scene.layout.Region)getParent()).getWidth() / ((double)(mDFTSize.getSize()) / (double)SpectrumUtils.getZoomMultiplier(mZoom));
        }

        private void updateFX(javafx.scene.input.MouseEvent event) {
            if(event.getSource() == mOverlayPanel) {
                mOverlayPanel.setCursorLocation(new javafx.geometry.Point2D(event.getX(), event.getY()));
            } else {
                mWaterfallPanel.setCursorLocation(new javafx.geometry.Point2D(event.getX(), event.getY()));
                mWaterfallPanel.setCursorFrequency(mOverlayPanel.getFrequencyFromAxis(event.getX()));
            }
        }

        public void mouseEnteredFX(javafx.scene.input.MouseEvent event) {
            if(event.getSource() == mOverlayPanel) {
                mOverlayPanel.setCursorVisible(true);
            } else {
                mWaterfallPanel.setCursorVisible(true);
            }
        }

        public void mouseExitedFX(javafx.scene.input.MouseEvent event) {
            if(event.getSource() == mOverlayPanel) {
                mOverlayPanel.setCursorVisible(false);
            } else {
                mWaterfallPanel.setCursorVisible(false);
            }
        }

        public void mouseClickedFX(javafx.scene.input.MouseEvent event) {
            if(event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                Platform.runLater(() -> showContextMenu(event));
            }
        }

        private void showContextMenu(javafx.scene.input.MouseEvent event) {
            ContextMenu contextMenu = new ContextMenu();

            if(event.getSource() == mWaterfallPanel) {
                contextMenu.getItems().add(new PauseItem(mWaterfallPanel, "Pause"));
                contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }

            long frequency = mOverlayPanel.getFrequencyFromAxis(event.getX());

            if(event.getSource() == mOverlayPanel) {
                ArrayList<Channel> channels = mOverlayPanel.getChannelsAtFrequency(frequency);
                Menu channelMenu = new Menu("Channels");
                for(Channel channel : channels) {
                    MenuItem viewChannel = new MenuItem("View/Edit: " + channel.getShortTitle());
                    viewChannel.setOnAction(e -> MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel)));
                    channelMenu.getItems().add(viewChannel);
                }
                contextMenu.getItems().add(channelMenu);
                if(!channels.isEmpty()) contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }

            Menu colorMenu = new Menu("Color");
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.CHANNEL_CONFIG));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.CHANNEL_CONFIG_PROCESSING));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.CHANNEL_CONFIG_SELECTED));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.SPECTRUM_CURSOR));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.SPECTRUM_LINE));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.SPECTRUM_BACKGROUND));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.SPECTRUM_GRADIENT_BOTTOM));
            colorMenu.getItems().add(new ColorSettingMenuItem(mSettingsManager, ColorSettingName.SPECTRUM_GRADIENT_TOP));
            contextMenu.getItems().add(colorMenu);

            Menu displayMenu = new Menu("Display");
            contextMenu.getItems().add(displayMenu);

            if(event.getSource() != mWaterfallPanel) {
                Menu averagingMenu = new Menu("Averaging");
                averagingMenu.getItems().add(new javafx.scene.control.CustomMenuItem(new AveragingItem(mSpectrumPanel, 4), false));
                displayMenu.getItems().add(averagingMenu);

                Menu channelDisplayMenu = new Menu("Channel");
                channelDisplayMenu.getItems().add(new ChannelDisplayItem(mOverlayPanel, ChannelDisplay.ALL));
                channelDisplayMenu.getItems().add(new ChannelDisplayItem(mOverlayPanel, ChannelDisplay.ENABLED));
                channelDisplayMenu.getItems().add(new ChannelDisplayItem(mOverlayPanel, ChannelDisplay.NONE));
                displayMenu.getItems().add(channelDisplayMenu);
            }

            Menu fftWidthMenu = new Menu("FFT Width");
            displayMenu.getItems().add(fftWidthMenu);
            for(DFTSize width : DFTSize.values()) fftWidthMenu.getItems().add(new DFTSizeItem(SpectralDisplayPanel.this, width));

            Menu frameRateMenu = new Menu("Frame Rate");
            displayMenu.getItems().add(frameRateMenu);
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 14));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 16));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 18));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 20));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 25));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 30));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 40));
            frameRateMenu.getItems().add(new FrameRateItem(mComplexDftProcessor, 50));

            Menu fftWindowType = new Menu("Window Type");
            displayMenu.getItems().add(fftWindowType);
            for(WindowType type : WindowType.values()) fftWindowType.getItems().add(new FFTWindowTypeItem(mComplexDftProcessor, type));

            if(event.getSource() != mWaterfallPanel) {
                Menu smoothingMenu = new Menu("Smoothing");
                if(mSpectrumPanel.getSmoothingType() != SmoothingType.NONE) {
                    smoothingMenu.getItems().add(new javafx.scene.control.CustomMenuItem(new SmoothingItem(mSpectrumPanel, 5), false));
                    smoothingMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                }
                smoothingMenu.getItems().add(new SmoothingTypeItem(mSpectrumPanel, SmoothingType.GAUSSIAN));
                smoothingMenu.getItems().add(new SmoothingTypeItem(mSpectrumPanel, SmoothingType.TRIANGLE));
                smoothingMenu.getItems().add(new SmoothingTypeItem(mSpectrumPanel, SmoothingType.RECTANGLE));
                smoothingMenu.getItems().add(new SmoothingTypeItem(mSpectrumPanel, SmoothingType.NONE));
                displayMenu.getItems().add(smoothingMenu);
            }

            javafx.scene.control.Menu zoomMenu = new Menu("Zoom");
            double windowOffset = (double)event.getX() / (double)getWidth();
            zoomMenu.getItems().add(new javafx.scene.control.CustomMenuItem(new ZoomItem(frequency, windowOffset), false));
            contextMenu.getItems().add(zoomMenu);

            if(mTuner != null) {
                contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                contextMenu.getItems().add(new DisableSpectrumWaterfallMenuItem(SpectralDisplayPanel.this));
            }

            boolean separatorAdded = false;
            for(DiscoveredTuner discoveredTuner : mDiscoveredTunerModel.getAvailableTuners()) {
                if(mTuner == null || mTuner != discoveredTuner.getTuner()) {
                    if(!separatorAdded) {
                        contextMenu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
                        separatorAdded = true;
                    }
                    contextMenu.getItems().add(new ShowTunerMenuItem(mDiscoveredTunerModel, discoveredTuner.getTuner()));
                }
            }

            if(contextMenu != null) {
                int yOffset = event.getSource() == mOverlayPanel ? 0 : (int)(getHeight() * 0.5);
                contextMenu.show(SpectralDisplayPanel.this, (int)event.getX(), (int)event.getY() + yOffset);
            }
        }
    }

    public class PauseItem extends javafx.scene.control.CheckMenuItem {
        private Pausable mPausable;
        public PauseItem(Pausable pausable, String label) {
            super(label);
            final boolean paused = pausable.isPaused();
            setSelected(paused);
            mPausable = pausable;
            setOnAction(e -> {
                javafx.application.Platform.runLater(() -> {
                    mPausable.setPaused(!paused);
                });
            });
        }
    }

    public class ZoomItem extends Slider
    {
        private static final long serialVersionUID = 1L;

        private long mFrequency;
        private double mWindowOffset;
        private javafx.beans.value.ChangeListener<Number> mChangeListener;

        public ZoomItem(long frequency, double windowOffset)
        {
            super(NO_ZOOM, MAX_ZOOM, mZoom);

            mFrequency = frequency;
            mWindowOffset = windowOffset;

            setMajorTickUnit(1);
            setMinorTickCount(0);
            setShowTickMarks(true);
            setShowTickLabels(true);
            setSnapToTicks(true);
            setBlockIncrement(1);
            setPrefWidth(200);

            mChangeListener = (observable, oldValue, newValue) -> {
                setZoom(newValue.intValue(), mFrequency, mWindowOffset);
            };
            this.valueProperty().addListener(mChangeListener);
        }
    }

    public class ChannelDisplayItem extends javafx.scene.control.CheckMenuItem {
        private OverlayPanel mOverlayPanel;
        private ChannelDisplay mChannelDisplay;
        public ChannelDisplayItem(OverlayPanel panel, ChannelDisplay display) {
            super(display.name());
            mOverlayPanel = panel;
            mChannelDisplay = display;
            setSelected(mOverlayPanel.getChannelDisplay() == mChannelDisplay);
            setOnAction(e -> {
                javafx.application.Platform.runLater(() -> {
                    mOverlayPanel.setChannelDisplay(mChannelDisplay);
                });
            });
        }
    }
}