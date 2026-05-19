package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.filter.smoothing.GaussianSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.NoSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.RectangularSmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter;
import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;
import io.github.dsheirer.dsp.filter.smoothing.TriangularSmoothingFilter;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.ColorSetting.ColorSettingName;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class SpectrumPanel extends StackPane implements DFTResultsListener, SettingChangeListener, SpectralDisplayAdjuster {
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(SpectrumPanel.class);

    private Canvas mCanvas;
    private GraphicsContext mGraphicsContext;

    private float[] mDisplayFFTBins;

    private int mZoom = 0;
    private int mZoomWindowOffset = 0;
    private int mSpectrumInset = 20;

    private float mDBScale = -120.0f;
    private int mAveraging = 1;

    private Color mColorSpectrumBackground;
    private Color mColorSpectrumGradientBottom;
    private Color mColorSpectrumGradientTop;
    private Color mColorSpectrumLine;

    private SettingsManager mSettingsManager;

    private SmoothingFilter mSmoothingFilter = new NoSmoothingFilter();

    private AnimationTimer mAnimationTimer;
    private volatile boolean mNeedsRedraw = false;
    private final Object mBinsLock = new Object();

    public SpectrumPanel(SettingsManager settingsManager) {
        mSettingsManager = settingsManager;
        mSettingsManager.getSettingsModel().addListener(this);

        getColors();

        mCanvas = new Canvas();
        mGraphicsContext = mCanvas.getGraphicsContext2D();

        this.getChildren().add(mCanvas);

        mCanvas.widthProperty().bind(this.widthProperty());
        mCanvas.heightProperty().bind(this.heightProperty());

        mCanvas.widthProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);
        mCanvas.heightProperty().addListener((observable, oldValue, newValue) -> mNeedsRedraw = true);

        mAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (mNeedsRedraw) {
                    mNeedsRedraw = false;
                    drawSpectrum();
                }
            }
        };
        mAnimationTimer.start();
    }

    public void dispose() {
        if (mSettingsManager != null) {
            mSettingsManager.getSettingsModel().removeListener(this);
        }
        if (mAnimationTimer != null) {
            mAnimationTimer.stop();
        }
    }

    public void receive(float[] currentFFTBins) {
        if (Float.isInfinite(currentFFTBins[0]) || Float.isNaN(currentFFTBins[0])) {
            currentFFTBins = new float[currentFFTBins.length];
        }

        synchronized (mBinsLock) {
            if (mDisplayFFTBins == null || mDisplayFFTBins.length != currentFFTBins.length) {
                mDisplayFFTBins = new float[currentFFTBins.length];
            }

            float[] smoothedBins = mSmoothingFilter.filter(currentFFTBins);

            if (mAveraging > 1) {
                float gain = 1.0f / (float) mAveraging;
                for (int x = 0; x < mDisplayFFTBins.length; x++) {
                    mDisplayFFTBins[x] += (smoothedBins[x] - mDisplayFFTBins[x]) * gain;
                }
            } else {
                System.arraycopy(smoothedBins, 0, mDisplayFFTBins, 0, smoothedBins.length);
            }
            mNeedsRedraw = true;
        }
    }

    private void drawSpectrum() {
        if (mGraphicsContext == null) return;

        double width = mCanvas.getWidth();
        double height = mCanvas.getHeight();

        if (width <= 0 || height <= 0) return;

        mGraphicsContext.setFill(mColorSpectrumBackground);
        mGraphicsContext.fillRect(0, 0, width, height);

        double insideHeight = height - mSpectrumInset;

        LinearGradient gradient = new LinearGradient(
            0, insideHeight / 2.0,
            0, height,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0, mColorSpectrumGradientTop),
            new Stop(1, mColorSpectrumGradientBottom)
        );

        mGraphicsContext.beginPath();
        mGraphicsContext.moveTo(width, insideHeight);
        mGraphicsContext.lineTo(0, insideHeight);

        float[] bins = getBins();

        if (bins != null && bins.length > 0) {
            float scalor = (float) insideHeight / -mDBScale;
            float binSize = (float) width / ((float) (bins.length));

            for (int x = 0; x < bins.length; x++) {
                float h = -bins[x] * scalor;
                if (h > insideHeight) h = (float) insideHeight;
                if (h < 0) h = 0;

                float xAxis = (float) x * binSize;
                mGraphicsContext.lineTo(xAxis, h);
            }
        } else {
            mGraphicsContext.lineTo(0, insideHeight);
            mGraphicsContext.lineTo(width, insideHeight);
        }

        mGraphicsContext.lineTo(width, insideHeight);
        mGraphicsContext.setFill(gradient);
        mGraphicsContext.fill();

        mGraphicsContext.setStroke(mColorSpectrumLine);
        mGraphicsContext.strokeLine(0, insideHeight, width, insideHeight);
    }

    public void setZoom(int zoom) {
        Validate.isTrue(0 <= zoom && zoom <= 6, "Unrecognized Zoom Level: " + zoom);
        mZoom = zoom;
        mNeedsRedraw = true;
    }

    public void setZoomWindowOffset(int offset) {
        mZoomWindowOffset = offset;
        mNeedsRedraw = true;
    }

    public void setSampleSize(double sampleSize) {
        Validate.isTrue(2.0 <= sampleSize && sampleSize <= 64.0);
        mDBScale = (float) (20.0 * FastMath.log10(1.0 / FastMath.pow(2.0, sampleSize - 1)));
        mNeedsRedraw = true;
    }

    public void setAveraging(int size) {
        mAveraging = size;
        mNeedsRedraw = true;
    }

    public int getAveraging() {
        return mAveraging;
    }

    public void clearSpectrum() {
        synchronized (mBinsLock) {
            if (mDisplayFFTBins != null) {
                Arrays.fill(mDisplayFFTBins, 0.0f);
            }
            mNeedsRedraw = true;
        }
    }

    private void getColors() {
        mColorSpectrumBackground = getColorFromSetting(ColorSettingName.SPECTRUM_BACKGROUND);
        mColorSpectrumGradientBottom = getColorFromSetting(ColorSettingName.SPECTRUM_GRADIENT_BOTTOM);
        mColorSpectrumGradientTop = getColorFromSetting(ColorSettingName.SPECTRUM_GRADIENT_TOP);
        mColorSpectrumLine = getColorFromSetting(ColorSettingName.SPECTRUM_LINE);
    }

    private Color getColorFromSetting(ColorSettingName name) {
        ColorSetting setting = mSettingsManager.getSettingsModel().getColorSetting(name);
        java.awt.Color awtColor = setting.getColor();
        return Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), awtColor.getAlpha() / 255.0);
    }

    @Override
    public void settingChanged(Setting setting) {
        if (setting instanceof ColorSetting colorSetting) {
            java.awt.Color awtColor = colorSetting.getColor();
            Color fxColor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue(), awtColor.getAlpha() / 255.0);

            switch (colorSetting.getColorSettingName()) {
                case SPECTRUM_BACKGROUND:
                    mColorSpectrumBackground = fxColor;
                    break;
                case SPECTRUM_GRADIENT_BOTTOM:
                    mColorSpectrumGradientBottom = fxColor;
                    break;
                case SPECTRUM_GRADIENT_TOP:
                    mColorSpectrumGradientTop = fxColor;
                    break;
                case SPECTRUM_LINE:
                    mColorSpectrumLine = fxColor;
                    break;
                default:
                    break;
            }
            mNeedsRedraw = true;
        }
    }

    private int getZoomMultiplier() {
        return (int) FastMath.pow(2.0, mZoom);
    }

    private float[] getBins() {
        synchronized (mBinsLock) {
            if (mZoom == 0 || mDisplayFFTBins == null) {
                return mDisplayFFTBins != null ? Arrays.copyOf(mDisplayFFTBins, mDisplayFFTBins.length) : null;
            } else {
                int length = mDisplayFFTBins.length / getZoomMultiplier();
                int offset = mZoomWindowOffset;

                if ((offset + length) >= mDisplayFFTBins.length) {
                    offset = mDisplayFFTBins.length - length;
                }

                if (offset < 0) {
                    offset = 0;
                }

                return Arrays.copyOfRange(mDisplayFFTBins, offset, offset + length);
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting) {}

    @Override
    public int getSmoothing() {
        return mSmoothingFilter.getPointSize();
    }

    @Override
    public void setSmoothing(int smoothing) {
        mSmoothingFilter.setPointSize(smoothing);
    }

    @Override
    public SmoothingType getSmoothingType() {
        return mSmoothingFilter.getSmoothingType();
    }

    @Override
    public void setSmoothingType(SmoothingType type) {
        if (mSmoothingFilter.getSmoothingType() != type) {
            int pointSize = getSmoothing();

            synchronized (mSmoothingFilter) {
                switch (type) {
                    case GAUSSIAN:
                        mSmoothingFilter = new GaussianSmoothingFilter();
                        break;
                    case RECTANGLE:
                        mSmoothingFilter = new RectangularSmoothingFilter();
                        break;
                    case TRIANGLE:
                        mSmoothingFilter = new TriangularSmoothingFilter();
                        break;
                    case NONE:
                    default:
                        mSmoothingFilter = new NoSmoothingFilter();
                        break;
                }
            }

            setSmoothing(pointSize);
        }
    }
}
