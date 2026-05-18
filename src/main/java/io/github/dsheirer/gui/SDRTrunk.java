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

import com.jidesoft.plaf.LookAndFeelFactory;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.DuplicateCallDetector;
import io.github.dsheirer.audio.broadcast.AudioStreamingManager;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
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


import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.embed.swing.SwingNode;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ButtonType;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import io.github.dsheirer.gui.theme.ThemeManager;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class SDRTrunk extends Application implements Listener<TunerEvent>, io.github.dsheirer.gui.VisibilityListener, io.github.dsheirer.gui.SidebarPanel.SidebarListener
{
    @Override
    public void onToggleSpectrum() {
        if (mCurrentViewId != null && mCurrentViewId.equals("now_playing")) {
            mNowPlayingSpectrumDisabled = !mNowPlayingSpectrumDisabled;
            mControllerPanel.getNowPlayingPanel().setSpectralPanelVisible(!mNowPlayingSpectrumDisabled);
            if (mNowPlayingSpectrumDisabled) {
                mSpectralPanel.stop();
            } else {
                mSpectralPanel.start();
            }
        } else if (mCurrentViewId != null && mCurrentViewId.equals("tuners")) {
            mTunerSpectrumDisabled = !mTunerSpectrumDisabled;
            if (mTunerSpectrumDisabled) {
                mTopContentPanel.remove(mSpectralPanel);
                mSpectralPanel.stop();
            } else {
                mTopContentPanel.add(mSpectralPanel, BorderLayout.CENTER);
                mSpectralPanel.start();
            }
        }
        mMainContentPanel.revalidate();
        mMainContentPanel.repaint();
    }

    @Override
    public void onToggleDetails() {
        toggleNowPlayingDetailsPanelVisibility();
        javax.swing.SwingUtilities.invokeLater(() -> {
            mMainContentPanel.revalidate();
            mMainContentPanel.repaint();
        });
    }

    @Override
    public void onToggleStreaming() {
        toggleBroadcastStatusPanelVisibility();
        javax.swing.SwingUtilities.invokeLater(() -> {
            mMainContentPanel.revalidate();
            mMainContentPanel.repaint();
        });
    }

    @Override
    public void onToggleResource() {
        toggleResourceStatusPanelVisibility();
        mMainContentPanel.revalidate();
        mMainContentPanel.repaint();
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

    private AudioRecordingManager mAudioRecordingManager;
    private ChannelAlertMonitor mChannelAlertMonitor;
    private AudioStreamingManager mAudioStreamingManager;
    private BroadcastStatusPanel mBroadcastStatusPanel;
    private ControllerPanel mControllerPanel;
    private DiagnosticMonitor mDiagnosticMonitor;
    private IconModel mIconModel = new IconModel();
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private SpectralDisplayPanel mSpectralPanel;
    private JPanel mMainContentPanel;
    private JPanel mTopContentPanel;
    private JavaFxWindowManager mJavaFxWindowManager;
    private io.github.dsheirer.gui.SidebarPanel mSidebarPanel;
    private UserPreferences mUserPreferences = new UserPreferences();
    private TunerManager mTunerManager;
    private ApplicationLog mApplicationLog;
    private TwoToneLog mTwoToneLog;
    private ResourceMonitor mResourceMonitor;
    private Rectangle mNormalBounds;
    private JFXPanel mControllerResourceStatusPanel;
    private JFXPanel mNowPlayingResourceStatusPanel;

    private String mTitle;

    private Stage mPrimaryStage;

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

            //Initialize the GUI
            initGUI();

            BorderPane root = new BorderPane();
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(SDRTrunk.class.getResource("/sdrtrunk_style.css").toExternalForm());
            } catch (Exception e) {
                mLog.error("Could not load stylesheet", e);
            }

            SwingNode swingNode = new SwingNode();
            javax.swing.SwingUtilities.invokeLater(() -> {
                swingNode.setContent(mMainContentPanel);
            });
            root.setCenter(swingNode);


            Dimension dimension = mUserPreferences.getSwingPreference().getDimension(WINDOW_FRAME_IDENTIFIER);
            if(dimension != null)
            {
                primaryStage.setWidth(dimension.getWidth());
                primaryStage.setHeight(dimension.getHeight());
                if(mUserPreferences.getSwingPreference().getMaximized(WINDOW_FRAME_IDENTIFIER, false))
                {
                    primaryStage.setMaximized(true);
                }
            }
            else
            {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                primaryStage.setWidth(screenSize.width * 0.6);
                primaryStage.setHeight(screenSize.height * 0.6);
            }

            Point location = mUserPreferences.getSwingPreference().getLocation(WINDOW_FRAME_IDENTIFIER);
            if(location != null)
            {
                primaryStage.setX(location.getX());
                primaryStage.setY(location.getY());
            }


            primaryStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle((int)primaryStage.getX(), (int)primaryStage.getY(), (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
                }
            });
            primaryStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle((int)primaryStage.getX(), (int)primaryStage.getY(), (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
                }
            });
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle((int)primaryStage.getX(), (int)primaryStage.getY(), (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
                }
            });
            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    mNormalBounds = new Rectangle((int)primaryStage.getX(), (int)primaryStage.getY(), (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
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
            primaryStage.show();

            CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
            final boolean calibrating = !calibrationManager.isCalibrated() &&
                !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

            // Workaround for SwingNode and intermittent page contents not painting initially
            javafx.application.Platform.runLater(() -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (mMainContentPanel != null) {
                        mMainContentPanel.revalidate();
                        mMainContentPanel.repaint();
                    }

                    Platform.runLater(() -> {
                        if(calibrating)
                        {
                            CalibrationDialog calibrationDialog = mJavaFxWindowManager.getCalibrationDialog(mUserPreferences);
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
                });
            });



            try
            {
                TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(mSpectralPanel, mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
                Tuner tuner = tunerSpectralDisplayManager.showFirstTuner();

                if(tuner != null)
                {
                    updateTitle(tuner.getPreferredName());
                }

            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void init() throws Exception
    {
        String operatingSystem = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        ThemeManager themeManager = new ThemeManager();
        if(operatingSystem.contains("mac") || operatingSystem.contains("nux")) {
            try {
                LookAndFeelFactory.installJideExtension();
            } catch(Exception e) {
                mLog.error("Error trying to set LookAndFeelFactory extension for OS [" + operatingSystem + "]");
            }
        }

        if(!GraphicsEnvironment.isHeadless())
        {
        }

        mApplicationLog = new ApplicationLog(mUserPreferences);
        mApplicationLog.start();

        mTwoToneLog = new TwoToneLog(mUserPreferences);
        mTwoToneLog.start();
        UsbMonitorManager.manage(mUserPreferences);
        io.github.dsheirer.gui.WindowsReliabilityManager.manage(mUserPreferences);

        //Note: invoke this early in the application lifecycle, before the TunerManager causes the sdrplay classes
        //to be loaded since the jextract auto-generated code attempts to load the library by name and that can fail
        //when the library was not installed into a normal/default location, particularly on windows OS systems.
        if(SDRPlayLibraryHelper.LOADED)
        {
            mLog.debug("SDRPlay API native library preemptively loaded");
        }

        mResourceMonitor = new ResourceMonitor(mUserPreferences);

        ThreadPool.logSettings();

        //Load properties file
        loadProperties();

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();

        //Register FontAwesome so we can use the fonts in Swing windows
        IconFontSwing.register(FontAwesome.getIconFont());

        mTunerManager = new TunerManager(mUserPreferences);
        mTunerManager.start();

        mSettingsManager = new SettingsManager();

        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, mTunerManager, aliasModel, eventLogManager, mIconModel);

        boolean headless = GraphicsEnvironment.isHeadless();

        mDiagnosticMonitor = new DiagnosticMonitor(mUserPreferences, mPlaylistManager.getChannelProcessingManager(),
                mTunerManager, headless);
        mDiagnosticMonitor.start();


        if(!headless)
        {
            mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, mTunerManager, mPlaylistManager, this::onViewChanged);

            // Add Sidebar
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    mSidebarPanel = new io.github.dsheirer.gui.SidebarPanel(this);
                });
            } catch (Exception e) {
                mLog.error("Error creating sidebar panel", e);
            }
        }


        CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
        final boolean calibrating = !calibrationManager.isCalibrated() &&
            !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

        new ChannelSelectionManager(mPlaylistManager.getChannelModel());

        AudioPlaybackManager audioPlaybackManager = new AudioPlaybackManager(mUserPreferences);

        mAudioRecordingManager = new AudioRecordingManager(mUserPreferences);
        mChannelAlertMonitor = new ChannelAlertMonitor(mPlaylistManager.getChannelModel(), mPlaylistManager.getChannelProcessingManager(), mUserPreferences);
        mAudioRecordingManager.start();
        mChannelAlertMonitor.start();

        mAudioStreamingManager = new AudioStreamingManager(mPlaylistManager.getBroadcastModel(), BroadcastFormat.MP3,
            mUserPreferences);
        mAudioStreamingManager.start();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(mUserPreferences);

        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(duplicateCallDetector);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(audioPlaybackManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioRecordingManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mChannelAlertMonitor);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioStreamingManager);

        MapService mapService = new MapService(aliasModel, mIconModel);
        mPlaylistManager.getChannelProcessingManager().addDecodeEventListener(mapService);

        mNowPlayingDetailsVisible = mPreferences.getBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, true);


        if(!GraphicsEnvironment.isHeadless())
        {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    mControllerPanel = new io.github.dsheirer.controller.ControllerPanel(mPlaylistManager, audioPlaybackManager, mIconModel, mapService,
                            mSettingsManager, mTunerManager, mUserPreferences, mNowPlayingDetailsVisible, this);

                    mControllerPanel.addView("playlist_editor", mJavaFxWindowManager.getView(ViewIdentifier.PLAYLIST_EDITOR));
                    mControllerPanel.addView("user_prefs", mJavaFxWindowManager.getView(ViewIdentifier.USER_PREFERENCES_EDITOR));
                    mControllerPanel.addView("msg_viewer", mJavaFxWindowManager.getView(ViewIdentifier.RECORDING_VIEWER));

                    mControllerPanel.addView("logs", mJavaFxWindowManager.getView(ViewIdentifier.LOGS));

                    mSpectralPanel = new SpectralDisplayPanel(mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
                });
            } catch (Exception e) {
                mLog.error("Error creating controller panel", e);
            }
        }



        TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(mSpectralPanel,
            mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
        mTunerManager.getDiscoveredTunerModel().addListener(tunerSpectralDisplayManager);
        mTunerManager.getDiscoveredTunerModel().addListener(this);

        mPlaylistManager.init();

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
                new ChannelAutoStartFrame(mPlaylistManager.getChannelProcessingManager(), channels, mUserPreferences);
            }
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {

        /**
         * Setup main JFrame window
         */
        mTitle = SystemProperties.getInstance().getApplicationName();

        // The main application window is now JavaFX, not JFrame,
        // so setting the taskbar icon is handled in the JavaFX start() method.


        Dimension dimension = mUserPreferences.getSwingPreference().getDimension(WINDOW_FRAME_IDENTIFIER);

        mSpectralPanel.setPreferredSize(new Dimension(1280, 300));
        mControllerPanel.setPreferredSize(new Dimension(1280, 500));

        if(dimension != null)
        {
            Dimension spectral = mUserPreferences.getSwingPreference().getDimension(SPECTRAL_PANEL_IDENTIFIER);
            if(spectral != null)
            {
                Dimension pref = mSpectralPanel.getPreferredSize();
                mSpectralPanel.setPreferredSize(new Dimension(pref.width, spectral.height));
                // mSpectralPanel.setSize(spectral);
            }

            Dimension controller = mUserPreferences.getSwingPreference().getDimension(CONTROLLER_PANEL_IDENTIFIER);
            if(controller != null)
            {
                Dimension pref = mControllerPanel.getPreferredSize();
                mControllerPanel.setPreferredSize(new Dimension(pref.width, controller.height));
                // mControllerPanel.setSize(controller);
            }


        }
        else
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = (int) (screenSize.width * 0.6);
            int height = (int) (screenSize.height * 0.6);
        }

        Point location = mUserPreferences.getSwingPreference().getLocation(WINDOW_FRAME_IDENTIFIER);
        if(location != null)
        {
        }
        else
        {
        }

        mMainContentPanel = new JPanel(new BorderLayout());

        JPanel contentWithSidebar = new JPanel(new BorderLayout());
        contentWithSidebar.add(mSidebarPanel, BorderLayout.WEST);

        JPanel rightContentPanel = new JPanel(new BorderLayout());

        JPanel rightTopPanel = new JPanel(new BorderLayout());
        rightTopPanel.add(mControllerPanel.getAudioPanel(), BorderLayout.NORTH);

        mTopContentPanel = new JPanel(new BorderLayout());
        mTopContentPanel.add(mSpectralPanel, BorderLayout.CENTER);

        rightTopPanel.add(mTopContentPanel, BorderLayout.CENTER);

        rightContentPanel.add(rightTopPanel, BorderLayout.NORTH);
        rightContentPanel.add(mControllerPanel, BorderLayout.CENTER);

        contentWithSidebar.add(rightContentPanel, BorderLayout.CENTER);
        mMainContentPanel.add(contentWithSidebar, BorderLayout.CENTER);
        mBroadcastStatusVisible = mPreferences.getBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, false);
        mResourceStatusVisible = mPreferences.getBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, true);

        mControllerResourceStatusPanel = mJavaFxWindowManager.createStatusPanel(mResourceMonitor);
        mNowPlayingResourceStatusPanel = mJavaFxWindowManager.createStatusPanel(mResourceMonitor);

        mControllerPanel.getNowPlayingPanel().setComponents(mSpectralPanel, getBroadcastStatusPanel(), mNowPlayingResourceStatusPanel);


        mResourceMonitor.start();
        mControllerPanel.setResourcePanel(mControllerResourceStatusPanel);

            });
        } catch (Exception e) {
            mLog.error("Error creating initGUI", e);
        }
    }


    @Override
    public void stop() throws Exception {
        processShutdown();
    }

    private void processShutdown()
    {
        mLog.info("Application shutdown started ...");
        io.github.dsheirer.gui.WindowsReliabilityManager.stopWatchdog();
        mDiagnosticMonitor.stop();
        if (mPrimaryStage != null) {
            if (!mPrimaryStage.isMaximized() || mNormalBounds == null) {
                mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, new Point((int)mPrimaryStage.getX(), (int)mPrimaryStage.getY()));
                mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, new Dimension((int)mPrimaryStage.getWidth(), (int)mPrimaryStage.getHeight()));
            } else {
                mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, mNormalBounds.getLocation());
                mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, mNormalBounds.getSize());
            }
            mUserPreferences.getSwingPreference().setMaximized(WINDOW_FRAME_IDENTIFIER, mPrimaryStage.isMaximized());
        }
        mUserPreferences.getSwingPreference().setDimension(SPECTRAL_PANEL_IDENTIFIER, mSpectralPanel.getSize());
        mUserPreferences.getSwingPreference().setDimension(CONTROLLER_PANEL_IDENTIFIER, mControllerPanel.getSize());
        mJavaFxWindowManager.shutdown();
        mLog.info("Stopping channels ...");
        mPlaylistManager.getChannelProcessingManager().shutdown();
        mAudioRecordingManager.stop();
        if(mChannelAlertMonitor != null) mChannelAlertMonitor.stop();
        mResourceMonitor.stop();

        mLog.info("Stopping spectral display ...");
        mSpectralPanel.clearTuner();
        mLog.info("Stopping tuners ...");
        mTunerManager.stop();
        mLog.info("Shutdown complete.");
        mApplicationLog.stop();
        mTwoToneLog.stop();
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
            mBroadcastStatusPanel.setPreferredSize(new Dimension(880, 70));
            mBroadcastStatusPanel.setDisable(true);
        }

        return mBroadcastStatusPanel;
    }

    /**
     * Toggles visibility of the broadcast channels status panel at the bottom of the controller panel
     */
    private void toggleBroadcastStatusPanelVisibility() {
        mBroadcastStatusVisible = !mBroadcastStatusVisible;
        mPreferences.putBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, mBroadcastStatusVisible);
        EventQueue.invokeLater(() -> {
            mControllerPanel.getNowPlayingPanel().setBroadcastStatusPanelVisible(mBroadcastStatusVisible);
            mMainContentPanel.revalidate();
        });
    }



    /**
     * Toggles visibility of the resource status panel at the bottom of the main UI window
     */
    private void toggleResourceStatusPanelVisibility() {
        mResourceStatusVisible = !mResourceStatusVisible;
        mPreferences.putBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, mResourceStatusVisible);

        EventQueue.invokeLater(() -> {
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
    public class BroadcastStatusVisibleMenuItem extends JCheckBoxMenuItem
    {
        public BroadcastStatusVisibleMenuItem()
        {
            super("Show Streaming Status");
            setSelected(mBroadcastStatusVisible);
            addActionListener(e -> {
                toggleBroadcastStatusPanelVisibility();
                setSelected(mBroadcastStatusVisible);
            });
        }
    }

    /**
     * Resource status panel visible toggle menu item
     */
    public class ResourceStatusVisibleMenuItem extends JCheckBoxMenuItem
    {
        public ResourceStatusVisibleMenuItem()
        {
            super("Show Resource Status");
            setSelected(mResourceStatusVisible);
            addActionListener(e -> {
                toggleResourceStatusPanelVisibility();
                setSelected(mResourceStatusVisible);
            });
        }
    }

    /**
     * Now Playing channel details visible toggle menu item
     */
    public class NowPlayingChannelDetailsVisibleMenuItem extends JCheckBoxMenuItem
    {
        public NowPlayingChannelDetailsVisibleMenuItem()
        {
            super("Show Now Playing Channel Details");
            setSelected(mNowPlayingDetailsVisible);
            addActionListener(e -> {
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
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("prism.allowhidpi", "true");
        Application.launch(SDRTrunk.class, args);
    }

    public void onViewChanged(String id) {
        if (id == null) return;

        if (mSidebarPanel != null) {
            mSidebarPanel.setActive(id);
        }

        if (id.startsWith("playlist_")) {
            mCurrentViewId = "playlist_editor";
            mTopContentPanel.remove(mSpectralPanel);
            mSpectralPanel.stop();
            mControllerPanel.setResourcePanelVisible(false);
            mControllerPanel.showView("playlist_editor");
            mMainContentPanel.revalidate();
            mMainContentPanel.repaint();
            return;
        }

        mCurrentViewId = id;

        if (id.equals("now_playing")) {
            mTopContentPanel.remove(mSpectralPanel);
            mControllerPanel.setResourcePanelVisible(false);
            mControllerPanel.getNowPlayingPanel().setComponents(mSpectralPanel, getBroadcastStatusPanel(), mNowPlayingResourceStatusPanel);
            mControllerPanel.getNowPlayingPanel().setSpectralPanelVisible(!mNowPlayingSpectrumDisabled);
            if (mNowPlayingSpectrumDisabled) {
                mSpectralPanel.stop();
            } else {
                mSpectralPanel.start();
            }
        } else if (id.equals("tuners")) {
            if (!mTunerSpectrumDisabled) {
                mTopContentPanel.add(mSpectralPanel, BorderLayout.CENTER);
                mSpectralPanel.start();
            } else {
                mTopContentPanel.remove(mSpectralPanel);
                mSpectralPanel.stop();
            }
            mControllerPanel.setResourcePanelVisible(mResourceStatusVisible);
        } else {
            mTopContentPanel.remove(mSpectralPanel);
            mSpectralPanel.stop();
            mControllerPanel.setResourcePanelVisible(false);
        }

        mControllerPanel.showView(id);

        mMainContentPanel.revalidate();
        mMainContentPanel.repaint();
    }

    @Override
    public void onItemSelected(String id) {
        if (id.equals("exit")) {
            processShutdown();
            System.exit(0);
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

}