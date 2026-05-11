package io.github.dsheirer.gui.control;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DbPowerMeterJFX extends Canvas {
    public static final double DEFAULT_MINIMUM_POWER = -110.0d;
    public static final double DEFAULT_MAXIMUM_POWER = 0.0d;
    private static final int BAR_WIDTH = 30;
    private static final int PADDING = 3;
    private static final int DOUBLE_PADDING = PADDING * 2;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final Color COLOR_BAR = Color.LIGHTGRAY;
    private static final Color COLOR_THRESHOLD = Color.BLUE;
    private static final Color COLOR_PEAK = Color.PINK;
    private static final Color COLOR_FOREGROUND = Color.BLACK;

    private double mMinimumValue = DEFAULT_MINIMUM_POWER;
    private double mMaximumValue = DEFAULT_MAXIMUM_POWER;
    private double mPeak = mMinimumValue;
    private double mPower = mMinimumValue;
    private double mSquelchThreshold = mMinimumValue;

    private boolean mPeakVisible = false;
    private boolean mSquelchThresholdVisible = false;

    public DbPowerMeterJFX() {
        setWidth(90);
        setHeight(100);
        widthProperty().addListener(e -> render());
        heightProperty().addListener(e -> render());
        render();
    }

    public void reset() {
        mPeak = mMinimumValue;
        mPower = mMinimumValue;
        mSquelchThreshold = mMinimumValue;
        render();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        render();
    }

    private void render() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        g.clearRect(0, 0, w, h);

        double topInset = 5;
        double bottomInset = 5;
        double leftInset = 5;

        // Draw meter border
        g.setStroke(COLOR_FOREGROUND);
        g.strokeRect(leftInset, topInset, BAR_WIDTH, h - topInset - bottomInset);

        // Draw filled bar
        g.setFill(COLOR_BAR);
        double fillHeight = h - topInset - bottomInset - DOUBLE_PADDING;
        double height = fillHeight * getPowerPercent() - PADDING;
        double top = (fillHeight - height) + topInset + PADDING;
        g.fillRect(leftInset + PADDING, top, BAR_WIDTH - DOUBLE_PADDING, height);

        // Draw threshold
        if (isSquelchThresholdVisible()) {
            g.setStroke(COLOR_THRESHOLD);
            drawPercentLine(g, getSquelchThresholdPercent(), topInset, bottomInset, leftInset, h);
        }

        // Draw peak line
        if (isPeakVisible()) {
            g.setStroke(COLOR_PEAK);
            drawPercentLine(g, getPeakPercent(), topInset, bottomInset, leftInset, h);
        }

        // Draw legend/scale
        g.setFill(COLOR_FOREGROUND);
        g.setFont(Font.getDefault());
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        for (double legendValue : getScaleValues(h)) {
            drawScaleText(g, legendValue, topInset, bottomInset, leftInset, h);
        }
    }

    private void drawPercentLine(GraphicsContext g, double percentValue, double topInset, double bottomInset, double leftInset, double h) {
        double totalHeight = h - topInset - bottomInset - DOUBLE_PADDING;
        double height = totalHeight * percentValue - PADDING;
        double top = (totalHeight - height) + topInset + PADDING;
        g.strokeLine(leftInset, top, leftInset + BAR_WIDTH, top);
    }

    private void drawScaleText(GraphicsContext g, double value, double topInset, double bottomInset, double leftInset, double h) {
        double percent = getPercent(value);
        double totalHeight = h - topInset - bottomInset - DOUBLE_PADDING;
        double height = totalHeight * percent - PADDING;
        double top = (totalHeight - height) + topInset + PADDING;
        double left = leftInset + BAR_WIDTH - PADDING;
        String label = DECIMAL_FORMAT.format(value);
        g.fillText(label, left + DOUBLE_PADDING, top);
    }

    private List<Double> getScaleValues(double h) {
        // Approximate ascent height for standard font
        double ascent = 12 * 2;
        double labelCount = h / ascent;
        double interval = getExtent() / labelCount;
        List<Double> values = new ArrayList<>();
        for (double x = 0; x > getMinimumValue(); x -= interval) {
            values.add(x);
        }
        return values;
    }

    public double getPower() { return mPower; }
    public void setPower(double power) { mPower = power; render(); }
    public double getSquelchThreshold() { return mSquelchThreshold; }
    public void setSquelchThreshold(double squelchThreshold) { mSquelchThreshold = squelchThreshold; render(); }
    public double getMinimumValue() { return mMinimumValue; }
    public void setMinimumValue(double minimumValue) { mMinimumValue = minimumValue; render(); }
    public double getMaximumValue() { return mMaximumValue; }
    public void setMaximumValue(double maximumValue) { mMaximumValue = maximumValue; render(); }
    public double getExtent() { return mMaximumValue - mMinimumValue; }
    public double getPeak() { return mPeak; }
    public void setPeak(double peak) { mPeak = peak; render(); }
    public boolean isPeakVisible() { return mPeakVisible & isValidValue(getPeak()); }
    public void setPeakVisible(boolean peakVisible) { mPeakVisible = peakVisible; render(); }
    public boolean isSquelchThresholdVisible() { return mSquelchThresholdVisible & isValidValue(getSquelchThreshold()); }
    public void setSquelchThresholdVisible(boolean squelchThresholdVisible) { mSquelchThresholdVisible = squelchThresholdVisible; render(); }
    private double getPercent(double value) { return (value - getMinimumValue()) / getExtent(); }
    private double getPowerPercent() { return getPercent(mPower); }
    private double getSquelchThresholdPercent() { return getPercent(mSquelchThreshold); }
    private double getPeakPercent() { return getPercent(mPeak); }
    private boolean isValidValue(double value) { return getMinimumValue() <= value && value <= getMaximumValue(); }
}
