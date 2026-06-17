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
package io.github.dsheirer.spectrum;

import java.util.prefs.Preferences;
import io.github.dsheirer.preference.display.DisplayPreference;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import com.google.common.eventbus.Subscribe;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannel;











import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class OverlayPanel extends Pane implements Listener<ChannelEvent>, ISourceEventProcessor, SettingChangeListener
{
    private Canvas mCanvas = new Canvas();

    private Color toFXColor(javafx.scene.paint.Color c) {
        if (c == null) return Color.WHITE;
        return c;
    }

    private GraphicsContext mGraphicsContext = mCanvas.getGraphicsContext2D();
    private java.util.concurrent.atomic.AtomicBoolean repaintPending = new java.util.concurrent.atomic.AtomicBoolean(false);
    public void repaint() { 
        if (repaintPending.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                repaintPending.set(false);
                paintComponentFX();
            });
        }
    }
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(OverlayPanel.class);
    private final DecimalFormat PPM_FORMATTER = new DecimalFormat( "#.0" );

    

    static
    {
        
    }

    private static DecimalFormat CURSOR_FORMAT = new DecimalFormat("000.00000");
    private long mFrequency = 0;
    private int mBandwidth = 0;
    private Point2D mCursorLocation = new Point2D(0, 0);
    private boolean mCursorVisible = false;

    private DFTSize mDFTSize = DFTSize.FFT04096;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    //Hard upper bound on the number of frequency ticks rendered in a single paint.  A correctly sized
    //display draws at most a few hundred; this only ever engages if the tick increment math produces a
    //degenerate (near-zero) step, guarding the draw loop from pinning the JavaFX thread.
    private static final int MAX_FREQUENCY_TICKS = 4000;

    /**
     * Colors used by this component
     */
    private Color mColorChannelConfig;
    private Color mColorChannelConfigProcessing;
    private Color mColorChannelConfigSelected;
    private Color mColorSpectrumBackground;
    private Color mColorSpectrumCursor;
    private Color mColorSpectrumLine;

    //Currently visible/displayable channels
    private List<Channel> mVisibleChannels = new CopyOnWriteArrayList<>();
    private List<Channel> mTrafficChannels = new CopyOnWriteArrayList<>();

    private ChannelDisplay mChannelDisplay = ChannelDisplay.ALL;

    //Defines the offset at the bottom of the spectral display to account for
    //the frequency labels
    private Preferences mPreferences = Preferences.userNodeForPackage(DisplayPreference.class);
    private double mSpectrumInset = mPreferences.getDouble("spectrum.inset", 20.0);
    //Cache the overlay label sizing so the spectrum repaint path never reads the (Windows
    //registry-backed) Preferences on every label/frame. Reading getDouble() per paint pinned the
    //JavaFX thread in WindowsRegQueryValueEx and froze the UI when a tuner was streaming. These are
    //refreshed when DISPLAY preferences change.
    private double mLabelWidth = mPreferences.getDouble("overlay.label.width", 50.0);
    private double mLabelHeight = mPreferences.getDouble("overlay.label.height", 12.0);
    private LabelSizeManager mLabelSizeMonitor = new LabelSizeManager();

    private SettingsManager mSettingsManager;
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;

    /**
     * Translucent overlay panel for displaying channel configurations,
     * processing channels, selected channels, frequency labels and lines, and
     * a cursor with a frequency readout.
     */
    public OverlayPanel(SettingsManager settingsManager, ChannelModel channelModel, ChannelProcessingManager channelProcessingManager) {
        MyEventBus.getGlobalEventBus().register(this);
        mSettingsManager = settingsManager;

        if(mSettingsManager != null)
        {
            mSettingsManager.getSettingsModel().addListener(this);
        }

        mChannelModel = channelModel;

        if(mChannelModel != null)
        {
            mChannelModel.addListener(this::receive);
        }

        mChannelProcessingManager = channelProcessingManager;

        if(mChannelProcessingManager != null)
        {
            mChannelProcessingManager.addChannelEventListener(this::receive);
        }

        

        //Set the background transparent, so the spectrum display can be seen
        

        //Fetch color settings from settings manager
        setColors();

        this.getChildren().add(mCanvas);

        //Size the overlay canvas to track this panel.  A JavaFX Canvas does not resize on its own, so
        //without this binding the canvas stays 0x0 - which (a) makes the overlay invisible and, worse,
        //(b) collapses the label-spacing math (divide-by-zero on the canvas width) to a 1 Hz tick step,
        //pinning the JavaFX thread for millions of strokeText() calls across the full tuner bandwidth
        //and freezing the application on startup.  Matches SpectrumPanel/WaterfallPanel/FrequencyOverlayPanel.
        mCanvas.widthProperty().bind(widthProperty());
        mCanvas.heightProperty().bind(heightProperty());

        mCanvas.widthProperty().addListener((obs, oldVal, newVal) -> mLabelSizeMonitor.update());
        mCanvas.heightProperty().addListener((obs, oldVal, newVal) -> mLabelSizeMonitor.update());
    }

    public void dispose() {
        MyEventBus.getGlobalEventBus().unregister(this);
        if(mChannelModel != null)
        {
            mChannelModel.removeListener(this);
        }

        mChannelModel = null;

        mVisibleChannels.clear();

        if(mSettingsManager != null)
        {
            mSettingsManager.getSettingsModel().removeListener(this);
        }

        mSettingsManager = null;
    }

    /**
     * Refreshes cached display preferences (label sizing, spectrum inset) when the user changes DISPLAY
     * preferences, so the repaint path can use cached values instead of reading the registry per frame.
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DISPLAY)
        {
            mSpectrumInset = mPreferences.getDouble("spectrum.inset", 20.0);
            mLabelWidth = mPreferences.getDouble("overlay.label.width", 50.0);
            mLabelHeight = mPreferences.getDouble("overlay.label.height", 12.0);
            repaint();
        }
    }

    /**
     * Sets/changes the DFT bin size
     */
    public void setDFTSize(DFTSize size)
    {
        mDFTSize = size;
    }

    public ChannelDisplay getChannelDisplay()
    {
        return mChannelDisplay;
    }

    public void setChannelDisplay(ChannelDisplay display)
    {
        mChannelDisplay = display;
    }

    public void setCursorLocation(Point2D point)
    {
        mCursorLocation = point;

        repaint();
    }

    public void setCursorVisible(boolean visible)
    {
        mCursorVisible = visible;

        repaint();
    }

    /**
     * Sets the current zoom level (2^zoom)
     *
     * 0 	No Zoom
     * 1	2x Zoom
     * 2	4x Zoom
     * 3	8x Zoom
     * 4	16x Zoom
     * 5	32x Zoom
     * 6	64x Zoom
     *
     * @param zoom level, 0 - 6.
     */
    public void setZoom(int zoom)
    {
        mZoom = zoom;

        mLabelSizeMonitor.update();
    }

    public void setZoomWindowOffset(int offset)
    {
        mDFTZoomWindowOffset = offset;
    }

    /**
     * Fetches the color settings from the settings manager
     */
    private void setColors()
    {
        mColorChannelConfig = getColor(ColorSettingName.CHANNEL_CONFIG);
        mColorChannelConfigProcessing = getColor(ColorSettingName.CHANNEL_CONFIG_PROCESSING);
        mColorChannelConfigSelected = getColor(ColorSettingName.CHANNEL_CONFIG_SELECTED);
        mColorSpectrumCursor = getColor(ColorSettingName.SPECTRUM_CURSOR);
        mColorSpectrumLine = getColor(ColorSettingName.SPECTRUM_LINE);
        mColorSpectrumBackground = getColor(ColorSettingName.SPECTRUM_BACKGROUND);
    }

    /**
     * Fetches a named color setting from the settings manager.  If the setting
     * doesn't exist, creates the setting using the defaultColor
     */
    private Color getColor(ColorSettingName name)
    {
        ColorSetting setting = mSettingsManager.getSettingsModel().getColorSetting(name);

        return toFXColor(setting.getColor());
    }

    /**
     * Monitors for setting changes.  Colors can be changed by external actions
     * and will automatically update in this class
     */
    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting)setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_CONFIG:
                    mColorChannelConfig = toFXColor(colorSetting.getColor());
                    break;
                case CHANNEL_CONFIG_PROCESSING:
                    mColorChannelConfigProcessing = toFXColor(colorSetting.getColor());
                    break;
                case CHANNEL_CONFIG_SELECTED:
                    mColorChannelConfigSelected = toFXColor(colorSetting.getColor());
                    break;
                case SPECTRUM_BACKGROUND:
                    mColorSpectrumBackground = toFXColor(colorSetting.getColor());
                    break;
                case SPECTRUM_CURSOR:
                    mColorSpectrumCursor = toFXColor(colorSetting.getColor());
                    break;
                case SPECTRUM_LINE:
                    mColorSpectrumLine = toFXColor(colorSetting.getColor());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Renders the channel configs, lines, labels, and cursor
     */
    public void paintComponentFX()
    {
        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();

        //Clear to transparent only.  This canvas is layered on top of the spectrum panel in a StackPane,
        //so it must NOT paint an opaque background (SPECTRUM_BACKGROUND defaults to opaque black) or it
        //would hide the spectrum trace beneath it.  Matches FrequencyOverlayPanel, which only clears.
        mGraphicsContext.clearRect(0, 0, width, height);

        drawFrequencies(mGraphicsContext);
        drawChannels(mGraphicsContext);
        drawCursor(mGraphicsContext);
    }

    /**
     * Draws a cursor on the panel, whenever the mouse is hovering over the
     * panel
     */
    private void drawCursor(GraphicsContext graphics)
    {
        if(mCursorVisible)
        {
            drawFrequencyLine(graphics, mCursorLocation.getX(), mColorSpectrumCursor);

            String frequency = CURSOR_FORMAT.format(getFrequencyFromAxis(mCursorLocation.getX()) / 1E6D);

            

            double rectWidth = mLabelWidth; double rectHeight = mLabelHeight;

            if(mCursorLocation.getY() > rectHeight)
            {
                graphics.strokeText(frequency, mCursorLocation.getX() + 5, mCursorLocation.getY());
            }

            if(mZoom != 0)
            {
                graphics.strokeText("Zoom: " + (int)FastMath.pow(2.0, mZoom) + "x", mCursorLocation.getX() + 17,
                    mCursorLocation.getY() + 11);
            }
        }
    }

    /**
     * Draws the frequency lines and labels every 10kHz
     */
    private void drawFrequencies(GraphicsContext graphics)
    {
        

        long minFrequency = getMinDisplayFrequency();
        long maxFrequency = getMaxDisplayFrequency();

        //Frequency increments for label and tick spacing.  Read the major increment first: it forces the
        //spacing recalculation so all three increments come from the same pass (the label/minor getters
        //would otherwise return stale values from a previous layout).
        int major = mLabelSizeMonitor.getMajorTickIncrement(graphics);
        int label = mLabelSizeMonitor.getLabelIncrement(graphics);
        int minor = mLabelSizeMonitor.getMinorTickIncrement(graphics);

        //Avoid divide by zero error
        if(minor <= 0)
        {
            minor = 1;
        }
        if(label <= 0)
        {
            label = 1;
        }
        if(major <= 0)
        {
            major = 1;
        }

        //Defensive cap: regardless of how the increment math turned out, never step finer than the span
        //divided by MAX_FREQUENCY_TICKS.  Without this, a degenerate increment (e.g. from a not-yet
        //laid-out 0-width canvas) makes this loop advance 1 Hz at a time across the whole tuner
        //bandwidth, pinning the JavaFX thread for millions of strokeText() calls and freezing the UI.
        long span = maxFrequency - minFrequency;
        if(span > 0)
        {
            int minimumStep = (int)Math.min(Integer.MAX_VALUE, (span / MAX_FREQUENCY_TICKS) + 1);

            if(minor < minimumStep)
            {
                minor = minimumStep;
            }
            if(label < minor)
            {
                label = minor;
            }
            if(major < minor)
            {
                major = minor;
            }
        }

        //Adjust the start frequency to a multiple of the minor tick spacing
        long frequency = minFrequency - (minFrequency % minor);

        while(frequency < maxFrequency)
        {
            if(frequency % label == 0)
            {
                drawFrequencyLineAndLabel(graphics, frequency);
            }
            else if(frequency % major == 0)
            {
                drawTickLine(graphics, frequency, true);
            }
            else
            {
                drawTickLine(graphics, frequency, false);
            }

            frequency += minor;
        }
    }

    /**
     * Draws a vertical line and a corresponding frequency label at the bottom
     */
    private void drawFrequencyLineAndLabel(GraphicsContext graphics, long frequency)
    {
        double xAxis = getAxisFromFrequency(frequency);

        drawFrequencyLine(graphics, xAxis, mColorSpectrumLine);

        drawTickLine(graphics, frequency, false);

        graphics.setStroke(mColorSpectrumLine);

        drawFrequencyLabel(graphics, xAxis, frequency);
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawTickLine(GraphicsContext graphics, long frequency, boolean major)
    {
        graphics.setStroke(mColorSpectrumLine);

        double xAxis = getAxisFromFrequency(frequency);

        double start = mCanvas.getHeight() - mSpectrumInset;
        double end = start + (major ? 9.0d : 3.0d);

        graphics.strokeLine(xAxis, start, xAxis, end);
    }


    /**
     * Draws a vertical line at the xaxis
     */
    private void drawFrequencyLine(GraphicsContext graphics, double xaxis, Color color)
    {
        graphics.setStroke(color);

        graphics.strokeLine(xaxis, 0.0d, xaxis, mCanvas.getHeight() - mSpectrumInset);
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawChannelCenterLine(GraphicsContext graphics, double xaxis)
    {
        double height = mCanvas.getHeight() - mSpectrumInset;

        graphics.setStroke(Color.LIGHTGRAY);

        graphics.strokeLine(xaxis, height * 0.65d, xaxis, height - 1.0d);
    }

    /**
     * Draws the Automatic Frequency Control (AFC) channel center offset
     */
    private void drawAFC(GraphicsContext graphics, double frequencyAxis, double errorAxis, double bandwidth,
                         int correction, long frequency)
    {
        double height = mCanvas.getHeight() - mSpectrumInset;
        double verticalAxisTop = height * 0.88d;
        double verticalAxisBottom = height * 0.98d;

        double halfBandwidth = bandwidth / 2.0;
        double errorEdgeStart = errorAxis - halfBandwidth;
        double errorEdgeStop = errorAxis + halfBandwidth;

        graphics.setStroke(Color.YELLOW);

        //Horizontal line connecting frequency and error line
        graphics.strokeLine(errorEdgeStart, verticalAxisBottom, errorEdgeStop, verticalAxisBottom);

        //Vertical band edge lines
        graphics.strokeLine(errorEdgeStart, verticalAxisTop, errorEdgeStart, verticalAxisBottom);
        graphics.strokeLine(errorEdgeStop, verticalAxisTop, errorEdgeStop, verticalAxisBottom);

        double ppm = (double)correction / ((double)frequency / 1E6d);

        String label = "PPM " + PPM_FORMATTER.format(ppm) ;

        

        double rectWidth = mLabelWidth; double rectHeight = mLabelHeight;

        //Only render the correction value label if the spacing is large enough
        if(rectWidth <= bandwidth && rectHeight * 5 <= height)
        {
            graphics.strokeText(label, (float)(errorEdgeStart + 1.0), (float)(verticalAxisBottom - 2.0));
        }
    }

    /**
     * Returns the x-axis value corresponding to the frequency
     */
    private double getAxisFromFrequency(long frequency)
    {
        double screenWidth = (double)mCanvas.getWidth();

        double pixelsPerBin = screenWidth / (double)mDFTSize.getSize();

        double pixelOffsetToMinDisplayFrequency = pixelsPerBin * 2.0d;

        //Calculate frequency offset from the min frequency
        double frequencyOffset = (double)(frequency - getMinDisplayFrequency());

        //Determine ratio of frequency offset to overall bandwidth
        double ratio = frequencyOffset / (double)getDisplayBandwidth();

        //Apply the ratio to the screen width minus 1 bin width
        double screenOffset = screenWidth * ratio;

        return pixelOffsetToMinDisplayFrequency + screenOffset;
    }

    /**
     * Returns the frequency corresponding to the x-axis value using the current
     * zoom level.
     */
    public long getFrequencyFromAxis(double xAxis)
    {
        double width = mCanvas.getWidth();

        //Guard against an unlaid-out/zero-width canvas or non-finite input, which would otherwise divide by zero,
        //round Infinity to Long.MAX_VALUE, and overflow to a garbage (~Long.MIN_VALUE) frequency on the cursor.
        if(width <= 0 || !Double.isFinite(xAxis))
        {
            return getMinDisplayFrequency();
        }

        double offset = xAxis / width;

        long frequency = getMinDisplayFrequency() + FastMath.round((double)getDisplayBandwidth() * offset);

        if(frequency > (getMaxFrequency()))
        {
            frequency = getMaxFrequency();
        }

        if(frequency < getMinDisplayFrequency())
        {
            frequency = getMinDisplayFrequency();
        }

        return frequency;
    }

    /**
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel(GraphicsContext graphics, double xaxis, long frequency)
    {
        String label = mLabelSizeMonitor.format(frequency);

        

        double rectWidth = mLabelWidth; double rectHeight = mLabelHeight;

        float xOffset = (float)rectWidth / 2;

        graphics.strokeText(label, (float)(xaxis - xOffset), (float)(mCanvas.getHeight() - 2.0f));
    }


    /**
     * Draws visible channel configs as translucent shaded frequency regions
     */
    private void drawChannels(GraphicsContext graphics)
    {
        for(Channel channel : mVisibleChannels)
        {
            if(mChannelDisplay == ChannelDisplay.ALL || (mChannelDisplay == ChannelDisplay.ENABLED && channel.isProcessing()))
            {
                List<TunerChannel> tunerChannels = channel.getTunerChannels();

                for(TunerChannel tunerChannel: tunerChannels)
                {
                    if(tunerChannel.overlaps(getMinDisplayFrequency(), getMaxDisplayFrequency()))
                    {
                        //Choose the correct background color to use
                        if (channel.isSelected()) {
                            graphics.setFill(mColorChannelConfigSelected);
                        } else if (channel.isProcessing()) {
                            graphics.setFill(mColorChannelConfigProcessing);
                        } else {
                            graphics.setFill(mColorChannelConfig);
                        }

                        double xAxis = getAxisFromFrequency(tunerChannel.getFrequency());
                        double width = (double)(tunerChannel.getBandwidth()) / (double)getDisplayBandwidth() * mCanvas.getWidth();

                        graphics.fillRect(xAxis - (width / 2.0d), 0.0d, width,
                                mCanvas.getHeight() - mSpectrumInset);

                        //Fill the box with the correct color
                        
                        

                        //Change to the line color to render the channel name, etc.
                        graphics.setStroke(mColorSpectrumLine);

                        //Draw the labels starting at yAxis position 0
                        double yAxis = 0;

                        //Draw the system label and adjust the y-axis position
                        String system = channel.hasSystem() ? channel.getSystem() : " ";

                        yAxis += drawLabel(graphics, system, null, xAxis, yAxis, width);

                        //Draw the site label and adjust the y-axis position
                        String site = channel.hasSite() ? channel.getSite() : " ";

                        yAxis += drawLabel(graphics, site, null, xAxis, yAxis, width);

                        //Draw the channel label and adjust the y-axis position
                        yAxis += drawLabel(graphics, channel.getName(), null, xAxis, yAxis, width);

                        //Draw the decoder label
                        drawLabel(graphics, channel.getDecodeConfiguration().getDecoderType().getShortDisplayString(),
                                null, xAxis, yAxis, width);
                        long frequency = tunerChannel.getFrequency();
                        double frequencyAxis = getAxisFromFrequency(frequency);
                        drawChannelCenterLine(graphics, frequencyAxis);

                        /* Draw Automatic Frequency Control line */
                        int correction = channel.getChannelFrequencyCorrection();

                        if(correction != 0)
                        {
                            long error = frequency + correction;
                            drawAFC(graphics, frequencyAxis, getAxisFromFrequency(error), width, correction,
                                    tunerChannel.getFrequency());
                        }
                    }
                }
            }
        }
    }


    /**
     * Draws a textual label at the x/y position, clipping the end of the text
     * to fit within the maxwidth value.
     *
     * @return height of the drawn label
     */
    private double drawLabel(GraphicsContext graphics, String text, Font font, double x, double baseY, double maxWidth)
    {
        

        if(text == null || text.isEmpty())
        {
            return 0;
        }

        double labelWidth = mLabelWidth; double labelHeight = mLabelHeight;

        double offset = labelWidth / 2.0d;
        double y = baseY + labelHeight;

        /**
         * If the label is wider than the max width, left justify the text and
         * clip the end of it
         */
        if(offset > (maxWidth / 2.0d))
        {
            //label.setRect(x - (maxWidth / 2.0d), y - labelHeight, maxWidth, labelHeight);

            

            graphics.strokeText(text, (float)(x - (maxWidth / 2.0d)), (float)y);

            
        }
        else
        {
            graphics.strokeText(text, (float)(x - offset), (float)y);
        }

        return labelHeight;
    }

    /**
     * Frequency change event handler
     */
    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mBandwidth = event.getValue().intValue();
                mLabelSizeMonitor.update();
                break;
            case NOTIFICATION_FREQUENCY_CHANGE:
                mFrequency = event.getValue().longValue();
                mLabelSizeMonitor.update();
                break;
            default:
                break;
        }

        /**
         * Reset the visible channels list
         */
        mVisibleChannels.clear();
        mVisibleChannels.addAll(mChannelModel.getChannelsInFrequencyRange(getMinFrequency(), getMaxFrequency()));

        for(Channel trafficChannel: mTrafficChannels)
        {
            if(trafficChannel.isWithin(getMinFrequency(), getMaxFrequency()))
            {
                mVisibleChannels.add(trafficChannel);
            }
        }
    }

    /**
     * Channel change event handler
     */
    @Override
    public void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case NOTIFICATION_ADD:
            case NOTIFICATION_PROCESSING_START:
                if(channel.getChannelType() == ChannelType.TRAFFIC && !mTrafficChannels.contains(channel))
                {
                    mTrafficChannels.add(channel);
                }
                if(!mVisibleChannels.contains(channel) && channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.add(channel);
                }
                break;
            case NOTIFICATION_DELETE:
                mVisibleChannels.remove(channel);
                break;
            case NOTIFICATION_PROCESSING_STOP:
            case NOTIFICATION_PROCESSING_START_REJECTED:
                if(channel.getChannelType() == ChannelType.TRAFFIC)
                {
                    mVisibleChannels.remove(channel);
                    mTrafficChannels.remove(channel);
                }
                break;
            case NOTIFICATION_CONFIGURATION_CHANGE:
                if(mVisibleChannels.contains(channel) && !channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.remove(channel);
                }

                if(!mVisibleChannels.contains(channel) && channel.isWithin(getMinFrequency(), getMaxFrequency()))
                {
                    mVisibleChannels.add(channel);
                }
                break;
            default:
                break;
        }

        repaint();
    }

    public int getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Currently displayed minimum frequency
     */
    public long getMinFrequency()
    {
        return mFrequency - (mBandwidth / 2);
    }

    /**
     * Currently displayed maximum frequency
     */
    public long getMaxFrequency()
    {
        return mFrequency + (mBandwidth / 2);
    }

    public boolean containsFrequency(long frequency)
    {
        return FastMath.abs(mFrequency - frequency) <= (mBandwidth / 2);
    }

    private long getMinDisplayFrequency()
    {
        double bandwidthPerBin = (double)mBandwidth / (double)(mDFTSize.getSize());

        return getMinFrequency() + (int)((mDFTZoomWindowOffset) * bandwidthPerBin);
    }

    private long getMaxDisplayFrequency()
    {
        return getMinDisplayFrequency() + getDisplayBandwidth();
    }

    private int getDisplayBandwidth()
    {
        if(mZoom != 0)
        {
            return mBandwidth / (int)FastMath.pow(2.0, mZoom);
        }

        return mBandwidth;
    }

    /**
     * Returns a list of channel configs that contain the frequency within their
     * min/max frequency settings.
     */
    public ArrayList<Channel> getChannelsAtFrequency(long frequency)
    {
        ArrayList<Channel> configs = new ArrayList<Channel>();

        for(Channel config : mVisibleChannels)
        {
            List<TunerChannel> channels = config.getTunerChannels();

            for(TunerChannel channel: channels)
            {
                if(channel != null && channel.getMinFrequency() <= frequency && channel.getMaxFrequency() >= frequency)
                {
                    configs.add(config);
                }
            }
        }

        return configs;
    }

    @Override
    public void settingDeleted(Setting setting)
    { /* not implemented */ }

    /**
     * Calculates correct spacing and format for frequency labels and major/minor
     * tick lines based on current frequency, bandwidth, zoom and screen size.
     */
    public class LabelSizeManager
    {
        private static final double LABEL_FILL_THRESHOLD = 0.5d;

        private DecimalFormat mFrequencyFormat = new DecimalFormat("0.0");

        private boolean mUpdateRequired = true;
        private int mLabelIncrement = 1;
        private int mMajorTickIncrement = 1;
        private int mMinorTickIncrement = 1;

        public String format(long frequency)
        {
            return mFrequencyFormat.format((double)frequency / 1E6D);
        }

        private void setPrecision(int precision)
        {
            if(precision < 1)
            {
                precision = 1;
            }

            if(precision > 5)
            {
                precision = 5;
            }

            mFrequencyFormat.setMinimumFractionDigits(precision);
            mFrequencyFormat.setMaximumFractionDigits(precision);
        }

        private void update(GraphicsContext graphics)
        {
            if(mUpdateRequired)
            {
                double canvasWidth = OverlayPanel.this.mCanvas.getWidth();
                int displayBandwidth = getDisplayBandwidth();

                //Until the canvas has actually been laid out (width 0) and the tuner bandwidth is known,
                //the spacing math below divides by zero.  (int)log10(bandwidth/0) is (int)+Infinity =
                //Integer.MAX_VALUE, which then overflows the pow() calls to a 0 increment.  The draw loop
                //turns that into a 1 Hz step and renders one line + label per Hz across the entire
                //bandwidth, pinning the JavaFX thread and freezing the UI.  Fall back to a single
                //whole-bandwidth increment and leave the update pending so it recomputes once valid
                //dimensions arrive.
                if(canvasWidth <= 0 || displayBandwidth <= 0)
                {
                    int safe = Math.max(1, displayBandwidth);
                    mLabelIncrement = safe;
                    mMajorTickIncrement = safe;
                    mMinorTickIncrement = safe;
                    return;
                }

                //Set maximum precision as a starting point
                setPrecision(5);

                int maxLabelWidth = Math.max(1, (int)mLabelWidth);

                double maxLabels = (canvasWidth * LABEL_FILL_THRESHOLD) / (double)maxLabelWidth;

                if(maxLabels < 1.0d)
                {
                    maxLabels = 1.0d;
                }

                //Calculate the next smallest base 10 value for the major increment.  Clamp to >= 0 so the
                //pow() brackets below can never collapse to a 0 minimum (and therefore a 0 increment).
                int power = (int)FastMath.log10((double)displayBandwidth / maxLabels);

                if(power < 0)
                {
                    power = 0;
                }

                //Set the number of decimal places to display in frequency labels
                int precision = 5 - power;

                int start = (int)FastMath.pow(10.0, power + 1);

                int minimum = (int)FastMath.pow(10.0, power);

                int labelIncrement = start;

                while(((double)displayBandwidth / (double)labelIncrement) < maxLabels && labelIncrement >= minimum)
                {
                    labelIncrement /= 2;
                    precision++;
                }

                if(labelIncrement == minimum)
                {
                    precision = 5 - power;
                }

                setPrecision(precision);

                //Clamp every increment to >= 1 so the draw loop can never be handed a 0 (or negative) step.
                mLabelIncrement = Math.max(1, labelIncrement);
                mMajorTickIncrement = Math.max(1, labelIncrement / 2);
                mMinorTickIncrement = Math.max(1, labelIncrement / 10);

                mUpdateRequired = false;
            }
        }

        /**
         * Forces the display to update the label and frequency display
         * calculations
         */
        public void update()
        {
            mUpdateRequired = true;
        }

        public int getMajorTickIncrement(GraphicsContext graphics)
        {
            //Check to see if a calculation update is scheduled
            update(graphics);

            return mMajorTickIncrement;
        }

        public int getMinorTickIncrement(GraphicsContext graphics)
        {
            //Check to see if a calculation update is scheduled
            update(graphics);

            return mMinorTickIncrement;
        }

        public int getLabelIncrement(GraphicsContext graphics)
        {
            //Check to see if a calculation update is scheduled
            update(graphics);

            return mLabelIncrement;
        }



    }

    public enum ChannelDisplay
    {
        ALL, ENABLED, NONE;
    }
}

