

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
package io.github.dsheirer.spectrum;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;


import javafx.scene.canvas.Canvas; import javafx.scene.canvas.GraphicsContext;

import javafx.geometry.Point2D;





import java.text.DecimalFormat;
import org.apache.commons.math3.util.FastMath;



/**
 * Frequency overlay panel.
 */
public class FrequencyOverlayPanel extends Pane implements ISourceEventProcessor, SettingChangeListener
{
    private Canvas mCanvas = new Canvas();
    private static final long serialVersionUID = 1L;
    private final DecimalFormat PPM_FORMATTER = new DecimalFormat( "#.0" );
    

    private static DecimalFormat CURSOR_FORMAT = new DecimalFormat("000.00000");
    private long mFrequency = 0;
    private int mBandwidth = 0;
    private int mChannelBandwidth = 0;

    private long mEstimatedCarrierOffsetFrequency = 0;
    private Point2D mCursorLocation = new Point2D(0, 0);
    private boolean mCursorVisible = false;

    private DFTSize mDFTSize = DFTSize.FFT04096;

    /**
     * Colors used by this component
     */
    private Color mColorSpectrumBackground;
    private Color mColorSpectrumCursor;
    private Color mColorSpectrumLine;

    //Defines the offset at the bottom of the spectral display to account for
    //the frequency labels
    private double mSpectrumInset = 20.0d;
    private LabelSizeManager mLabelSizeMonitor = new LabelSizeManager();

    private SettingsManager mSettingsManager;

    /**
     * Translucent overlay panel for displaying channel configurations,
     * processing channels, selected channels, frequency labels and lines, and
     * a cursor with a frequency readout.
     */
    public FrequencyOverlayPanel(SettingsManager settingsManager)
    {
        mSettingsManager = settingsManager;

        if(mSettingsManager != null)
        {
            mSettingsManager.getSettingsModel().addListener(this);
        }

        // addComponentListener(mLabelSizeMonitor);
        
        getChildren().add(mCanvas);
        mCanvas.widthProperty().bind(widthProperty());
        mCanvas.heightProperty().bind(heightProperty());
        mCanvas.widthProperty().addListener((obs, oldVal, newVal) -> requestRedraw());
        mCanvas.heightProperty().addListener((obs, oldVal, newVal) -> requestRedraw());
        setBackground(javafx.scene.layout.Background.EMPTY);

        //Fetch color settings from settings manager
        setColors();
    }

    /**
     * Sets the estimated carrier offset of the channel's signal
     * @param estimatedCarrierOffsetFrequency current setting.  Set to 0 to reset.
     */
    public void setEstimatedCarrierOffsetFrequency(long estimatedCarrierOffsetFrequency)
    {
        mEstimatedCarrierOffsetFrequency = estimatedCarrierOffsetFrequency;
    }

    /**
     * Sets the bandwidth of the channel.
     * @param channelBandwidth in Hertz
     */
    public void setChannelBandwidth(int channelBandwidth)
    {
        mChannelBandwidth = channelBandwidth;
    }

    public void dispose()
    {
        if(mSettingsManager != null)
        {
            mSettingsManager.getSettingsModel().removeListener(this);
        }

        mSettingsManager = null;
    }

    /**
     * Sets/changes the DFT bin size
     */
    public void setDFTSize(DFTSize size)
    {
        mDFTSize = size;
    }


    private void requestRedraw() {
        javafx.application.Platform.runLater(() -> draw(mCanvas.getGraphicsContext2D()));
    }

    public void setCursorLocation(Point2D point)
    {
        mCursorLocation = point;

        requestRedraw();
    }

    public void setCursorVisible(boolean visible)
    {
        mCursorVisible = visible;
        requestRedraw();
    }

