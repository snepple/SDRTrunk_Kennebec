package io.github.dsheirer.gui.control;

import javafx.scene.paint.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

/**
 * Power meter for displaying signal power levels in dB scale with optional peak value and squelch threshold lines.
 */
public class DbPowerMeter extends Region
{
    public static final double DEFAULT_MINIMUM_POWER = -110.0d;
    public static final double DEFAULT_MAXIMUM_POWER = 0.0d;
    private static final int BAR_WIDTH = 30;
    private static final int PADDING = 3;
    private static final int DOUBLE_PADDING = PADDING * 2;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final Color COLOR_BAR = Color.LIGHTGRAY;
    private static final Color COLOR_THRESHOLD = Color.BLUE;
    private static final Color COLOR_PEAK = Color.PINK;

    private double mMinimumValue = DEFAULT_MINIMUM_POWER;
    private double mMaximumValue = DEFAULT_MAXIMUM_POWER;
    private double mExtent = mMaximumValue - mMinimumValue;
    private double mPeak = mMinimumValue;
    private double mPower = mMinimumValue;
    private double mSquelchThreshold = mMinimumValue;

    private boolean mPeakVisible = false;
    private boolean mSquelchThresholdVisible = false;

    private Canvas mCanvas;

    /**
     * Constructs an instance.
     */
    public DbPowerMeter()
    {
        setPrefSize(90, 100);
        setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 3;");

        mCanvas = new Canvas();
        getChildren().add(mCanvas);
        mCanvas.widthProperty().bind(widthProperty());
        mCanvas.heightProperty().bind(heightProperty());
        mCanvas.widthProperty().addListener(e -> repaint());
        mCanvas.heightProperty().addListener(e -> repaint());
    }

    /**
     * Resets peak, power and squelch to minimums
     */
    public void reset()
    {
        mPeak = mMinimumValue;
        mPower = mMinimumValue;
        mSquelchThreshold = mMinimumValue;
        repaint();
    }

    private void repaint()
    {
        draw();
    }

    /**
     * Primary method for rendering the control
     */
    protected void draw()
    {
        GraphicsContext g = mCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight());

        double leftInset = getPadding().getLeft();
        double topInset = getPadding().getTop();
        double bottomInset = getPadding().getBottom();
        double height = getHeight() - topInset - bottomInset;

        //Draw meter border
        g.setStroke(Color.BLACK);
        g.strokeRect(leftInset, topInset, BAR_WIDTH, height);

        //Draw filled bar
        g.setFill(COLOR_BAR);
        double fillHeight = height - DOUBLE_PADDING;
        double barHeight = fillHeight * getPowerPercent() - PADDING;
        double top = (fillHeight - barHeight) + topInset + PADDING;
        g.fillRect(leftInset + PADDING, top, BAR_WIDTH - DOUBLE_PADDING, barHeight);

        //Draw threshold
        if(isSquelchThresholdVisible())
        {
            g.setStroke(COLOR_THRESHOLD);
            drawPercentLine(g, getSquelchThresholdPercent());
        }

        //Draw peak line
        if(isPeakVisible())
        {
            g.setStroke(COLOR_PEAK);
            drawPercentLine(g, getPeakPercent());
        }

