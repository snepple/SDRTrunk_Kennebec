package io.github.dsheirer.spectrum;

import java.util.prefs.Preferences;
import io.github.dsheirer.preference.display.DisplayPreference;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import com.google.common.eventbus.Subscribe;

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

    //Bounds for the constant status strings, measured once and cached.  Constructing a Text node every
    //frame just to measure it triggered CSS/layout machinery and churned while idle (mDisabled starts true).
    private static double sDisabledTextWidth = -1;
    private static double sDisabledTextHeight;
    private static double sPausedTextWidth = -1;
    private static double sPausedTextHeight;

    private Canvas mCanvas;
    private GraphicsContext mGraphicsContext;
    private WritableImage mWaterfallImage;
    private PixelWriter mPixelWriter;
    private int[] mColorMap;
    private int mHeadY = 0;
    private java.util.concurrent.ConcurrentLinkedQueue<int[]> mRowQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private volatile int mDFTSize = 4096;
    private Preferences mPreferences = Preferences.userNodeForPackage(DisplayPreference.class);
    private int mImageHeight = mPreferences.getInt("waterfall.image.height", 700);

    private Color mColorSpectrumCursor;
    private Point2D mCursorLocation = new Point2D(0, 0);
    private boolean mCursorVisible = false;
    private long mCursorFrequency = 0;
    private volatile boolean mPaused = false;
    private volatile boolean mDisabled = true;
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
        MyEventBus.getGlobalEventBus().register(this);

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

        this.visibleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && mAnimationTimer != null) {
                mAnimationTimer.start();
            } else if (mAnimationTimer != null) {
                mAnimationTimer.stop();
            }
        });

        this.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && isVisible() && mAnimationTimer != null) {
                mAnimationTimer.start();
            } else if (mAnimationTimer != null) {
                mAnimationTimer.stop();
            }
        });

        mAnimationTimer.start();
    }

    public void dispose() {
        MyEventBus.getGlobalEventBus().unregister(this);
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

    

    public void setZoomWindowOffset(int offset) {
        mDFTZoomWindowOffset = offset;
        mNeedsRedraw = true;
    }

    private Color getColor(ColorSettingName name) {
        ColorSetting setting = mSettingsManager.getSettingsModel().getColorSetting(name);
        javafx.scene.paint.Color awtColor = setting.getColor();
        return awtColor;
    }

    @Override
    public void settingChanged(Setting setting) {
        if (setting instanceof ColorSetting colorSetting) {
            if (colorSetting.getColorSettingName() == ColorSettingName.SPECTRUM_CURSOR) {
                javafx.scene.paint.Color awtColor = colorSetting.getColor();
                mColorSpectrumCursor = awtColor;
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

    @Subscribe
    public void onPreferenceUpdate(PreferenceType preferenceType) {
        if (preferenceType == PreferenceType.DISPLAY) {
            int prefHeight = mPreferences.getInt("waterfall.image.height", 700);
            if (prefHeight != mImageHeight) {
                mImageHeight = prefHeight;
                javafx.application.Platform.runLater(() -> {
                    reset();
                });
            }
        }
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

    /**
     * Solid background color used when the waterfall is disabled.  Derived from the lowest ("no signal")
     * color of the active waterfall palette so it matches the live display, with a navy-blue fallback.
     */
    private Color getDisabledBackgroundColor() {
        if (mColorMap != null && mColorMap.length > 0) {
            int argb = mColorMap[0];
            return Color.rgb((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        }
        return Color.rgb(0, 0, 128);
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

        int multiplier = SpectrumUtils.getZoomMultiplier(mZoom);
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
            //Only show the frequency when it is a sane RF value; guards against an out-of-range/garbage value
            //(e.g. from a zero-width overlay canvas) rendering as a huge negative number.
            if (mCursorFrequency > 0 && mCursorFrequency < 30_000_000_000L) {
                String frequency = CURSOR_FORMAT.format(mCursorFrequency / 1000000.0D);
                mGraphicsContext.fillText(frequency, mCursorLocation.getX() + 5, mCursorLocation.getY());
            }
        }

        if (mDisabled) {
            if (sDisabledTextWidth < 0) {
                javafx.scene.text.Text measureText = new javafx.scene.text.Text(DISABLED);
                sDisabledTextWidth = measureText.getBoundsInLocal().getWidth();
                sDisabledTextHeight = measureText.getBoundsInLocal().getHeight();
            }
            //Fill the whole waterfall with a solid background (the palette's lowest/"no signal" color, so it
            //matches the active waterfall's blue and adapts to a user-selected color map) and draw the
            //disabled message in a contrasting color - matches the classic disabled appearance.
            mGraphicsContext.setFill(getDisabledBackgroundColor());
            mGraphicsContext.fillRect(0, 0, width, height);
            mGraphicsContext.setFill(Color.YELLOW);
            mGraphicsContext.fillText(DISABLED, 5, sDisabledTextHeight);
        } else if (mPaused) {
            if (sPausedTextWidth < 0) {
                javafx.scene.text.Text measureText = new javafx.scene.text.Text(PAUSED);
                sPausedTextWidth = measureText.getBoundsInLocal().getWidth();
                sPausedTextHeight = measureText.getBoundsInLocal().getHeight();
            }
            mGraphicsContext.setFill(Color.color(0, 0, 0, 0.6));
            mGraphicsContext.fillRoundRect(15, 8, sPausedTextWidth + 10, sPausedTextHeight + 6, 6, 6);
            mGraphicsContext.setFill(Color.WHITE);
            mGraphicsContext.fillText(PAUSED, 20, 20 + sPausedTextHeight * 0.3);
        }

        paintZoomIndicator(mGraphicsContext, width, height);
    }

    private void paintZoomIndicator(GraphicsContext graphics, double width, double height) {
        if (mZoom != 0) {
            double indWidth = width / 4;
            double x = (width / 2) - (indWidth / 2);

            graphics.strokeRect(x, height - 12, indWidth, 10);
            double zoomWidth = indWidth / SpectrumUtils.getZoomMultiplier(mZoom);
            double windowOffset = 0;

            if (mDFTZoomWindowOffset != 0) {
                windowOffset = ((double) mDFTZoomWindowOffset / (double) mDFTSize) * indWidth;
            }

            graphics.fillRect(x + windowOffset, height - 12, zoomWidth, 10);
            graphics.fillText("Zoom: " + SpectrumUtils.getZoomMultiplier(mZoom) + "x", x + indWidth + 3, height - 2);
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

        //Bound the render queue. The waterfall image only holds mImageHeight rows, so buffering more is
        //pointless; and if the JavaFX thread falls behind (it consumes this queue in draw()), an
        //unbounded queue grows the heap without limit - each retained int[] is mDFTSize ints, and the
        //DFT only feeds this when a tuner is streaming. Left unbounded that climbs into the gigabytes,
        //and the resulting GC pressure starves the FX thread further into a permanent "Not Responding"
        //spiral. Dropping the oldest rows keeps memory bounded; under load the display just skips frames.
        int cap = mImageHeight > 0 ? mImageHeight : 700;
        while (mRowQueue.size() > cap) {
            mRowQueue.poll();
        }

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
