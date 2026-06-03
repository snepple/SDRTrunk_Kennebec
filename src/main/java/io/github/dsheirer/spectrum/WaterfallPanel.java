package io.github.dsheirer.spectrum;

import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Point2D;
import java.text.DecimalFormat;
import java.util.Arrays;

public class WaterfallPanel extends StackPane implements DFTResultsListener, Pausable, SettingChangeListener {
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(WaterfallPanel.class);
    private static DecimalFormat CURSOR_FORMAT = new DecimalFormat("0.00000");
    private static final String PAUSED = "PAUSED - Right Click to Unpause";
    private static final String DISABLED = "DISABLED - Right Click to Select a Tuner";

    private Canvas mCanvas;
    private GraphicsContext mGraphicsContext;
    private WritableImage mWaterfallImage;
    private PixelWriter mPixelWriter;
    private int[] mColorMap;
    private int mHeadY = 0;
    private java.util.concurrent.ConcurrentLinkedQueue<int[]> mRowQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private int mDFTSize = 4096;
    private int mImageHeight = 700;

    private Color mColorSpectrumCursor;
    private Point2D mCursorLocation = new Point2D(0, 0);
    private boolean mCursorVisible = false;
    private long mCursorFrequency = 0;
    private boolean mPaused = false;
    private boolean mDisabled = true;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    private SettingsManager mSettingsManager;
    private AnimationTimer mAnimationTimer;
    private volatile boolean mNeedsRedraw = false;