        //Draw legend/scale
        g.setFill(Color.BLACK);
        for(double legendValue: getScaleValues())
        {
            drawScaleText(g, legendValue);
        }
    }

    /**
     * Draws a line as a percentage of the value range (extent) using the current graphics color.
     */
    private void drawPercentLine(GraphicsContext g, double percentValue)
    {
        double topInset = getPadding().getTop();
        double bottomInset = getPadding().getBottom();
        double leftInset = getPadding().getLeft();

        double totalHeight = getHeight() - topInset - bottomInset - DOUBLE_PADDING;
        double height = totalHeight * percentValue - PADDING;
        double top = (totalHeight - height) + topInset + PADDING;
        g.strokeLine(leftInset, top, leftInset + BAR_WIDTH, top);
    }

    /**
     * Draws a tick and text for the value
     * @param g graphics object
     * @param value to draw
     */
    private void drawScaleText(GraphicsContext g, double value)
    {
        double percent = getPercent(value);
        double topInset = getPadding().getTop();
        double bottomInset = getPadding().getBottom();
        double leftInset = getPadding().getLeft();

        double totalHeight = getHeight() - topInset - bottomInset - DOUBLE_PADDING;
        double height = totalHeight * percent - PADDING;
        double top = (totalHeight - height) + topInset + PADDING;
        double left = leftInset + BAR_WIDTH - PADDING;
        double textOffset = 4;
        String label = DECIMAL_FORMAT.format(value);
        g.fillText(label, left + DOUBLE_PADDING, top + textOffset);
    }

    /**
     * Scale values to display, dynamically adapted to the height of the control
     */
    private List<Double> getScaleValues()
    {
        double ascent = 14;
        double labelCount = getHeight() / ascent;
        double interval = getExtent() / labelCount;
        List<Double> values = new ArrayList<>();
        for(double x = 0; x > getMinimumValue(); x -= interval)
        {
            values.add(x);
        }

        return values;
    }

    /**
     * Current power level
     * @return power level (dB)
     */
    public double getPower()
    {
        return mPower;
    }

    /**
     * Sets the current power level constrained to the minimum and maximum values for this control.
     * @param power (dB)
     */
    public void setPower(double power)
    {
        if(power > getMaximumValue())
        {
            mPower = getMaximumValue();
        }
        else if(power < getMinimumValue())
        {
            mPower = getMinimumValue();
        }
        else
        {
            mPower = power;
        }

        repaint();
    }

    /**
     * Squelch threshold value.
     * @return threshold (dB)
     */
    public double getSquelchThreshold()
    {
        return mSquelchThreshold;
    }

    /**
     * Sets the squelch thresold value
     * @param squelchThreshold (dB)
     */
    public void setSquelchThreshold(double squelchThreshold)
    {
        mSquelchThreshold = squelchThreshold;
        repaint();
    }

    /**
     * Minimum displayable power level
     * @return minimum (dB) - defaults to -110 dB
     */
    public double getMinimumValue()
    {
        return mMinimumValue;
    }

    /**
     * Sets the minimum displayable power level
     * @param minimumValue (dB)
     */
    public void setMinimumValue(double minimumValue)
    {
        mMinimumValue = minimumValue;
        updateExtent();
        repaint();
    }

    /**
     * Maximum displayable power level
     * @return maximum (dB) - defaults to 0 dB
     */
    public double getMaximumValue()
    {
        return mMaximumValue;
    }

    /**
     * Sets the maximum displayable power level
     * @param maximumValue (dB)
     */
    public void setMaximumValue(double maximumValue)
    {
        mMaximumValue = maximumValue;
        updateExtent();
        repaint();
    }

    /**
     * Updates the min/max extent
     */
    private void updateExtent()
    {
        mExtent = getMaximumValue() - getMinimumValue();
    }

    /**
     * Extent or range of displayable values.
     * @return extent (dB)
     */
    private double getExtent()
    {
        return mExtent;
    }

    /**
     * Peak value line
     * @return peak (dB)
     */
    public double getPeak()
    {
        return mPeak;
    }

    /**
     * Sets the peak value line
     * @param peak (db)
     */
    public void setPeak(double peak)
    {
        mPeak = peak;
        repaint();
    }

    /**
     * Indicates if the peak line is set to visible and the current peak value is within the min/max range.
     */
    public boolean isPeakVisible()
    {
        return mPeakVisible & isValidValue(getPeak());
    }

    /**
     * Sets the peak line visibility
     * @param peakVisible value
     */
    public void setPeakVisible(boolean peakVisible)
    {
        mPeakVisible = peakVisible;
        repaint();
    }

    /**
     * Indicates if the squelch threshold line is set to visible and the current squelch threshold value is
     * within the min/max displayable value range.
     * @return true if visible.
     */
    public boolean isSquelchThresholdVisible()
    {
        return mSquelchThresholdVisible & isValidValue(getSquelchThreshold());
    }

    /**
     * Sets the visibility of the squelch threshold line
     * @param squelchThresholdVisible visibility
     */
    public void setSquelchThresholdVisible(boolean squelchThresholdVisible)
    {
        mSquelchThresholdVisible = squelchThresholdVisible;
        repaint();
    }

    /**
     * Calculates the percentage of the min/max extent for the value
     * @param value to calculate
     * @return percentage of displayable value range
     */
    private double getPercent(double value)
    {
        return (value - getMinimumValue()) / getExtent();
    }

    /**
     * Power as percentage of displayable range
     */
    private double getPowerPercent()
    {
        return getPercent(mPower);
    }

    /**
     * Squelch threshold as percentage of displayable range
     */
    private double getSquelchThresholdPercent()
    {
        return getPercent(mSquelchThreshold);
    }

    /**
     * Peak as percentage of displayable range
     */
    private double getPeakPercent()
    {
        return getPercent(mPeak);
    }

    /**
     * Indicates if the value is valid, meaning it falls within the min/max range of displayable values.
     */
    private boolean isValidValue(double value)
    {
        return getMinimumValue() <= value && value <= getMaximumValue();
    }
}
