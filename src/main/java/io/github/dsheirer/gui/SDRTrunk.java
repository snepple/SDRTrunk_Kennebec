

/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.gui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import javafx.scene.control.Button;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.DuplicateCallDetector;
import io.github.dsheirer.audio.broadcast.AudioStreamingManager;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
import io.github.dsheirer.audio.broadcast.StreamingWatchdog;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.controller.ControllerPanel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelAutoStartFrame;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelSelectionManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.preference.CalibrateRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.log.ApplicationLog;
import io.github.dsheirer.log.TwoToneLog;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.controller.channel.ChannelAlertMonitor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayLibraryHelper;
import io.github.dsheirer.source.tuner.ui.TunerSpectralDisplayManager;
import io.github.dsheirer.spectrum.DisableSpectrumWaterfallMenuItem;
import io.github.dsheirer.spectrum.ShowTunerMenuItem;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import java.awt.GraphicsEnvironment;

import javafx.geometry.Point2D;

import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Rectangle2D;




import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;


import javafx.application.Platform;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import jiconfont.icons.font_awesome.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;





import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;



import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.module.log.DiagnosticEngine;





public class SDRTrunk extends Application implements Listener<TunerEvent>, io.github.dsheirer.gui.VisibilityListener, io.github.dsheirer.gui.SidebarPanel.SidebarListener
{
    @Override
    public void onToggleSpectrum() {
        if (mCurrentViewId != null && mCurrentViewId.equals("now_playing")) {
            mNowPlayingSpectrumDisabled = !mNowPlayingSpectrumDisabled;
            mControllerPanel.getNowPlayingPanel().setSpectralPanelVisible(!mNowPlayingSpectrumDisabled);
            //Delegate the actual start/stop to the widget state so a minimized widget stays paused.
            mControllerPanel.getNowPlayingPanel().updateSpectrumProcessing();
        } else if (mCurrentViewId != null && mCurrentViewId.equals("tuners")) {
            mTunerSpectrumDisabled = !mTunerSpectrumDisabled;
            if (mTunerSpectrumDisabled) {
                mTopContentPanel.setCenter(null);
                mTopContentPanel.setMinHeight(0);
                mTopContentPanel.setPrefHeight(0);
                mTopContentPanel.setMaxHeight(0);
                mTopContentPanel.setVisible(false);
                mTopContentPanel.setManaged(false);
                mSpectralPanel.stop();
            } else {
                mSpectralPanel.setPrefHeight(300);
                mTopContentPanel.setCenter(mSpectralPanel);
                mTopContentPanel.setMinHeight(300);
                mTopContentPanel.setPrefHeight(300);
                mTopContentPanel.setMaxHeight(300);
                mTopContentPanel.setVisible(true);
                mTopContentPanel.setManaged(true);
                //Ensure the shared spectral panel is shown when displayed directly on the tuner page (it may have
                //been left invisible by a minimized Now Playing widget).
                mSpectralPanel.setVisible(true);
                mSpectralPanel.setManaged(true);
                mSpectralPanel.start();
            }
            Platform.runLater(() -> {
                mTopContentPanel.requestLayout();
                if (mRightContentPanel != null) mRightContentPanel.requestLayout();
            });
        }
        
    }

    @Override
    public void onToggleDetails() {
        toggleNowPlayingDetailsPanelVisibility();
        
    }

    @Override
    public void onToggleStreaming() {
        toggleBroadcastStatusPanelVisibility();
        
    }

    @Override
    public void onToggleResource() {
        toggleResourceStatusPanelVisibility();
        
    }

    @Override
    public boolean isSpectrumVisible() {
        if (mCurrentViewId != null && mCurrentViewId.equals("now_playing")) {
            return !mNowPlayingSpectrumDisabled;
        } else if (mCurrentViewId != null && mCurrentViewId.equals("tuners")) {
            return !mTunerSpectrumDisabled;
        }
        return false;
    }

    @Override
    public boolean isResourceVisible() {
        return mResourceStatusVisible;
    }

    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(SDRTrunk.class);

    private static final String PREFERENCE_BROADCAST_STATUS_VISIBLE = "sdrtrunk.broadcast.status.visible";
    private static final String PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE = "sdrtrunk.now.playing.details.visible";
    private static final String PREFERENCE_RESOURCE_STATUS_VISIBLE = "sdrtrunk.resource.status.visible";
    private static final String BASE_WINDOW_NAME = "sdrtrunk.main.window";
    private static final String CONTROLLER_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".control.panel";
    private static final String SPECTRAL_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".spectral.panel";
    private static final String WINDOW_FRAME_IDENTIFIER = BASE_WINDOW_NAME + ".frame";

    private boolean mBroadcastStatusVisible;
    private boolean mResourceStatusVisible;
    private boolean mNowPlayingDetailsVisible;

    private boolean mNowPlayingSpectrumDisabled = false;
    private boolean mTunerSpectrumDisabled = false;
    private String mCurrentViewId = "now_playing";

    private WindowsReliabilityManager mWindowsReliabilityManager;
    private SystemTrayManager mSystemTrayManager;
    private AudioRecordingManager mAudioRecordingManager;
    private ChannelAlertMonitor mChannelAlertMonitor;
    private io.github.dsheirer.monitor.DiskSpaceManager mDiskSpaceManager;
    private io.github.dsheirer.monitor.ConfigurationBackupService mConfigurationBackupService;
    private io.github.dsheirer.monitor.StreamingCredentialPreflight mStreamingCredentialPreflight;
    private io.github.dsheirer.transcription.RadioIdNameLearner mRadioIdNameLearner;
    private io.github.dsheirer.controller.channel.ChannelResumeService mChannelResumeService;
    private io.github.dsheirer.module.ai.PredictiveMaintenanceEngine mPredictiveMaintenanceEngine;
    private AudioStreamingManager mAudioStreamingManager;
    private StreamingWatchdog mStreamingWatchdog;
    private BroadcastStatusPanel mBroadcastStatusPanel;
    private ControllerPanel mControllerPanel;
    private DiagnosticMonitor mDiagnosticMonitor;
    private IconModel mIconModel = new IconModel();
    private PlaylistManager mPlaylistManager;
    private io.github.dsheirer.preference.ai.ToneDiscoveryManager mToneDiscoveryManager;
    private SettingsManager mSettingsManager;
    private SpectralDisplayPanel mSpectralPanel;
    private BorderPane mMainContentPanel;
    private BorderPane mTopContentPanel;
    private VBox mRightContentPanel;
    private JavaFxWindowManager mJavaFxWindowManager;
    private io.github.dsheirer.gui.SidebarPanel mSidebarPanel;
    private UserPreferences mUserPreferences = new UserPreferences();
    private static java.nio.channels.FileChannel sInstanceLockChannel;
    private static java.nio.channels.FileLock sInstanceLock;
    private TunerManager mTunerManager;
    private ApplicationLog mApplicationLog;
    private TwoToneLog mTwoToneLog;
    private StateJournal mStateJournal;
    private ResourceMonitor mResourceMonitor;
    private Rectangle2D mNormalBounds;
    private javafx.scene.Node mControllerResourceStatusPanel;
    private javafx.scene.Node mNowPlayingResourceStatusPanel;

