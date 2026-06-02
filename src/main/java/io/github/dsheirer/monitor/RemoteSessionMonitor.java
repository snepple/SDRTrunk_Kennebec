package io.github.dsheirer.monitor;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.eventbus.RemoteDesktopModeEvent;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class RemoteSessionMonitor {
    private static final Logger mLog = LoggerFactory.getLogger(RemoteSessionMonitor.class);
    private static RemoteSessionMonitor mInstance;
    
    private Tailer mTailer;
    private Thread mTailerThread;
    private boolean mCurrentlyActive = false;
    private Timer mDebounceTimer = new Timer(true);
    private TimerTask mDebounceTask;

    public static synchronized void init() {
        if (mInstance == null) {
            mInstance = new RemoteSessionMonitor();
            mInstance.start();
        }
    }

    private void start() {
        String appData = System.getenv("APPDATA");
        if (appData == null) return;
        
        File rustDeskLog = new File(appData, "RustDesk\\log\\RustDesk_rCURRENT.log");
        if (!rustDeskLog.exists()) {
            mLog.info("RustDesk log not found at {}, not monitoring.", rustDeskLog.getAbsolutePath());
            return;
        }

        mTailer = new Tailer(rustDeskLog, new LogTailerListener(), 1000, true);
        mTailerThread = new Thread(mTailer, "RustDeskLogTailer");
        mTailerThread.setDaemon(true);
        mTailerThread.start();
        mLog.info("Started monitoring RustDesk logs at {}", rustDeskLog.getAbsolutePath());
    }

    private void handleStatusChange(boolean connected) {
        if (mDebounceTask != null) {
            mDebounceTask.cancel();
        }

        mDebounceTask = new TimerTask() {
            @Override
            public void run() {
                if (mCurrentlyActive != connected) {
                    mCurrentlyActive = connected;
                    mLog.info("Remote desktop mode active: {}", mCurrentlyActive);
                    MyEventBus.getGlobalEventBus().post(new RemoteDesktopModeEvent(mCurrentlyActive));
                }
            }
        };

        // 5 seconds debounce
        mDebounceTimer.schedule(mDebounceTask, 5000);
    }

    private class LogTailerListener extends TailerListenerAdapter {
        @Override
        public void handle(String line) {
            if (line == null) return;
            // Example lines: "connected to" or "disconnected from"
            // RustDesk uses specific log patterns, we look for typical connection/disconnection keywords
            if (line.contains("connection established") || line.contains("peer connected") || line.contains("Remote session started")) {
                handleStatusChange(true);
            } else if (line.contains("connection closed") || line.contains("peer disconnected") || line.contains("Remote session ended")) {
                handleStatusChange(false);
            }
        }

        @Override
        public void fileRotated() {
            mLog.info("RustDesk log rotated.");
        }
    }
}
