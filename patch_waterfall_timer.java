package io.github.dsheirer.spectrum;

import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
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

import java.awt.Point;
import java.text.DecimalFormat;
import java.util.Arrays;

public class WaterfallPanel extends JFXPanel implements DFTResultsListener, Pausable, SettingChangeListener {
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
    private int[] mPixels;
    private int[] mPausedPixels;

    private int mDFTSize = 4096;
    private int mImageHeight = 700;

    private Color mColorSpectrumCursor;
    private Point mCursorLocation = new Point(0, 0);
    private boolean mCursorVisible = false;
    private long mCursorFrequency = 0;
    private boolean mPaused = false;
    private boolean mDisabled = true;
    private int mZoom = 0;
    private int mDFTZoomWindowOffset = 0;

    private SettingsManager mSettingsManager;
    private AnimationTimer mAnimationTimer;
    private volatile boolean mNeedsRedraw = false;

    // Lock for synchronizing pixel array manipulation from DSP vs UI drawing
    private final Object mPixelsLock = new Object();

    public WaterfallPanel(SettingsManager settingsManager) {
        super();
        mSettingsManager = settingsManager;
        mSettingsManager.getSettingsModel().addListener(this);
        mColorSpectrumCursor = getColor(ColorSettingName.SPECTRUM_CURSOR);
        mColorMap = WaterfallColorModel.getARGBColorMap();

        Platform.runLater(() -> {
            mCanvas = new Canvas(mDFTSize, mImageHeight);
            mGraphicsContext = mCanvas.getGraphicsContext2D();

            StackPane root = new StackPane();
            root.getChildren().add(mCanvas);

            // Bind canvas size to parent size so it resizes
            mCanvas.widthProperty().bind(root.widthProperty());
            mCanvas.heightProperty().bind(root.heightProperty());

            mCanvas.widthProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);
            mCanvas.heightProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);

            Scene scene = new Scene(root);
            setScene(scene);

            reset();

            mAnimationTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (mNeedsRedraw) {
                        mNeedsRedraw = false;
                        draw();
                    }
                }
            };
            mAnimationTimer.start();
        });
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
        if (mDFTSize <= 0 || mImageHeight <= 0) return;

        synchronized (mPixelsLock) {
            mPixels = new int[mDFTSize * mImageHeight];
            mWaterfallImage = new WritableImage(mDFTSize, mImageHeight);
            mPixelWriter = mWaterfallImage.getPixelWriter();
            mNeedsRedraw = true;
        }
    }

    public void setPaused(boolean paused) {
        synchronized (mPixelsLock) {
            if (paused) {
                mPausedPixels = mPixels.clone();
            }
            mPaused = paused;
            mNeedsRedraw = true;
        }
    }

    public boolean isPaused() {
        return mPaused;
    }

    public boolean isDisabled() {
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
        java.awt.Color awtColor = setting.getColor();
        return Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), awtColor.getAlpha() / 255.0);
    }

    @Override
    public void settingChanged(Setting setting) {
        if (setting instanceof ColorSetting colorSetting) {
            if (colorSetting.getColorSettingName() == ColorSettingName.SPECTRUM_CURSOR) {
                java.awt.Color awtColor = colorSetting.getColor();
                mColorSpectrumCursor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), awtColor.getAlpha() / 255.0);
                mNeedsRedraw = true;
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting) {}

    public void setCursorLocation(Point point) {
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

        mGraphicsContext.clearRect(0, 0, width, height);

        int multiplier = getZoomMultiplier();
        double binPixelWidth = getBinPixelWidth(multiplier);
        int offset = (int) (getPixelOffset(multiplier) - binPixelWidth);

        synchronized (mPixelsLock) {
            if (mPixelWriter != null && mPixels != null) {
                int[] pixelsToDraw = mPaused ? mPausedPixels : mPixels;
                try {
                    mPixelWriter.setPixels(0, 0, mDFTSize, mImageHeight, PixelFormat.getIntArgbInstance(), pixelsToDraw, 0, mDFTSize);
                } catch (Exception e) {
                    mLog.error("Error drawing pixels - " + e.getLocalizedMessage());
                }
            }
        }

        // Draw the image
        mGraphicsContext.drawImage(mWaterfallImage, offset, 0, (width * multiplier) + binPixelWidth, height);

        mGraphicsContext.setStroke(mColorSpectrumCursor);
        mGraphicsContext.setFill(mColorSpectrumCursor);

        if (mCursorVisible) {
            mGraphicsContext.strokeLine(mCursorLocation.x, 0, mCursorLocation.x, height);
            String frequency = CURSOR_FORMAT.format(mCursorFrequency / 1000000.0D);
            mGraphicsContext.fillText(frequency, mCursorLocation.x + 5, mCursorLocation.y);
        }

        if (mDisabled) {
            mGraphicsContext.fillText(DISABLED, 20, 20);
        } else if (mPaused) {
            mGraphicsContext.fillText(PAUSED, 20, 20);
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

        int[] newPixels = new int[update.length];

        double sum = 0.0d;
        for (int x = 0; x < update.length - 1; x++) {
            sum += update[x];
        }

        float average = (float) (sum / (double) (update.length - 1));
        float scale = 256.0f / average;

        for (int x = 0; x < update.length - 1; x++) {
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

        synchronized (mPixelsLock) {
            if (mDFTSize != newPixels.length) {
                mDFTSize = newPixels.length;
                Platform.runLater(this::reset);
                return; // Wait for the reset on next cycle
            }

            if (mPixels != null) {
                System.arraycopy(mPixels, 0, mPixels, mDFTSize, mPixels.length - mDFTSize);
                System.arraycopy(newPixels, 0, mPixels, 0, newPixels.length);
                mNeedsRedraw = true;
            }
        }
    }

    public void clearWaterfall() {
        mDisabled = true;
        synchronized (mPixelsLock) {
            if (mPixels != null) {
                Arrays.fill(mPixels, 0);
            }
            mNeedsRedraw = true;
        }
    }
}
