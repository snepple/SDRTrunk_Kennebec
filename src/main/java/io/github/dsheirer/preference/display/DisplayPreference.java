package io.github.dsheirer.preference.display;

import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DisplayPreference {
    private static final String PREF_WATERFALL_HEIGHT = "waterfall.image.height";
    private static final String PREF_SPECTRUM_INSET = "spectrum.inset";
    private static final String PREF_LABEL_WIDTH = "overlay.label.width";
    private static final String PREF_LABEL_HEIGHT = "overlay.label.height";
    private static final String PREF_FFT_WINDOW_TYPE = "fft.window.type"; // Storing name or index? Store string name of WindowType

    private Preferences mPreferences = Preferences.userNodeForPackage(DisplayPreference.class);
    private Listener<PreferenceType> mPreferenceUpdateListener;

    private IntegerProperty mWaterfallImageHeight = new SimpleIntegerProperty(700);
    private DoubleProperty mSpectrumInset = new SimpleDoubleProperty(20.0);
    private DoubleProperty mOverlayLabelWidth = new SimpleDoubleProperty(50.0);
    private DoubleProperty mOverlayLabelHeight = new SimpleDoubleProperty(12.0);

    // Storing the String name of the WindowType enum
    private String mFFTWindowType = "BLACKMAN_HARRIS_7";

    public DisplayPreference(Listener<PreferenceType> listener) {
        mPreferenceUpdateListener = listener;
        load();
    }

    private void load() {
        mWaterfallImageHeight.set(mPreferences.getInt(PREF_WATERFALL_HEIGHT, 700));
        mSpectrumInset.set(mPreferences.getDouble(PREF_SPECTRUM_INSET, 20.0));
        mOverlayLabelWidth.set(mPreferences.getDouble(PREF_LABEL_WIDTH, 50.0));
        mOverlayLabelHeight.set(mPreferences.getDouble(PREF_LABEL_HEIGHT, 12.0));
        mFFTWindowType = mPreferences.get(PREF_FFT_WINDOW_TYPE, "BLACKMAN_HARRIS_7");

        mWaterfallImageHeight.addListener((obs, oldV, newV) -> save());
        mSpectrumInset.addListener((obs, oldV, newV) -> save());
        mOverlayLabelWidth.addListener((obs, oldV, newV) -> save());
        mOverlayLabelHeight.addListener((obs, oldV, newV) -> save());
    }

    private void save() {
        mPreferences.putInt(PREF_WATERFALL_HEIGHT, mWaterfallImageHeight.get());
        mPreferences.putDouble(PREF_SPECTRUM_INSET, mSpectrumInset.get());
        mPreferences.putDouble(PREF_LABEL_WIDTH, mOverlayLabelWidth.get());
        mPreferences.putDouble(PREF_LABEL_HEIGHT, mOverlayLabelHeight.get());
        mPreferences.put(PREF_FFT_WINDOW_TYPE, mFFTWindowType);
        
        if (mPreferenceUpdateListener != null) {
            mPreferenceUpdateListener.receive(PreferenceType.DISPLAY);
        }
    }

    public IntegerProperty waterfallImageHeightProperty() {
        return mWaterfallImageHeight;
    }

    public DoubleProperty spectrumInsetProperty() {
        return mSpectrumInset;
    }

    public DoubleProperty overlayLabelWidthProperty() {
        return mOverlayLabelWidth;
    }

    public DoubleProperty overlayLabelHeightProperty() {
        return mOverlayLabelHeight;
    }

    public String getFFTWindowType() {
        return mFFTWindowType;
    }

    public void setFFTWindowType(String windowType) {
        if (!mFFTWindowType.equals(windowType)) {
            mFFTWindowType = windowType;
            save();
        }
    }
}