    private String mTitle;

    private Stage mPrimaryStage;
    private AudioPlaybackManager mAudioPlaybackManager;
    private MapService mMapService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        mPrimaryStage = primaryStage;
        boolean headless = GraphicsEnvironment.isHeadless();

        if(headless)
        {
            mLog.info("starting main application headless");
            autoStartChannels();
        }
        else
        {
            mLog.info("starting main application gui");
            //The app intentionally keeps running when the main window is temporarily hidden to the tray. Disable
            //implicit exit before any startup splash/main-window transition so JavaFX never treats a transient
            //no-visible-window moment as an application shutdown request.
            Platform.setImplicitExit(false);
            try
            {

            mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, mTunerManager, mPlaylistManager, this::onViewChanged);
            mSidebarPanel = new io.github.dsheirer.gui.SidebarPanel(this);
            mControllerPanel = new io.github.dsheirer.controller.ControllerPanel(mPlaylistManager, mAudioPlaybackManager, mIconModel, mMapService,
                    mSettingsManager, mTunerManager, mUserPreferences, mNowPlayingDetailsVisible, this);
            mControllerPanel.addView("playlist_editor", mJavaFxWindowManager.getView(ViewIdentifier.PLAYLIST_EDITOR));
            mControllerPanel.addView("user_prefs", mJavaFxWindowManager.getView(ViewIdentifier.USER_PREFERENCES_EDITOR));
            mControllerPanel.addView("msg_viewer", mJavaFxWindowManager.getView(ViewIdentifier.RECORDING_VIEWER));
            mControllerPanel.addView("logs", mJavaFxWindowManager.getView(ViewIdentifier.LOGS));
            mSpectralPanel = new SpectralDisplayPanel(mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
            
            //Initialize the GUI
            initGUI();

            BorderPane root = new BorderPane();
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(SDRTrunk.class.getResource("/sdrtrunk_style.css").toExternalForm());
            } catch (Exception e) {
                mLog.error("Could not load stylesheet", e);
            }

            root.setCenter(mMainContentPanel);


            javafx.geometry.Rectangle2D screenSize = javafx.stage.Screen.getPrimary().getVisualBounds();
            primaryStage.setWidth(Math.max(800, screenSize.getWidth() * 0.6));
            primaryStage.setHeight(Math.max(600, screenSize.getHeight() * 0.6));
            primaryStage.centerOnScreen();


            primaryStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
                }
            });
            primaryStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
                }
            });
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
                }
            });
            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle2D(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
                }
            });

            try {
                java.awt.Image appIcon = javax.imageio.ImageIO.read(SDRTrunk.class.getResource("/images/SDRTrunk_Application_Icon.png"));
                if (java.awt.Taskbar.isTaskbarSupported() && java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    java.awt.Taskbar.getTaskbar().setIconImage(appIcon);
                }

                // Set multiple icon sizes to fix high-DPI blurriness and taskbar issues
                primaryStage.getIcons().addAll(
                    new javafx.scene.image.Image(SDRTrunk.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 16, 16, true, true),
                    new javafx.scene.image.Image(SDRTrunk.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 32, 32, true, true),
                    new javafx.scene.image.Image(SDRTrunk.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 64, 64, true, true),
                    new javafx.scene.image.Image(SDRTrunk.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 128, 128, true, true),
                    new javafx.scene.image.Image(SDRTrunk.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"))
                );
            } catch (Exception e) {
                mLog.error("Error setting application icon", e);
            }

            primaryStage.setTitle(mTitle);

            primaryStage.setScene(scene);
            //Show the window invisibly (opacity 0) so JavaFX lays out and paints the (initially empty) content while
            //the splash still covers it.  The window is revealed at full opacity only once the content has actually
            //rendered (see the reveal logic below), so the user goes straight from the splash to a fully-drawn window
            //instead of watching a bare/outlined shell fill in.
            primaryStage.setOpacity(0.0);
            primaryStage.show();

            // Show the first-time wizard after the stage has a scene so that JavaFX's
            // HeavyweightDialog.initOwner() can safely bind to the owner stage's stylesheet list.
            // Showing it before setScene()/show() causes a NullPointerException inside JavaFX.
            java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
            boolean wizardCompleted = p.getBoolean("sdrtrunk.first.time.wizard.completed", false);
            if (!wizardCompleted) {
                // Hide the always-on-top splash before showing the wizard; otherwise the splash
                // sits on top of the wizard and the user cannot see or interact with it.
                notifyPreloader(new SDRTrunkPreloader.HideNotification());
                io.github.dsheirer.gui.wizard.FirstTimeWizard wizard = new io.github.dsheirer.gui.wizard.FirstTimeWizard(mUserPreferences, mJavaFxWindowManager, primaryStage);
                wizard.showAndWait();
            }

            //Closing the main window triggers the same confirmation as the sidebar Exit, then fully quits the app.
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                confirmAndExit();
            });

            //Minimizing sends the app to the system tray to keep running as a background service (when the tray is
            //available); otherwise it minimizes to the taskbar as usual.
            primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
                if(isIconified && mSystemTrayManager != null && mSystemTrayManager.isAvailable())
                {
                    Platform.runLater(() -> {
                        primaryStage.hide();
                        mSystemTrayManager.notifyMinimizedToTray();
                    });
                }
            });

            // Reveal the fully-drawn window and hide the splash together, so the transition is splash -> ready window
            // with no empty/outlined intermediate state. A safety timeout guarantees the window is revealed even if
            // the readiness signal never arrives.
            final boolean[] revealed = {false};
            Runnable reveal = () -> {
                if(!revealed[0])
                {
                    revealed[0] = true;
                    primaryStage.setOpacity(1.0);
                    primaryStage.setIconified(false);
                    //Let JavaFX/native window state observe the now-visible main stage for one render pulse before
                    //hiding the preloader. This avoids an implicit-exit race on Windows jpackage launches where the
                    //splash can be the only visible native window while the main stage transitions from opacity 0.
                    final javafx.animation.AnimationTimer[] hideSplashOnNextPulse = new javafx.animation.AnimationTimer[1];
                    hideSplashOnNextPulse[0] = new javafx.animation.AnimationTimer() {
                        @Override
                        public void handle(long now)
                        {
                            hideSplashOnNextPulse[0].stop();
                            notifyPreloader(new SDRTrunkPreloader.HideNotification());
                            primaryStage.toFront();
                            primaryStage.requestFocus();
                        }
                    };
                    hideSplashOnNextPulse[0].start();
                }
            };

            //The window is shown invisibly first; we must wait until the heavy initial layout/paint of the content has
            //completed before revealing it. An AnimationTimer forces continuous render pulses: while the FX thread is
            //busy doing the first layout, frames are far apart; once several consecutive frames render smoothly the
            //content has settled and is fully painted, so we reveal. A wall-clock cap prevents waiting indefinitely.
            Runnable revealWhenRendered = () -> {
                final javafx.animation.AnimationTimer[] timer = new javafx.animation.AnimationTimer[1];
                final long[] startNanos = { -1 };
                final long[] lastPulseNanos = { -1 };
                final int[] smoothFrames = { 0 };
                timer[0] = new javafx.animation.AnimationTimer() {
                    @Override
                    public void handle(long now)
                    {
                        if(startNanos[0] < 0)
                        {
                            startNanos[0] = now;
                        }

                        if(lastPulseNanos[0] > 0)
                        {
                            long deltaMs = (now - lastPulseNanos[0]) / 1_000_000L;
                            //A smooth (~25fps+) frame means the FX thread is keeping up and no longer doing heavy work.
                            if(deltaMs < 40)
                            {
                                smoothFrames[0]++;
                            }
                            else
                            {
                                smoothFrames[0] = 0;
                            }
                        }
                        lastPulseNanos[0] = now;

                        long elapsedMs = (now - startNanos[0]) / 1_000_000L;
                        if(smoothFrames[0] >= 5 || elapsedMs >= 12000)
                        {
                            timer[0].stop();
                            reveal.run();
                        }
                    }
                };
                timer[0].start();
            };

            if(mControllerPanel != null)
            {
                mControllerPanel.setOnContentReady(() -> Platform.runLater(revealWhenRendered));
            }
            else
            {
                Platform.runLater(revealWhenRendered);
            }

            javafx.animation.PauseTransition splashFallback =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(15));
            splashFallback.setOnFinished(e -> reveal.run());
            splashFallback.play();

            // Wire up global hotkeys
            try {
                  new HotkeyManager(scene, new HotkeyManager.HotkeyListener() {
                      @Override public void onToggleSpectrum() { SDRTrunk.this.onToggleSpectrum(); }
                      @Override public void onToggleMute() { SDRTrunk.this.toggleAudioMute(); }
                      @Override public void onToggleNightMode() { ThemeManager.toggleNightMode(); }
                  });
                  mLog.info("HotkeyManager initialized");
                  
                  mSystemTrayManager = new SystemTrayManager(primaryStage, this);
                  mLog.info("SystemTrayManager initialized");
                  
                  io.github.dsheirer.gui.theme.ThemeManager.registerScene(scene);
              } catch (Exception e) {
                mLog.error("Failed to initialize HotkeyManager", e);
            }

            CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
            final boolean calibrating = !calibrationManager.isCalibrated() &&
                !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

            Platform.runLater(() -> {
                        if(calibrating)
                        {
                            CalibrationDialog calibrationDialog = mJavaFxWindowManager.getCalibrationDialog(mUserPreferences);
                            calibrationDialog.initOwner(primaryStage);
                            java.util.Optional<ButtonType> calibrate = calibrationDialog.showAndWait();
                            if(calibrate.isPresent() && calibrate.get().getText().equals("Calibrate"))
                            {
                                //Request focus and execute calibration
                                MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.VECTOR_CALIBRATION));
                                MyEventBus.getGlobalEventBus().post(new CalibrateRequest());
                            }
                            else
                            {
                                autoStartChannels();
                            }
                        }
                        else
                        {
                            autoStartChannels();
                        }
                    });



            try
            {
                TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(mSpectralPanel, mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
                mTunerManager.getDiscoveredTunerModel().addListener(tunerSpectralDisplayManager);
                mTunerManager.getDiscoveredTunerModel().addListener(this);
                Tuner tuner = tunerSpectralDisplayManager.showFirstTuner();

                if(tuner != null)
                {
                    updateTitle(tuner.getPreferredName());
                }

            }
            catch(Exception e)
            {
                mLog.error("Error during application startup", e);
            }

            //Start the local REST health/restart API in GUI mode too (opt-out via
            //-Dsdrtrunk.api.enabled=false or SDRTRUNK_API_ENABLED=false) so external monitors can
            //observe application health without requiring headless mode.
            String apiEnabled = System.getProperty("sdrtrunk.api.enabled", System.getenv("SDRTRUNK_API_ENABLED"));
            if(apiEnabled == null || !apiEnabled.equalsIgnoreCase("false"))
            {
                RestApiWatchdog.start(this, mTunerManager, mPlaylistManager.getChannelProcessingManager(),
                    mPlaylistManager.getBroadcastModel());
            }

            //Now that the main window exists, let a second launch raise it to the foreground.
            SingleInstanceManager.setFocusHandler(this::showMainWindow);
            }
            catch(Throwable startupError)
            {
                mLog.error("Fatal error during GUI startup", startupError);
                notifyPreloader(new SDRTrunkPreloader.HideNotification());
                try
                {
                    // Write an emergency crash file so the user can find details even without a console.
                    java.nio.file.Path logDir = mUserPreferences != null
                        ? mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog()
                        : java.nio.file.Path.of(System.getProperty("user.home"), "SDRTrunk", "logs");
                    java.nio.file.Files.createDirectories(logDir);
                    java.nio.file.Files.writeString(logDir.resolve("startup_crash.txt"),
                        "SDRTrunk startup failed:\n" + startupError + "\n\nSee sdrtrunk.log for full details.");
                }
                catch(Exception ignored) {}
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("SDRTrunk Startup Error");
                errorAlert.setHeaderText("SDRTrunk failed to start");
                errorAlert.setContentText(startupError.getMessage() != null
                    ? startupError.getMessage()
                    : startupError.getClass().getSimpleName() + " — see logs/startup_crash.txt for details.");
                errorAlert.showAndWait();
                Platform.exit();
            }
        }
    }

    /**
     * Acquires an exclusive file lock so only one instance of SDRTrunk runs at a time. Returns true if
     * this instance got the lock (or if locking could not be evaluated, in which case startup proceeds).
     * The lock is held for the JVM lifetime and released by the OS when the process exits.
     */
    private boolean acquireSingleInstanceLock()
    {
        try
        {
            java.nio.file.Path lockPath = mUserPreferences.getDirectoryPreference()
                .getDirectoryApplicationRoot().resolve("sdrtrunk.lock");
            sInstanceLockChannel = java.nio.channels.FileChannel.open(lockPath,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
            sInstanceLock = sInstanceLockChannel.tryLock();
            return sInstanceLock != null;
        }
        catch(java.nio.channels.OverlappingFileLockException e)
        {
            return false;
        }
        catch(Exception e)
        {
            //Don't block startup if the lock can't be evaluated (e.g. unusual filesystem).
            mLog.warn("Unable to evaluate single-instance lock - continuing startup", e);
            return true;
        }
    }

    public void init() throws Exception
    {
        //Refuse to start a second instance. Multiple instances fight over the same tuners, USB bus, CPU,
        //and the REST API port (which is what the "Address already in use" errors indicate), which on a
        //resource-constrained host leads directly to an unresponsive UI. The lock is released
        //automatically when this JVM exits (including a forced kill by the watchdog).
        if(!acquireSingleInstanceLock())
        {
            mLog.error("Another instance of SDRTrunk is already running - exiting this instance to avoid " +
                "resource contention. Close the other instance first.");
            // Show a visible dialog — without this the app silently disappears, which looks like a crash.
            javax.swing.SwingUtilities.invokeLater(() ->
                javax.swing.JOptionPane.showMessageDialog(null,
                    "SDRTrunk is already running.\nClose the existing instance before starting a new one.",
                    "SDRTrunk Already Running",
                    javax.swing.JOptionPane.WARNING_MESSAGE));
            // Give Swing time to show the dialog before the JVM exits.
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            System.exit(0);
        }

        String operatingSystem = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        ThemeManager themeManager = new ThemeManager();
        if(operatingSystem.contains("mac") || operatingSystem.contains("nux")) {
            try {
            } catch(Exception e) {
                mLog.error("Error trying to set LookAndFeelFactory extension for OS [" + operatingSystem + "]");
                DiagnosticEngine.InsightCard card = DiagnosticEngine.mapError(e, "LookAndFeel init");
                if (card != null) { mLog.warn("Diagnostic: {} - {}", card.title, card.remediation); }
            }
        }

        if(!GraphicsEnvironment.isHeadless())
        {
        }
        
        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.1));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Initializing Application Logs..."));

        mApplicationLog = new ApplicationLog(mUserPreferences);
        mApplicationLog.start();

        mTwoToneLog = new TwoToneLog(mUserPreferences);
        mTwoToneLog.start();
        UsbMonitorManager.manage(mUserPreferences);
        io.github.dsheirer.gui.WindowsReliabilityManager.manage(mUserPreferences);
        
        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.2));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Loading SDRPlay API..."));

        //Note: invoke this early in the application lifecycle, before the TunerManager causes the sdrplay classes
        //to be loaded since the jextract auto-generated code attempts to load the library by name and that can fail
        //when the library was not installed into a normal/default location, particularly on windows OS systems.
        if(SDRPlayLibraryHelper.LOADED)
        {
            mLog.debug("SDRPlay API native library preemptively loaded");
        }

        mResourceMonitor = new ResourceMonitor(mUserPreferences);

        ThreadPool.logSettings();
        
        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.4));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Loading Preferences..."));

        //Load properties file
        loadProperties();

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();

        //Register FontAwesome so we can use the fonts in Swing windows

        mTunerManager = new TunerManager(mUserPreferences);
        mTunerManager.start();

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.5));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Starting Tuner Manager..."));

        mSettingsManager = new SettingsManager();

        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, mTunerManager, aliasModel, eventLogManager, mIconModel);

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.7));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Initializing Display Managers..."));
        boolean headless = GraphicsEnvironment.isHeadless();
        mDiagnosticMonitor = new DiagnosticMonitor(mUserPreferences, mPlaylistManager.getChannelProcessingManager(),
                mTunerManager, headless);
        mDiagnosticMonitor.start();
        if (mJavaFxWindowManager != null) mJavaFxWindowManager.setDiagnosticMonitor(mDiagnosticMonitor);

        io.github.dsheirer.monitor.RemoteSessionMonitor.init();

        CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
        final boolean calibrating = !calibrationManager.isCalibrated() &&
            !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

        //Adaptive DSP path selection: when the interactive calibration dialog will not be shown (headless,
        //or the user suppressed it) but the SIMD implementations are not yet benchmarked, calibrate in the
        //background so we use the fastest vector path instead of the slow scalar fallback.  Calibration is
        //persisted, so this runs at most once per machine.
        boolean calibrationDialogWillShow = !headless && calibrating;
        if(mUserPreferences.getApplicationPreference().isAutoVectorCalibrationEnabled() &&
            !calibrationManager.isCalibrated() && !calibrationDialogWillShow)
        {
            Thread calibrationThread = new Thread(() -> {
                try
                {
                    mLog.info("Auto-calibrating SIMD/vector DSP implementations in the background for optimal performance");
                    calibrationManager.calibrate();
                }
                catch(Throwable t)
                {
                    mLog.warn("Background DSP calibration did not complete - continuing with current settings", t);
                }
            }, "sdrtrunk-auto-vector-calibration");
            calibrationThread.setDaemon(true);
            calibrationThread.setPriority(Thread.MIN_PRIORITY);
            calibrationThread.start();
        }

        new ChannelSelectionManager(mPlaylistManager.getChannelModel());

        mAudioPlaybackManager = new AudioPlaybackManager(mUserPreferences);

        mAudioRecordingManager = new AudioRecordingManager(mUserPreferences);
        mChannelAlertMonitor = new ChannelAlertMonitor(mPlaylistManager.getChannelModel(), mPlaylistManager.getChannelProcessingManager(), mUserPreferences);
        mAudioRecordingManager.start();
        mChannelAlertMonitor.start();

        mAudioStreamingManager = new AudioStreamingManager(mPlaylistManager.getBroadcastModel(), BroadcastFormat.MP3,
            mUserPreferences);
        mAudioStreamingManager.start();

        mStreamingWatchdog = new StreamingWatchdog(mPlaylistManager.getBroadcastModel());
        mStreamingWatchdog.start();

        //Automated disk space management and daily configuration backups for unattended operation
        mDiskSpaceManager = new io.github.dsheirer.monitor.DiskSpaceManager(mUserPreferences);
        mDiskSpaceManager.start();
        mConfigurationBackupService = new io.github.dsheirer.monitor.ConfigurationBackupService(mUserPreferences);
        mConfigurationBackupService.start();

        //Validate streaming credentials at startup and daily so expired keys alert before feeds die
        mStreamingCredentialPreflight = new io.github.dsheirer.monitor.StreamingCredentialPreflight(
            mPlaylistManager.getBroadcastModel());
        mStreamingCredentialPreflight.start();

        //Learn friendly names for radio IDs from audio transcriptions (digital protocols only)
        mRadioIdNameLearner = new io.github.dsheirer.transcription.RadioIdNameLearner(aliasModel, mUserPreferences);
        mRadioIdNameLearner.start();

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.8));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Initializing Audio Services..."));
        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(mUserPreferences);

        //Wire up two-tone paging detection so it receives live audio.  The detector routes audio to each configured
        //detector based on the alias(es) selected for it (see TwoToneDetector), and fires app, Zello, and MQTT alerts.
        mAudioPlaybackManager.setTwoToneDetector(
            new io.github.dsheirer.dsp.tone.TwoToneDetector(mPlaylistManager));

        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(duplicateCallDetector);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioPlaybackManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioRecordingManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mChannelAlertMonitor);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioStreamingManager);

        mMapService = new MapService(aliasModel, mIconModel);
        mPlaylistManager.getChannelProcessingManager().addDecodeEventListener(mMapService);

        mNowPlayingDetailsVisible = mPreferences.getBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, true);

        mPlaylistManager.init();
        
        mToneDiscoveryManager = new io.github.dsheirer.preference.ai.ToneDiscoveryManager(mUserPreferences, mPlaylistManager);

        //Deferred startup validation: receive-chain self-test and playlist lint, run after tuner
        //discovery and channel auto-start have settled so results reflect steady state.
        io.github.dsheirer.util.ThreadPool.SCHEDULED.schedule(() -> {
            try
            {
                io.github.dsheirer.monitor.StartupSelfTest.run(mUserPreferences, mTunerManager);
            }
            catch(Throwable t)
            {
                mLog.error("Startup self-test error", t);
            }

            try
            {
                io.github.dsheirer.playlist.PlaylistLinter.lint(mPlaylistManager);
            }
            catch(Throwable t)
            {
                mLog.error("Playlist lint error", t);
            }
        }, 90, java.util.concurrent.TimeUnit.SECONDS);

        notifyPreloader(new javafx.application.Preloader.ProgressNotification(0.9));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Finalizing Startup..."));
        // Initialize StateJournal for event recording
        try {
            mStateJournal = StateJournal.init(
                mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot());
            mStateJournal.record("application_init", java.util.Map.of("headless", String.valueOf(headless)));
            mLog.info("StateJournal initialized");
        } catch (Exception e) {
            mLog.error("Failed to initialize StateJournal", e);
        }
        
        notifyPreloader(new javafx.application.Preloader.ProgressNotification(1.0));
        notifyPreloader(new SDRTrunkPreloader.TextNotification("Starting UI..."));
    }

    /**
     * Shows a dialog that lists the channels that have been designated for auto-start, sorted by auto-start order and
     * allows the user to start now, cancel, or allow the timer to expire and then start the channels.  The dialog will
     * only show if there are one ore more channels designated for auto-start.
     */
    private void autoStartChannels()
    {
        List<Channel> channels = mPlaylistManager.getChannelModel().getAutoStartChannels();

        if(channels.size() > 0)
        {
            if(GraphicsEnvironment.isHeadless())
            {
                for(Channel channel: channels)
                {
                    try
                    {
                        mLog.info("Auto-starting channel " + channel.getName());
                        mPlaylistManager.getChannelProcessingManager().start(channel);
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Channel: " + channel.getName() + " auto-start failed: " + ce.getMessage());
                    }
                }
            }
            else
            {
                new ChannelAutoStartFrame(mPlaylistManager.getChannelProcessingManager(), channels, mUserPreferences,
                    mPrimaryStage);
            }
        }

        //Resume channels that were running when a previous session ended unexpectedly (crash/kill),
        //including manually-started channels not flagged for auto-start, then begin tracking the
        //current session's running channel state.
        mChannelResumeService = new io.github.dsheirer.controller.channel.ChannelResumeService(
            mPlaylistManager.getChannelProcessingManager(), mPlaylistManager.getChannelModel(), mUserPreferences);
        mChannelResumeService.resume();
        mChannelResumeService.start();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
        try {

        /**
         * Setup main Stage window
         */
        mTitle = SystemProperties.getInstance().getApplicationName();

        // The main application window is now JavaFX, not Stage,
        // so setting the taskbar icon is handled in the JavaFX start() method.


        javafx.geometry.Dimension2D dimension = mUserPreferences.getSwingPreference().getDimension(WINDOW_FRAME_IDENTIFIER);

        mSpectralPanel.setPrefSize(1280, 300);
        mSpectralPanel.setMinSize(0, 0);
        mControllerPanel.setPrefSize(1280, 500);
        mControllerPanel.setMinSize(0, 0);

        if(dimension != null)
        {
            javafx.geometry.Dimension2D spectral = mUserPreferences.getSwingPreference().getDimension(SPECTRAL_PANEL_IDENTIFIER);
            if(spectral != null)
            {
                
                mSpectralPanel.setPrefHeight(spectral.getHeight());
                // mSpectralPanel.setSize(spectral);
            }

            javafx.geometry.Dimension2D controller = mUserPreferences.getSwingPreference().getDimension(CONTROLLER_PANEL_IDENTIFIER);
            if(controller != null)
            {
                
                mControllerPanel.setPrefHeight(controller.getHeight());
                // mControllerPanel.setSize(controller);
            }


        }
        else
        {
            javafx.geometry.Rectangle2D screenSize = javafx.stage.Screen.getPrimary().getVisualBounds();
            int width = (int) (screenSize.getWidth() * 0.6);
            int height = (int) (screenSize.getHeight() * 0.6);
        }

        javafx.geometry.Point2D location = mUserPreferences.getSwingPreference().getLocation(WINDOW_FRAME_IDENTIFIER);
        if(location != null)
        {
        }
        else
        {
        }

        mMainContentPanel = new BorderPane();
        mMainContentPanel.setMinSize(0, 0);

        BorderPane contentWithSidebar = new BorderPane();
        contentWithSidebar.setMinSize(0, 0);
        contentWithSidebar.setLeft(mSidebarPanel);

        mRightContentPanel = new VBox();
        mRightContentPanel.setMinSize(0, 0);

        mTopContentPanel = new BorderPane();
        mTopContentPanel.setMinHeight(0);
        mTopContentPanel.setPrefHeight(0);
        mTopContentPanel.setMaxHeight(0);
        mTopContentPanel.setVisible(false);
        mTopContentPanel.setManaged(false);
        mTopContentPanel.setCenter(mSpectralPanel);

        javafx.scene.control.SplitPane resizablePane = new javafx.scene.control.SplitPane();
        resizablePane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        resizablePane.getItems().addAll(mTopContentPanel, mControllerPanel);
        resizablePane.setDividerPositions(0.35);
        mRightContentPanel.getChildren().addAll(mControllerPanel.getAudioPanel(), resizablePane);
        VBox.setVgrow(resizablePane, Priority.ALWAYS);

        mRightContentPanel.maxHeightProperty().bind(contentWithSidebar.heightProperty());
        BorderPane.setAlignment(mRightContentPanel, Pos.TOP_LEFT);

        contentWithSidebar.setCenter(mRightContentPanel);
        mMainContentPanel.setCenter(contentWithSidebar);
        mBroadcastStatusVisible = mPreferences.getBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, false);
        mResourceStatusVisible = mPreferences.getBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, true);

        mControllerResourceStatusPanel = mJavaFxWindowManager.createStatusPanel(mResourceMonitor);
        mNowPlayingResourceStatusPanel = mJavaFxWindowManager.createStatusPanel(mResourceMonitor);

        mControllerPanel.getNowPlayingPanel().setNodes(mSpectralPanel, getBroadcastStatusPanel(), mNowPlayingResourceStatusPanel);


        mResourceMonitor.start();

        if(mUserPreferences.getAIPreference().isSystemHealthAdvisorEnabled())
        {
            mPredictiveMaintenanceEngine = new io.github.dsheirer.module.ai.PredictiveMaintenanceEngine(mResourceMonitor);
        }

        mControllerPanel.setResourcePanel(mControllerResourceStatusPanel);

        // Ensure the initial view is shown
        onViewChanged("now_playing");

        } catch (Exception e) {
            mLog.error("Error creating initGUI", e);
            DiagnosticEngine.InsightCard card = DiagnosticEngine.mapError(e, "initGUI");
            if (card != null) { mLog.warn("Diagnostic: {} - {}", card.title, card.remediation); }
        }
    }


    @Override
    public void stop() throws Exception {
        processShutdown();
        System.exit(0);
    }

    private void processShutdown()
    {
        mLog.info("Application shutdown started ...");
        io.github.dsheirer.gui.WindowsReliabilityManager.stopWatchdog();
        if(mDiagnosticMonitor != null) mDiagnosticMonitor.stop();
        if (mPrimaryStage != null) {
            if (!mPrimaryStage.isMaximized() || mNormalBounds == null) {
                mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, new javafx.geometry.Point2D((int)mPrimaryStage.getX(), (int)mPrimaryStage.getY()));
                mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, new javafx.geometry.Dimension2D((int)mPrimaryStage.getWidth(), (int)mPrimaryStage.getHeight()));
            } else {
                mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, new javafx.geometry.Point2D((int)mNormalBounds.getMinX(), (int)mNormalBounds.getMinY()));
                mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, new javafx.geometry.Dimension2D((int)mNormalBounds.getWidth(), (int)mNormalBounds.getHeight()));
            }
            mUserPreferences.getSwingPreference().setMaximized(WINDOW_FRAME_IDENTIFIER, mPrimaryStage.isMaximized());
        }
        if(mSpectralPanel != null) mUserPreferences.getSwingPreference().setDimension(SPECTRAL_PANEL_IDENTIFIER, new javafx.geometry.Dimension2D((int)mSpectralPanel.getWidth(), (int)mSpectralPanel.getHeight()));
        if(mControllerPanel != null) mUserPreferences.getSwingPreference().setDimension(CONTROLLER_PANEL_IDENTIFIER, new javafx.geometry.Dimension2D((int)mControllerPanel.getWidth(), (int)mControllerPanel.getHeight()));
        if(mJavaFxWindowManager != null) mJavaFxWindowManager.shutdown();
        mLog.info("Stopping channels ...");
        if(mPlaylistManager != null) mPlaylistManager.getChannelProcessingManager().shutdown();
        if(mChannelResumeService != null) mChannelResumeService.shutdown();
        if(mPredictiveMaintenanceEngine != null) mPredictiveMaintenanceEngine.stop();
        if(mAudioRecordingManager != null) mAudioRecordingManager.stop();
        if(mChannelAlertMonitor != null) mChannelAlertMonitor.stop();
        if(mDiskSpaceManager != null) mDiskSpaceManager.stop();
        if(mConfigurationBackupService != null) mConfigurationBackupService.stop();
        if(mStreamingCredentialPreflight != null) mStreamingCredentialPreflight.stop();
        if(mResourceMonitor != null) mResourceMonitor.stop();

        mLog.info("Stopping spectral display ...");
        if(mSpectralPanel != null) mSpectralPanel.clearTuner();
        mLog.info("Stopping tuners ...");
        if(mTunerManager != null) mTunerManager.stop();
        mLog.info("Shutdown complete.");
        if(mStateJournal != null) {
            mStateJournal.record("application_shutdown");
            mStateJournal.stop();
        }
        if(mApplicationLog != null) mApplicationLog.stop();
        if(mTwoToneLog != null) mTwoToneLog.stop();
        SingleInstanceManager.release();
    }

    /**
     * Performs application shutdown and exits the JVM.  Used after the user confirms they want to quit (from the
     * sidebar Exit item, the window close button, or the system tray).
     */
    public void shutdownAndExit()
    {
        processShutdown();
        System.exit(0);
    }

    /**
     * Shows a themed confirmation dialog and, if the user confirms, shuts down and exits the application.  Must be
     * called on the JavaFX Application Thread.
     */
    public void confirmAndExit()
    {
        //Bring the window forward so the confirmation has visible context (it may be hidden in the tray).
        if(mPrimaryStage != null)
        {
            if(!mPrimaryStage.isShowing())
            {
                mPrimaryStage.show();
            }
            mPrimaryStage.setIconified(false);
            mPrimaryStage.toFront();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit SDRTrunk");
        alert.setHeaderText("Exit SDRTrunk?");
        alert.setContentText("This will stop all channels and audio streaming and close the application.");
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        if(mPrimaryStage != null && mPrimaryStage.isShowing())
        {
            alert.initOwner(mPrimaryStage);
        }

        ThemeManager.applyCurrentTheme(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();

        if(result.isPresent() && result.get() == ButtonType.OK)
        {
            shutdownAndExit();
        }
    }

    /**
     * Restores and focuses the main application window.  Used by the system tray to bring the app back from the
     * background.  Safe to call from any thread.
     */
    public void showMainWindow()
    {
        Platform.runLater(() -> {
            if(mPrimaryStage != null)
            {
                mPrimaryStage.show();
                mPrimaryStage.setIconified(false);
                mPrimaryStage.toFront();
                mPrimaryStage.requestFocus();

                //On Windows a plain toFront() frequently fails to steal the foreground from another application
                //(e.g. the Explorer window the user double-clicked the .exe from).  Briefly toggling always-on-top
                //forces the OS to raise and focus the window, then restores normal stacking.
                boolean wasAlwaysOnTop = mPrimaryStage.isAlwaysOnTop();
                mPrimaryStage.setAlwaysOnTop(true);
                mPrimaryStage.setAlwaysOnTop(wasAlwaysOnTop);
            }
        });
    }

    /**
     * Toggles muting of all application audio output.
     * @return the new muted state (true if now muted)
     */
    public boolean toggleAudioMute()
    {
        if(mAudioPlaybackManager != null)
        {
            return mAudioPlaybackManager.toggleMasterMuted();
        }

        return false;
    }

    /**
     * @return true if all application audio output is currently muted.
     */
    public boolean isAudioMuted()
    {
        return mAudioPlaybackManager != null && mAudioPlaybackManager.isMasterMuted();
    }

    /**
     * Stops all currently playing channels without exiting the application.  Runs off the JavaFX thread so the UI
     * stays responsive.
     */
    public void stopAllChannels()
    {
        if(mPlaylistManager != null)
        {
            io.github.dsheirer.util.ThreadPool.CACHED.submit(() ->
                mPlaylistManager.getChannelProcessingManager().stopAllChannels());
        }
    }

    /**
     * (Re)starts all channels flagged for auto-start that are not already processing.  Runs off the JavaFX thread
     * so the UI stays responsive.
     */
    public void restartAutoplayChannels()
    {
        if(mPlaylistManager == null)
        {
            return;
        }

        io.github.dsheirer.util.ThreadPool.CACHED.submit(() -> {
            List<Channel> channels = mPlaylistManager.getChannelModel().getAutoStartChannels();

            for(Channel channel : channels)
            {
                try
                {
                    if(!mPlaylistManager.getChannelProcessingManager().isProcessing(channel))
                    {
                        mPlaylistManager.getChannelProcessingManager().start(channel);
                    }
                }
                catch(ChannelException ce)
                {
                    mLog.error("Tray restart - channel [" + channel.getName() + "] failed: " + ce.getMessage());
                }
            }
        });
    }

    /**
     * Lazy constructor for broadcast status panel
     */
    private BroadcastStatusPanel getBroadcastStatusPanel()
    {
        if(mBroadcastStatusPanel == null)
        {
            mBroadcastStatusPanel = new BroadcastStatusPanel(mPlaylistManager.getBroadcastModel(), mUserPreferences,
                "application.broadcast.status.panel");
            mBroadcastStatusPanel.setPrefSize((double)(double)880, 70);
            mBroadcastStatusPanel.setDisablePanel(false);
        }

        return mBroadcastStatusPanel;
    }

    /**
     * Toggles visibility of the broadcast channels status panel at the bottom of the controller panel
     */
    private void toggleBroadcastStatusPanelVisibility() {
        mBroadcastStatusVisible = !mBroadcastStatusVisible;
        mPreferences.putBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, mBroadcastStatusVisible);
        Platform.runLater(() -> {
            mControllerPanel.getNowPlayingPanel().setBroadcastStatusPanelVisible(mBroadcastStatusVisible);
            mMainContentPanel.requestLayout();
        });
    }



    /**
     * Toggles visibility of the resource status panel at the bottom of the main UI window
     */
    private void toggleResourceStatusPanelVisibility() {
        mResourceStatusVisible = !mResourceStatusVisible;
        mPreferences.putBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, mResourceStatusVisible);

        Platform.runLater(() -> {
            if (mCurrentViewId.equals("tuners")) {
                mControllerPanel.setResourcePanelVisible(mResourceStatusVisible);
            } else if (mCurrentViewId.equals("now_playing")) {
                mControllerPanel.getNowPlayingPanel().setResourceStatusPanelVisible(mResourceStatusVisible);
            }
        });
    }

    /**
     * Toggles visibility of the Now Playing channel details panel
     */
    private void toggleNowPlayingDetailsPanelVisibility()
    {
        mNowPlayingDetailsVisible = !mNowPlayingDetailsVisible;
        mControllerPanel.getNowPlayingPanel().setDetailTabsVisible(mNowPlayingDetailsVisible);
        mPreferences.putBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, mNowPlayingDetailsVisible);
    }


    /**
     * Loads the application properties file from the user's home directory,
     * creating the properties file for the first-time, if necessary
     */
    private void loadProperties()
    {
        Path propertiesPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("SDRTrunk.properties");

        if(!Files.exists(propertiesPath))
        {
            try
            {
                mLog.info("SDRTrunk - creating application properties file [" + propertiesPath.toAbsolutePath() + "]");
                Files.createFile(propertiesPath);
            }
            catch(IOException e)
            {
                mLog.error("SDRTrunk - couldn't create application properties file [" + propertiesPath.toAbsolutePath(), e);
            }
        }

        if(Files.exists(propertiesPath))
        {
            SystemProperties.getInstance().load(propertiesPath);
        }
        else
        {
            mLog.error("SDRTrunk - couldn't find or recreate the SDRTrunk application properties file");
        }
    }

    /**
     * Gets (or creates) the SDRTRunk application home directory.
     *
     * Note: the user can change this setting to allow log files and other
     * files to reside elsewhere on the file system.
     */
    private Path getHomePath()
    {
        Path homePath = FileSystems.getDefault()
            .getPath(System.getProperty("user.home"), "SDRTrunk");

        if(!Files.exists(homePath))
        {
            try
            {
                Files.createDirectory(homePath);

                mLog.info("SDRTrunk - created application home directory [" +
                    homePath.toString() + "]");
            }
            catch(Exception e)
            {
                homePath = null;

                mLog.error("SDRTrunk: exception while creating SDRTrunk home " +
                    "directory in the user's home directory", e);
            }
        }

        return homePath;
    }

    @Override
    public void receive(TunerEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                updateTitle(event.getTuner().getPreferredName());
                break;
            case REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY:
                updateTitle(null);
                break;
            case NOTIFICATION_SHUTTING_DOWN:
                Tuner currentTuner = mSpectralPanel.getTuner();

                if(event.hasTuner() && event.getTuner().equals(currentTuner) || currentTuner == null)
                {
                    updateTitle(null);
                }
                break;
        }
    }

    /**
     * Updates the title bar with the tuner name
     * @param tunerName optional
     */
    private void updateTitle(String tunerName)
    {
        final String finalTitle = tunerName != null ? mTitle + " - " + tunerName : mTitle;
        if (mPrimaryStage != null) {
            Platform.runLater(() -> mPrimaryStage.setTitle(finalTitle));
        }
    }


    /**
     * Broadcast status panel visible toggle menu item
     */
    public class BroadcastStatusVisibleMenuItem extends CheckMenuItem
    {
        public BroadcastStatusVisibleMenuItem()
        {
            super("Show Streaming Status");
            setSelected(mBroadcastStatusVisible);
            setOnAction(e -> {
                toggleBroadcastStatusPanelVisibility();
                setSelected(mBroadcastStatusVisible);
            });
        }
    }

    /**
     * Resource status panel visible toggle menu item
     */
    public class ResourceStatusVisibleMenuItem extends CheckMenuItem
    {
        public ResourceStatusVisibleMenuItem()
        {
            super("Show Resource Status");
            setSelected(mResourceStatusVisible);
            setOnAction(e -> {
                toggleResourceStatusPanelVisibility();
                setSelected(mResourceStatusVisible);
            });
        }
    }

    /**
     * Now Playing channel details visible toggle menu item
     */
    public class NowPlayingChannelDetailsVisibleMenuItem extends CheckMenuItem
    {
        public NowPlayingChannelDetailsVisibleMenuItem()
        {
            super("Show Now Playing Channel Details");
            setSelected(mNowPlayingDetailsVisible);
            setOnAction(e -> {
                toggleNowPlayingDetailsPanelVisibility();
                setSelected(mNowPlayingDetailsVisible);
            });
        }
    }

        /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        System.setProperty("com.github.weisj.jsvg.disableStax", "true");
        System.setProperty("com.ctc.wstx.maxAttributeSize", "10000000");
        System.setProperty("jdk.xml.maxAttributeSize", "10000000");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("prism.allowhidpi", "true");

        // Parse --headless argument
        boolean headless = false;
        for(String arg : args)
        {
            if("--headless".equalsIgnoreCase(arg))
            {
                headless = true;
                break;
            }
        }

        //Enforce a single running instance.  If another instance is already running, it has been signaled to bring
        //its window to the foreground and this duplicate launch exits immediately (before any heavy startup).
        if(!SingleInstanceManager.acquireLockOrSignalRunningInstance())
        {
            System.exit(0);
        }

        if(headless)
        {
            System.setProperty("java.awt.headless", "true");
            try
            {
                SDRTrunk sdrTrunk = new SDRTrunk();
                sdrTrunk.init();
                sdrTrunk.startHeadless();
            }
            catch(Exception e)
            {
                LoggerFactory.getLogger(SDRTrunk.class).error("Error starting SDRTrunk in headless mode", e);
            }
        }
        else
        {
            // GPU detection / fallback: probe with VolatileImage
            boolean gpuOk = false;
            try
            {
                java.awt.GraphicsConfiguration gc = java.awt.GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
                java.awt.image.VolatileImage vi = gc.createCompatibleVolatileImage(64, 64);
                int status = vi.validate(gc);
                gpuOk = (status != java.awt.image.VolatileImage.IMAGE_INCOMPATIBLE);
                vi.flush();
            }
            catch(Exception e)
            {
                gpuOk = false;
            }

            if(gpuOk)
            {
                System.setProperty("sun.java2d.d3d", "true");
            }
            else
            {
                System.setProperty("prism.order", "sw");
                LoggerFactory.getLogger(SDRTrunk.class).warn("GPU probe failed or incompatible; falling back to software rendering (prism.order=sw)");
            }

            Application.launch(SDRTrunk.class, args);
        }
    }

    /**
     * Starts SDRTrunk in headless mode (no GUI). Auto-starts channels and starts the REST API watchdog.
     */
    private void startHeadless()
    {
        mLog.info("Starting SDRTrunk in HEADLESS mode");
        autoStartChannels();
        RestApiWatchdog.start(this, mTunerManager, mPlaylistManager.getChannelProcessingManager(),
            mPlaylistManager.getBroadcastModel());
    }

    public void onViewChanged(String id) {
        if (id == null) return;

        if (mSidebarPanel != null) {
            mSidebarPanel.setActive(id);
        }

        if (id.startsWith("playlist_")) {
            mCurrentViewId = "playlist_editor";
            mTopContentPanel.setCenter(null);
            mTopContentPanel.setMinHeight(0);
            mTopContentPanel.setPrefHeight(0);
            mTopContentPanel.setMaxHeight(0);
            mTopContentPanel.setVisible(false);
            mTopContentPanel.setManaged(false);
            mSpectralPanel.stop();
            mControllerPanel.setResourcePanelVisible(false);
            mControllerPanel.showView("playlist_editor");
            
            Platform.runLater(() -> {
                mTopContentPanel.requestLayout();
                if (mRightContentPanel != null) mRightContentPanel.requestLayout();
            });
            return;
        }

        mCurrentViewId = id;

        if (id.equals("now_playing")) {
            mTopContentPanel.setCenter(null);
            mTopContentPanel.setMinHeight(0);
            mTopContentPanel.setPrefHeight(0);
            mTopContentPanel.setMaxHeight(0);
            mTopContentPanel.setVisible(false);
            mTopContentPanel.setManaged(false);
            mControllerPanel.setResourcePanelVisible(false);
            mControllerPanel.getNowPlayingPanel().setNodes(mSpectralPanel, getBroadcastStatusPanel(), mNowPlayingResourceStatusPanel);
            mControllerPanel.getNowPlayingPanel().setSpectralPanelVisible(!mNowPlayingSpectrumDisabled);
            //Let the Now Playing panel decide based on the widget's visible/minimized state, so a minimized or
            //hidden Spectrum/Waterfall widget keeps the DFT paused even after switching back to this view.
            mControllerPanel.getNowPlayingPanel().updateSpectrumProcessing();
        } else if (id.equals("tuners")) {
            if (!mTunerSpectrumDisabled) {
                double prefHeight = mUserPreferences.getSwingPreference().getDimension("spectrum_v2") != null ? mUserPreferences.getSwingPreference().getDimension("spectrum_v2").getHeight() : 300;
                mSpectralPanel.setPrefHeight(prefHeight);
                mTopContentPanel.setCenter(mSpectralPanel);
                mTopContentPanel.setMinHeight(0);
                mTopContentPanel.setPrefHeight(prefHeight);
                mTopContentPanel.setMaxHeight(Double.MAX_VALUE);
                mTopContentPanel.setVisible(true);
                mTopContentPanel.setManaged(true);
                //The shared spectral panel may have been left invisible by a minimized Now Playing widget; ensure
                //it is shown when displayed directly on the tuner page.
                mSpectralPanel.setVisible(true);
                mSpectralPanel.setManaged(true);
                mSpectralPanel.start();
            } else {
                mTopContentPanel.setCenter(null);
                mTopContentPanel.setMinHeight(0);
                mTopContentPanel.setPrefHeight(0);
                mTopContentPanel.setMaxHeight(0);
                mTopContentPanel.setVisible(false);
                mTopContentPanel.setManaged(false);
                mSpectralPanel.stop();
            }
            mControllerPanel.setResourcePanelVisible(mResourceStatusVisible);
        } else {
            mTopContentPanel.setCenter(null);
            mTopContentPanel.setMinHeight(0);
            mTopContentPanel.setPrefHeight(0);
            mTopContentPanel.setMaxHeight(0);
            mTopContentPanel.setVisible(false);
            mTopContentPanel.setManaged(false);
            mSpectralPanel.stop();
            mControllerPanel.setResourcePanelVisible(false);
        }

        mControllerPanel.showView(id);

        Platform.runLater(() -> {
            mTopContentPanel.requestLayout();
            if (mRightContentPanel != null) mRightContentPanel.requestLayout();
        });

        
    }

    @Override
    public void onItemSelected(String id) {
        if (id.equals("exit")) {
            confirmAndExit();
            return;
        }

        onViewChanged(id);

        if (id.startsWith("playlist_")) {
            if (id.equals("playlist_playlists")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.ViewPlaylistRequest());
            } else if (id.equals("playlist_channels")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.channel.ChannelTabRequest() {
                    @Override
                    public io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName getTabName() { return io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName.CHANNEL; }
                });
            } else if (id.equals("playlist_aliases")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.alias.AliasTabRequest() {
                    @Override
                    public io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName getTabName() { return io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName.ALIAS; }
                });
            } else if (id.equals("playlist_streaming")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.streaming.StreamTabRequest() {
                    @Override
                    public io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName getTabName() { return io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName.STREAM; }
                });
            } else if (id.equals("playlist_radioreference")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.radioreference.ViewRadioReferenceRequest());
            } else if (id.equals("playlist_twotones")) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.twotone.TwoToneTabRequest() {
                    @Override
                    public io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName getTabName() { return io.github.dsheirer.gui.playlist.PlaylistEditorRequest.TabName.TWO_TONE; }
                });
            }
        }
    }

    @Override
    public void onActionRequested(String actionId) {
    }

    /**
     * Toggles night/dark mode via ThemeManager.
     * Re-applies the theme by triggering a registry re-read on Windows.
     */
    private void toggleNightMode() {
        mLog.info("Toggle night mode requested");
        try {
            io.github.dsheirer.gui.theme.ThemeManager.toggleNightMode();
            mLog.info("Night mode toggled via ThemeManager");
        } catch (Exception e) {
            mLog.error("Error toggling night mode", e);
        }
    }

}
