package io.github.dsheirer.controller.channel;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.gui.NotificationManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.NotificationRecipient;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;

public class ChannelAlertMonitor implements Listener<AudioSegment> {
    private static final Logger mLog = LoggerFactory.getLogger(ChannelAlertMonitor.class);

    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;
    private UserPreferences mUserPreferences;

    // Global last audio time for fallback
    private long mGlobalLastAudioTime = System.currentTimeMillis();
    private Map<String, Boolean> mAlertSentMap = new ConcurrentHashMap<>();
    private Map<String, Long> mLastAudioTimeMap = new ConcurrentHashMap<>();

    private ScheduledFuture<?> mMonitorFuture;

    private Map<String, Integer> mConsecutiveAIFailuresMap = new ConcurrentHashMap<>();
    private Map<String, Integer> mLastAIAudioCountMap = new ConcurrentHashMap<>();
    private Map<String, Long> mLastAICheckTimeMap = new ConcurrentHashMap<>();
    private AIAudioMonitorAnalyzer mAiAudioMonitorAnalyzer;

    public ChannelAlertMonitor(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager, UserPreferences userPreferences) {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mUserPreferences = userPreferences;
        mAiAudioMonitorAnalyzer = new AIAudioMonitorAnalyzer(userPreferences);
    }

