package io.github.dsheirer.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingUtilities;

public class ThreadingBridge {
    private static final Logger mLog = LoggerFactory.getLogger(ThreadingBridge.class);

    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            mLog.trace("Dispatching to FX thread");
            Platform.runLater(action);
        }
    }

    public static void runOnSwingThread(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            mLog.trace("Dispatching to Swing EDT");
            SwingUtilities.invokeLater(action);
        }
    }
}