    /**
     * Fetches the color settings from the settings manager
     */
    private void setColors()
    {
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
        return setting.getColor();
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
                case SPECTRUM_BACKGROUND:
                    mColorSpectrumBackground = colorSetting.getColor();
                    break;
                case SPECTRUM_CURSOR:
                    mColorSpectrumCursor = colorSetting.getColor();
                    break;
                case SPECTRUM_LINE:
                    mColorSpectrumLine = colorSetting.getColor();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Renders the channel configs, lines, labels, and cursor
     */
    public void draw(GraphicsContext graphics) {
// // // // // //         graphics.clearRect(0, 0, getWidth(), getHeight());

        

        
// //         graphics.setBackground(mColorSpectrumBackground);
        
        drawFrequencies(graphics);
        drawCursor(graphics);
        drawEstimatedCarrierOffset(graphics);
        drawChannelBandwidth(graphics);
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
            double stringWidth = 50; double stringHeight = 12;
            

            if(mCursorLocation.getY() > stringHeight)
            {
                graphics.fillText(frequency, mCursorLocation.getX() + 5, mCursorLocation.getY());
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

        //Frequency increments for label and tick spacing
        int label = mLabelSizeMonitor.getLabelIncrement(graphics);
        int major = mLabelSizeMonitor.getMajorTickIncrement(graphics);
        int minor = mLabelSizeMonitor.getMinorTickIncrement(graphics);

        //Avoid divide by zero error
        if(minor == 0)
        {
            minor = 1;
        }
        if(label == 0)
        {
            label = 1;
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
        double start = getHeight() - mSpectrumInset;
        double end = start + (major ? 9.0d : 3.0d);
        graphics.strokeLine(xAxis, start, xAxis, end);
    }


    /**
     * Draws a vertical line at the xaxis
     */
    private void drawFrequencyLine(GraphicsContext graphics, double xaxis, Color color)
    {
graphics.setStroke(color);
        graphics.strokeLine(xaxis, 0.0d, xaxis, getHeight() - mSpectrumInset);
    }

    /**
     * Draws a vertical line at the xaxis
     */
    private void drawChannelCenterLine(GraphicsContext graphics, double xaxis)
    {
        double height = getHeight() - mSpectrumInset;
        graphics.setStroke(Color.LIGHTGRAY);
        graphics.strokeLine(xaxis, height * 0.65d, xaxis, height - 1.0d);
    }

    /**
     * Draws the estimated carrier offset measured by the CarrierOffsetProcessor
     */
    private void drawEstimatedCarrierOffset(GraphicsContext graphics)
    {
        if(mEstimatedCarrierOffsetFrequency == 0)
        {
            return;
        }

        long frequency = mFrequency + mEstimatedCarrierOffsetFrequency;
        double xAxis = getAxisFromFrequency(frequency);
// // // // // // 
        double height = getHeight() - mSpectrumInset;
        double verticalAxisTop = height * 0.02d;
        double verticalAxisBottom = height * 0.98d;

graphics.setStroke(Color.YELLOW);

        //Vertical band edge lines
        graphics.strokeLine(xAxis, verticalAxisTop, xAxis, verticalAxisBottom);
    }

    /**
     * Draws the channel bandwidth indicators
     */
    private void drawChannelBandwidth(GraphicsContext graphics)
    {
        if(mChannelBandwidth == 0)
        {
            return;
        }

        long minFrequency = mFrequency - (mChannelBandwidth / 2);
        long maxFrequency = mFrequency + (mChannelBandwidth / 2);

        double height = getHeight() - mSpectrumInset;
        double verticalAxisTop = height * 0.8d;
// // // // // //         double verticalAxisBottom = height * 0.98d;

        double minXAxis = getAxisFromFrequency(minFrequency);
        double maxXAxis = getAxisFromFrequency(maxFrequency);

graphics.setStroke(Color.GREEN);

        graphics.strokeLine(minXAxis, verticalAxisTop, minXAxis, getHeight());
        graphics.strokeLine(maxXAxis, verticalAxisTop, maxXAxis, getHeight());
    }

    /**
     * Returns the x-axis value corresponding to the frequency
     */
    private double getAxisFromFrequency(long frequency)
    {
        double screenWidth = (double)getWidth();
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
        double width = getWidth();
        double offset = xAxis / width;
        long frequency = getMinDisplayFrequency() + FastMath.round((double)getDisplayBandwidth() * offset);
        if(frequency > (getMaxFrequency()))
        {
            frequency = getMaxFrequency();
        }
        return frequency;
    }

    /**
     * Draws a frequency label at the x-axis position, at the bottom of the panel
     */
    private void drawFrequencyLabel(GraphicsContext graphics, double xaxis, long frequency)
    {
        String label = mLabelSizeMonitor.format(frequency);
        double stringWidth = 50; double stringHeight = 12;
        
        float xOffset = (float)stringWidth / 2;
        graphics.fillText(label, (float)(xaxis - xOffset), (float)(getHeight() - 2.0f));
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

        return getMinFrequency();
//        return getMinFrequency() + (int)((mDFTZoomWindowOffset) * bandwidthPerBin);
    }

    private long getMaxDisplayFrequency()
    {
        return getMinDisplayFrequency() + getDisplayBandwidth();
    }

    private int getDisplayBandwidth()
    {
        return mBandwidth;
    }

    @Override
    public void settingDeleted(Setting setting) { /* not implemented */ }

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

        private void recalculate()
        {
            if(mUpdateRequired)
            {
                //Set maximum precision as a starting point
                setPrecision(5);

                double stringWidth = 50;

                int maxLabelWidth = 50;

                double maxLabels = ((double) FrequencyOverlayPanel.this.getWidth() * LABEL_FILL_THRESHOLD) / (double)maxLabelWidth;

                //Calculate the next smallest base 10 value for the major increment
                int power = (int)FastMath.log10((double)getDisplayBandwidth() / maxLabels);

                //Set the number of decimal places to display in frequency labels
                int precision = 5 - power;

                int start = (int)FastMath.pow(10.0, power + 1);

                int minimum = (int)FastMath.pow(10.0, power);

                int labelIncrement = start;

                while(((double)getDisplayBandwidth() / (double)labelIncrement) < maxLabels && labelIncrement >= minimum)
                {
                    labelIncrement /= 2;
                    precision++;
                }

                if(labelIncrement == minimum)
                {
                    precision = 5 - power;
                }

                setPrecision(precision);

                mLabelIncrement = labelIncrement;
                mMajorTickIncrement = labelIncrement / 2;
                mMinorTickIncrement = labelIncrement / 10;

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
            recalculate();

            return mMajorTickIncrement;
        }

        public int getMinorTickIncrement(GraphicsContext graphics)
        {
            return mMinorTickIncrement;
        }

        public int getLabelIncrement(GraphicsContext graphics)
        {
            return mLabelIncrement;
        }

        

        
        
        
    }
}