    public void start() {
        if (mMonitorFuture == null) {
            // Check every 1 minute
            mMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkChannels, 1, 1, TimeUnit.MINUTES);
            mLog.info("Started ChannelAlertMonitor");
        }
    }

    public void stop() {
        if (mMonitorFuture != null) {
            mMonitorFuture.cancel(false);
            mMonitorFuture = null;
        }
    }

    @Override
    public void receive(AudioSegment audioSegment) {
        if (audioSegment != null) {
            long currentTime = System.currentTimeMillis();
            mGlobalLastAudioTime = currentTime;

            // If the audio segment has an alias list, we might be able to match it to a channel
            if(audioSegment.getAliasList() != null) {
                String aliasListName = audioSegment.getAliasList().getName();
                if(aliasListName != null) {
                    for(Channel channel : mChannelModel.getChannels()) {
                        if(aliasListName.equals(channel.getAliasListName()) && mChannelProcessingManager.isProcessing(channel)) {
                            mLastAudioTimeMap.put(channel.getName(), currentTime);
                            mAlertSentMap.put(channel.getName(), false);
                        }
                    }
                }
            }
        }
    }

    private void checkChannels() {
        long currentTime = System.currentTimeMillis();

        for (Channel channel : mChannelModel.getChannels()) {
            if (!mChannelProcessingManager.isProcessing(channel)) {
                // Ignore channels that are not currently processing (playing)
                // If it was processing before and stopped, reset its state so it doesn't alert immediately when restarted
                if (mLastAudioTimeMap.containsKey(channel.getName())) {
                     mLastAudioTimeMap.put(channel.getName(), currentTime);
                }
                continue;
            }

            ChannelAlertConfiguration alertConfig = channel.getAlertConfiguration();
            if (alertConfig != null) {
                if (alertConfig.isAiAudioMonitoringEnabled()) {
                    doAiMonitoringCheck(channel, currentTime);
                }

                if (alertConfig.isInactivityAlertEnabled()) {

                String channelName = channel.getName();

                // If we've never heard activity but it's processing, track it starting now
                if (!mLastAudioTimeMap.containsKey(channelName)) {
                    mLastAudioTimeMap.put(channelName, currentTime);
                }

                long lastActivity = mLastAudioTimeMap.getOrDefault(channelName, currentTime);
                long inactivityMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime - lastActivity);

                if (inactivityMinutes >= alertConfig.getInactivityDurationThresholdMinutes()) {
                    boolean alertSent = mAlertSentMap.getOrDefault(channelName, false);
                    if (!alertSent) {
                        sendInactivityAlert(channel, inactivityMinutes);
                        mAlertSentMap.put(channelName, true);
                    }
                }
                }
            }
        }
    }

    private void sendInactivityAlert(Channel channel, long inactivityMinutes) {
        mLog.warn("Channel Inactivity Alert Triggered for " + channel.getName() + ": " + inactivityMinutes + " minutes.");

        String title = "Channel Inactivity Alert";
        String message = "Channel '" + channel.getName() + "' has been inactive for " + inactivityMinutes + " minutes.";

        boolean notificationSent = false;

        if(mUserPreferences.getNotificationPreference() != null && mUserPreferences.getNotificationPreference().getRecipients() != null) {
            for (NotificationRecipient recipient : mUserPreferences.getNotificationPreference().getRecipients()) {
                if (recipient.isChannelInactivityEnabled()) {
                    notificationSent = true;
                }
            }
        }

        if (notificationSent) {
            NotificationManager.getInstance().showNotification(title, message, TrayIcon.MessageType.WARNING);
        }
    }


    private void doAiMonitoringCheck(Channel channel, long currentTime) {
        ChannelAlertConfiguration alertConfig = channel.getAlertConfiguration();
        String channelName = channel.getName();
        long lastCheckTime = mLastAICheckTimeMap.getOrDefault(channelName, 0L);
        long hoursSinceLastCheck = TimeUnit.MILLISECONDS.toHours(currentTime - lastCheckTime);

        if (lastCheckTime == 0L || hoursSinceLastCheck >= alertConfig.getAiAudioMonitoringCheckInterval()) {
            ThreadPool.CACHED.submit(() -> {
                try {
                    Path dir = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
                    if (!Files.exists(dir)) return;

                    List<Path> channelAudioFiles;
                    try (Stream<Path> paths = Files.list(dir)) {
                        String formattedChannelName = channelName.replace(" ", "_");
                        String searchStr = "_" + formattedChannelName + "_";
                        channelAudioFiles = paths
                                .filter(Files::isRegularFile)
                                .filter(p -> {
                                    String name = p.getFileName().toString().toLowerCase();
                                    return name.endsWith(".wav") || name.endsWith(".mp3");
                                })
                                .filter(p -> p.getFileName().toString().contains(searchStr))
                                .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                                .collect(Collectors.toList());
                    }

                    if (channelAudioFiles.size() < 5) {
                        return; // Wait until at least 5 files exist
                    }

                    if (alertConfig.isAiAudioMonitoringWaitNewAudio() && mConsecutiveAIFailuresMap.getOrDefault(channelName, 0) > 0) {
                        long newFilesCount = channelAudioFiles.stream()
                                .filter(p -> p.toFile().lastModified() > lastCheckTime)
                                .count();
                        if (newFilesCount < 5) {
                            return; // Wait until 5 new files are generated since last check
                        }
                    }

                    List<Path> filesToAnalyze = channelAudioFiles.subList(channelAudioFiles.size() - 5, channelAudioFiles.size());

                    // Update check time immediately so we don't queue multiple
                    mLastAICheckTimeMap.put(channelName, System.currentTimeMillis());

                    boolean functional = mAiAudioMonitorAnalyzer.analyze(filesToAnalyze);

                    if (functional) {
                        mConsecutiveAIFailuresMap.put(channelName, 0);
                    } else {
                        int failures = mConsecutiveAIFailuresMap.getOrDefault(channelName, 0) + 1;
                        mConsecutiveAIFailuresMap.put(channelName, failures);

                        if (failures >= alertConfig.getAiAudioMonitoringAlertThreshold()) {
                            String title = "AI Monitoring Alert";
                            String message = "AI Monitoring Alert: Audio being received on " + channelName + " is unintelligible or lacks clear voice/data.";
                            NotificationManager.getInstance().showNotification(title, message, TrayIcon.MessageType.WARNING);
                            mLog.warn(title + ": " + message);
                            mConsecutiveAIFailuresMap.put(channelName, 0);
                        }
                    }
                } catch (Exception e) {
                    mLog.error("Error during AI Audio Monitoring check for channel " + channelName, e);
                }
            });
        }
    }


}