    public WaterfallPanel(SettingsManager settingsManager) {
        super();
        mSettingsManager = settingsManager;
        mSettingsManager.getSettingsModel().addListener(this);
        mColorSpectrumCursor = getColor(ColorSettingName.SPECTRUM_CURSOR);
        mColorMap = WaterfallColorModel.getARGBColorMap();

        mCanvas = new Canvas(mDFTSize, mImageHeight);
        mGraphicsContext = mCanvas.getGraphicsContext2D();

        this.getChildren().add(mCanvas);

        // Bind canvas size to parent size so it resizes
        mCanvas.widthProperty().bind(this.widthProperty());
        mCanvas.heightProperty().bind(this.heightProperty());

        mCanvas.widthProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);
        mCanvas.heightProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);

        reset();

        mAnimationTimer = new AnimationTimer() {
            private long mLastDrawNanos = 0;
            private static final long MIN_FRAME_NANOS = 70_000_000L; // ~14 FPS cap

            @Override
            public void handle(long now) {
                if (mNeedsRedraw && (now - mLastDrawNanos) >= MIN_FRAME_NANOS) {
                    mNeedsRedraw = false;
                    mLastDrawNanos = now;
                    draw();
                }
            }
        };
        mAnimationTimer.start();
    }

    public void dispose() {
        if (mSettingsManager != null) {
            mSettingsManager.getSettingsModel().removeListener(this);
        }
        mSettingsManager = null;
        if (mAnimationTimer != null) {
            mAnimationTimer.stop();
        }
    }

    private void reset() {
        if (mDFTSize <= 0) return;
        mImageHeight = Math.max((int) Math.ceil(mCanvas.getHeight()), 1);

        mRowQueue.clear();
        mWaterfallImage = new WritableImage(mDFTSize, mImageHeight);
        mPixelWriter = mWaterfallImage.getPixelWriter();
        mHeadY = 0;
        mNeedsRedraw = true;
    }

    public void setPaused(boolean paused) {
        mPaused = paused;
        mNeedsRedraw = true;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public boolean isWaterfallDisabled() {
        return mDisabled;
    }

    public void setZoom(int zoom) {
        mZoom = zoom;
        mNeedsRedraw = true;
    }

    private int getZoomMultiplier() {
        return (int) FastMath.pow(2.0, mZoom);
    }

    public void setZoomWindowOffset(int offset) {
        mDFTZoomWindowOffset = offset;
        mNeedsRedraw = true;
    }

    private Color getColor(ColorSettingName name) {
        ColorSetting setting = mSettingsManager.getSettingsModel().getColorSetting(name);
        javafx.scene.paint.Color awtColor = setting.getColor();
        return Color.rgb((int)(awtColor.getRed() * 255), (int)(awtColor.getGreen() * 255), (int)(awtColor.getBlue() * 255), awtColor.getOpacity());
    }

    @Override
    public void settingChanged(Setting setting) {
        if (setting instanceof ColorSetting colorSetting) {
            if (colorSetting.getColorSettingName() == ColorSettingName.SPECTRUM_CURSOR) {
                javafx.scene.paint.Color awtColor = colorSetting.getColor();
                mColorSpectrumCursor = Color.rgb((int)(awtColor.getRed() * 255), (int)(awtColor.getGreen() * 255), (int)(awtColor.getBlue() * 255), awtColor.getOpacity());
                mNeedsRedraw = true;
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting) {}

    public void setCursorLocation(Point2D point) {
        mCursorLocation = point;
        mNeedsRedraw = true;
    }

    public void setCursorFrequency(long frequency) {
        mCursorFrequency = frequency;
    }

    public void setCursorVisible(boolean visible) {
        mCursorVisible = visible;
        mNeedsRedraw = true;
    }

    private double getPixelOffset(int multiplier) {
        double offset = 0;
        if (mZoom != 0) {
            double binPixelWidth = getBinPixelWidth(multiplier);
            offset = -binPixelWidth * (double) (mDFTZoomWindowOffset);
        }
        return offset;
    }

    private double getBinPixelWidth(int multiplier) {
        return ((double) mCanvas.getWidth() * (double) multiplier) / (double) mDFTSize;
    }

    private void draw() {
        if (mGraphicsContext == null || mWaterfallImage == null) return;

        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();

        if (Math.max((int) Math.ceil(height), 1) != mImageHeight && height > 0) {
            reset();
            return;
        }

        mGraphicsContext.clearRect(0, 0, width, height);

        if (!mPaused && mPixelWriter != null) {
            int[] row;
            while ((row = mRowQueue.poll()) != null) {
                if (row.length == mDFTSize) {
                    mHeadY--;
                    if (mHeadY < 0) {
                        mHeadY = mImageHeight - 1;
                    }
                    mPixelWriter.setPixels(0, mHeadY, mDFTSize, 1, PixelFormat.getIntArgbInstance(), row, 0, mDFTSize);
                }
            }
        }

        int multiplier = getZoomMultiplier();
        double binPixelWidth = getBinPixelWidth(multiplier);
        int offset = (int) (getPixelOffset(multiplier) - binPixelWidth);

        // Draw the image in two parts to handle the wrap-around
        double scaledWidth = (width * multiplier) + binPixelWidth;
        double bottomHeight = mImageHeight - mHeadY;

        if (bottomHeight > 0) {
            mGraphicsContext.drawImage(mWaterfallImage,
                0, mHeadY, mDFTSize, bottomHeight,
                offset, 0, scaledWidth, bottomHeight);
        }
        if (mHeadY > 0) {
            mGraphicsContext.drawImage(mWaterfallImage,
                0, 0, mDFTSize, mHeadY,
                offset, bottomHeight, scaledWidth, mHeadY);
        }

        mGraphicsContext.setStroke(mColorSpectrumCursor);
        mGraphicsContext.setFill(mColorSpectrumCursor);

        if (mCursorVisible) {
            mGraphicsContext.strokeLine(mCursorLocation.getX(), 0, mCursorLocation.getX(), height);
            String frequency = CURSOR_FORMAT.format(mCursorFrequency / 1000000.0D);
            mGraphicsContext.fillText(frequency, mCursorLocation.getX() + 5, mCursorLocation.getY());
        }

        if (mDisabled) {
            javafx.scene.text.Text measureText = new javafx.scene.text.Text(DISABLED);
            double textWidth = measureText.getBoundsInLocal().getWidth();
            double textHeight = measureText.getBoundsInLocal().getHeight();
            mGraphicsContext.setFill(Color.color(0, 0, 0, 0.6));
            mGraphicsContext.fillRoundRect(15, 8, textWidth + 10, textHeight + 6, 6, 6);
            mGraphicsContext.setFill(Color.WHITE);
            mGraphicsContext.fillText(DISABLED, 20, 20 + textHeight * 0.3);
        } else if (mPaused) {
            javafx.scene.text.Text measureText = new javafx.scene.text.Text(PAUSED);
            double textWidth = measureText.getBoundsInLocal().getWidth();
            double textHeight = measureText.getBoundsInLocal().getHeight();
            mGraphicsContext.setFill(Color.color(0, 0, 0, 0.6));
            mGraphicsContext.fillRoundRect(15, 8, textWidth + 10, textHeight + 6, 6, 6);
            mGraphicsContext.setFill(Color.WHITE);
            mGraphicsContext.fillText(PAUSED, 20, 20 + textHeight * 0.3);
        }

        paintZoomIndicator(mGraphicsContext, width, height);
    }

    private void paintZoomIndicator(GraphicsContext graphics, double width, double height) {
        if (mZoom != 0) {
            double indWidth = width / 4;
            double x = (width / 2) - (indWidth / 2);

            graphics.strokeRect(x, height - 12, indWidth, 10);
            double zoomWidth = indWidth / getZoomMultiplier();
            double windowOffset = 0;

            if (mDFTZoomWindowOffset != 0) {
                windowOffset = ((double) mDFTZoomWindowOffset / (double) mDFTSize) * indWidth;
            }

            graphics.fillRect(x + windowOffset, height - 12, zoomWidth, 10);
            graphics.fillText("Zoom: " + getZoomMultiplier() + "x", x + indWidth + 3, height - 2);
        }
    }

    @Override
    public void receive(float[] update) {
        mDisabled = false;

        if (mDFTSize != update.length) {
            mDFTSize = update.length;
            Platform.runLater(this::reset);
            return;
        }

        if (mPaused) return;

        int[] newPixels = new int[update.length];

        double sum = 0.0d;
        for (int x = 0; x < update.length; x++) {
            sum += update[x];
        }

        float average = (float) (sum / (double) update.length);
        float scale = 256.0f / average;

        for (int x = 0; x < update.length; x++) {
            float value = (average - update[x]) * scale;
            int colorIndex;

            if (value < 0) {
                colorIndex = 0;
            } else if (value > 255) {
                colorIndex = 255;
            } else {
                colorIndex = (int) value;
            }
            newPixels[x] = mColorMap[colorIndex];
        }

        mRowQueue.offer(newPixels);
        mNeedsRedraw = true;
    }

    public void clearWaterfall() {
        mDisabled = true;
        mRowQueue.clear();
        if (mGraphicsContext != null) {
            Platform.runLater(() -> mGraphicsContext.clearRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight()));
        }
        mNeedsRedraw = true;
    }
}
