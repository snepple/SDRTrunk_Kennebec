package io.github.dsheirer.gui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global Hotkey Manager for SDRTrunk.
 * Handles keyboard-centric tactical hotkeys.
 */
public class HotkeyManager {
    private static final Logger mLog = LoggerFactory.getLogger(HotkeyManager.class);
    
    private Scene mScene;
    private HotkeyListener mListener;

    /**
     * Callback interface for hotkey actions.
     */
    public interface HotkeyListener {
        /** Toggle spectrum display visibility */
        void onToggleSpectrum();
        /** Toggle audio mute */
        default void onToggleMute() {}
        /** Toggle night/dark mode */
        default void onToggleNightMode() {}
    }

    public HotkeyManager(Scene scene, HotkeyListener listener) {
        mScene = scene;
        mListener = listener;
        registerHotkeys();
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    public HotkeyManager(Scene scene) {
        this(scene, null);
    }

    private void registerHotkeys() {
        if (mScene == null) return;
        
        mScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.P) {
                mLog.info("Hotkey triggered: Pause/Resume");
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.M) {
                mLog.info("Hotkey triggered: Mute/Unmute");
                if (mListener != null) {
                    mListener.onToggleMute();
                }
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.S) {
                mLog.info("Hotkey triggered: Toggle Spectrum Visibility");
                if (mListener != null) {
                    mListener.onToggleSpectrum();
                }
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.T) {
                mLog.info("Hotkey triggered: Toggle Night Mode");
                if (mListener != null) {
                    mListener.onToggleNightMode();
                }
                event.consume();
            }
        });
    }
}
