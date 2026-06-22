package io.github.dsheirer.controller.channel;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.gui.NotificationManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.AlertCategory;
import io.github.dsheirer.preference.notification.SelfHealingOrchestrator;
import io.github.dsheirer.preference.notification.SystemAlert;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChannelAlertMonitor implements Listener<AudioSegment>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelAlertMonitor.class);

    /** Grace period after threshold breach before escalating to alert. */
    private static final long GRACE_PERIOD_MS = TimeUnit.MINUTES.toMillis(2);

    /** Observation window after auto-restart before escalating. */
    private static final long RESTART_OBSERVATION_MS = TimeUnit.MINUTES.toMillis(5);

    /** Consecutive valid AI verifications required before recovery notification (hysteresis). */
    private static final int AI_RECOVERY_VALID_THRESHOLD = 2;

    /** Maximum automatic channel restarts per inactivity episode. */
    private static final int MAX_AUTO_RESTARTS_PER_EPISODE = 2;

    private final ChannelModel mChannelModel;
    private final ChannelProcessingManager mChannelProcessingManager;
    private final UserPreferences mUserPreferences;
    private final AIAudioMonitorAnalyzer mAiAudioMonitorAnalyzer;

    private volatile SelfHealingOrchestrator mOrchestrator;

    private final Map<String, Long> mLastAudioTimeMap = new ConcurrentHashMap<>();
    private final Map<String, InactivityTracker> mInactivityTrackers = new ConcurrentHashMap<>();
    private final Map<String, AIAudioTracker> mAiAudioTrackers = new ConcurrentHashMap<>();

    private ScheduledFuture<?> mMonitorFuture;

    public ChannelAlertMonitor(ChannelModel channelModel, ChannelProcessingManager channelProcessingManager,
                               UserPreferences userPreferences)
    {
        mChannelModel = channelModel;
        mChannelProcessingManager = channelProcessingManager;
        mUserPreferences = userPreferences;
        mAiAudioMonitorAnalyzer = new AIAudioMonitorAnalyzer(userPreferences);
    }

    public void setSelfHealingOrchestrator(SelfHealingOrchestrator orchestrator)
    {
        mOrchestrator = orchestrator;
    }

    public void start()
    {
        if(mMonitorFuture == null)
        {
            mMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkChannels, 1, 1, TimeUnit.MINUTES);
            mLog.info("Started ChannelAlertMonitor");
        }
    }

    public void stop()
    {
        if(mMonitorFuture != null)
        {
            mMonitorFuture.cancel(false);
            mMonitorFuture = null;
        }
    }

    @Override
    public void receive(AudioSegment audioSegment)
    {
        if(audioSegment == null || audioSegment.getAliasList() == null)
        {
            return;
        }

        String aliasListName = audioSegment.getAliasList().getName();
        if(aliasListName == null)
        {
            return;
        }

        long currentTime = System.currentTimeMillis();

        for(Channel channel : mChannelModel.getChannels())
        {
            if(!aliasListName.equals(channel.getAliasListName()) ||
                !mChannelProcessingManager.isProcessing(channel))
            {
                continue;
            }

            String channelName = channel.getName();
            mLastAudioTimeMap.put(channelName, currentTime);

            InactivityTracker tracker = mInactivityTrackers.computeIfAbsent(channelName, k -> new InactivityTracker());
            InactivityMonitorState previousState = tracker.getState();

            if(previousState == InactivityMonitorState.INACTIVE_ALERT)
            {
                tracker.setState(InactivityMonitorState.RECOVERED);
                notifyInactivityRecovered(channel);
            }
            else if(previousState == InactivityMonitorState.PENDING_INACTIVE ||
                previousState == InactivityMonitorState.OBSERVING_RESTART)
            {
                tracker.setState(InactivityMonitorState.NORMAL_OPERATION);
                mLog.info("Channel '{}' recovered during grace/observation period", channelName);
            }
            else
            {
                tracker.setState(InactivityMonitorState.NORMAL_OPERATION);
            }

            tracker.setAutoRestartCount(0);
            tracker.setPendingSince(0);
            tracker.setObservationStarted(0);
        }
    }

    private void checkChannels()
    {
        long currentTime = System.currentTimeMillis();
        List<Channel> inactiveCandidates = new ArrayList<>();

        for(Channel channel : mChannelModel.getChannels())
        {
            if(!mChannelProcessingManager.isProcessing(channel))
            {
                if(mLastAudioTimeMap.containsKey(channel.getName()))
                {
                    mLastAudioTimeMap.put(channel.getName(), currentTime);
                }
                continue;
            }

            ChannelAlertConfiguration alertConfig = channel.getAlertConfiguration();
            if(alertConfig == null)
            {
                continue;
            }

            if(alertConfig.isAiAudioMonitoringEnabled())
            {
                doAiMonitoringCheck(channel, currentTime);
            }

            if(alertConfig.isInactivityAlertEnabled())
            {
                evaluateInactivity(channel, alertConfig, currentTime, inactiveCandidates);
            }
        }

        dispatchCorrelatedInactivityAlerts(inactiveCandidates, currentTime);
    }

    private void evaluateInactivity(Channel channel, ChannelAlertConfiguration alertConfig, long currentTime,
                                    List<Channel> inactiveCandidates)
    {
        String channelName = channel.getName();

        if(!mLastAudioTimeMap.containsKey(channelName))
        {
            mLastAudioTimeMap.put(channelName, currentTime);
        }

        long lastActivity = mLastAudioTimeMap.getOrDefault(channelName, currentTime);
        long elapsedMs = currentTime - lastActivity;
        long thresholdMs = TimeUnit.MINUTES.toMillis(alertConfig.getInactivityDurationThresholdMinutes());

        InactivityTracker tracker = mInactivityTrackers.computeIfAbsent(channelName, k -> new InactivityTracker());

        if(tracker.getState() == InactivityMonitorState.RECOVERED)
        {
            tracker.setState(InactivityMonitorState.NORMAL_OPERATION);
        }

        if(elapsedMs <= thresholdMs)
        {
            if(tracker.getState() == InactivityMonitorState.PENDING_INACTIVE)
            {
                tracker.setState(InactivityMonitorState.NORMAL_OPERATION);
                tracker.setPendingSince(0);
            }
            return;
        }

        switch(tracker.getState())
        {
            case NORMAL_OPERATION:
                tracker.setState(InactivityMonitorState.PENDING_INACTIVE);
                tracker.setPendingSince(currentTime);
                mLog.warn("Channel '{}' exceeded inactivity threshold ({} min) - entering grace period",
                    channelName, TimeUnit.MILLISECONDS.toMinutes(elapsedMs));
                break;

            case PENDING_INACTIVE:
                if(currentTime - tracker.getPendingSince() < GRACE_PERIOD_MS)
                {
                    return;
                }

                if(alertConfig.isInactivityAutoRestartEnabled() &&
                    tracker.getAutoRestartCount() < MAX_AUTO_RESTARTS_PER_EPISODE)
                {
                    performAutoRestart(channel, elapsedMs, tracker);
                }
                else
                {
                    tracker.setState(InactivityMonitorState.INACTIVE_ALERT);
                    inactiveCandidates.add(channel);
                }
                break;

            case OBSERVING_RESTART:
                if(currentTime - tracker.getObservationStarted() < RESTART_OBSERVATION_MS)
                {
                    return;
                }

                if(alertConfig.isInactivityAutoRestartEnabled() &&
                    tracker.getAutoRestartCount() < MAX_AUTO_RESTARTS_PER_EPISODE)
                {
                    performAutoRestart(channel, elapsedMs, tracker);
                }
                else
                {
                    tracker.setState(InactivityMonitorState.INACTIVE_ALERT);
                    inactiveCandidates.add(channel);
                }
                break;

            case INACTIVE_ALERT:
                inactiveCandidates.add(channel);
                break;

            default:
                break;
        }
    }

    private void performAutoRestart(Channel channel, long elapsedMs, InactivityTracker tracker)
    {
        String channelName = channel.getName();
        int attempt = tracker.getAutoRestartCount() + 1;
        tracker.setAutoRestartCount(attempt);
        tracker.setState(InactivityMonitorState.OBSERVING_RESTART);
        tracker.setObservationStarted(System.currentTimeMillis());

        mLog.warn("Channel '{}' inactive for {} minutes - auto-restarting (attempt {} of {})",
            channelName, TimeUnit.MILLISECONDS.toMinutes(elapsedMs), attempt, MAX_AUTO_RESTARTS_PER_EPISODE);

        try
        {
            mChannelProcessingManager.receive(ChannelEvent.requestDisable(channel));
            mChannelProcessingManager.receive(ChannelEvent.requestEnable(channel));

            String message = "Channel '" + channelName + "' was inactive for " +
                TimeUnit.MILLISECONDS.toMinutes(elapsedMs) +
                " minutes and has been automatically restarted (attempt " + attempt + ").";

            NotificationManager.getInstance().showNotification("Channel Auto-Restarted", message,
                TrayIcon.MessageType.INFO);
            dispatchAlert(AlertCategory.CHANNEL_INACTIVITY, message, "CHANNEL_RESTART:" + channelName);
        }
        catch(Exception e)
        {
            mLog.error("Error auto-restarting inactive channel '{}'", channelName, e);
            tracker.setState(InactivityMonitorState.INACTIVE_ALERT);
        }
    }

    private void dispatchCorrelatedInactivityAlerts(List<Channel> inactiveChannels, long currentTime)
    {
        if(inactiveChannels.isEmpty())
        {
            return;
        }

        Map<String, List<Channel>> byTuner = new HashMap<>();
        for(Channel channel : inactiveChannels)
        {
            String tunerKey = channel.activeTunerNameProperty().get();
            if(tunerKey == null || tunerKey.isEmpty())
            {
                tunerKey = "unknown";
            }
            byTuner.computeIfAbsent(tunerKey, k -> new ArrayList<>()).add(channel);
        }

        for(Map.Entry<String, List<Channel>> entry : byTuner.entrySet())
        {
            List<Channel> channels = entry.getValue();
            if(channels.size() == 1)
            {
                Channel channel = channels.get(0);
                long inactivityMinutes = getInactivityMinutes(channel.getName(), currentTime);
                sendInactivityAlert(channel, inactivityMinutes);
            }
            else
            {
                sendCorrelatedInactivityAlert(entry.getKey(), channels, currentTime);
            }
        }
    }

    private long getInactivityMinutes(String channelName, long currentTime)
    {
        long lastActivity = mLastAudioTimeMap.getOrDefault(channelName, currentTime);
        return TimeUnit.MILLISECONDS.toMinutes(currentTime - lastActivity);
    }

    private void sendInactivityAlert(Channel channel, long inactivityMinutes)
    {
        String channelName = channel.getName();
        InactivityTracker tracker = mInactivityTrackers.get(channelName);
        if(tracker != null && tracker.isAlertDispatched())
        {
            return;
        }

        String message = "Channel '" + channelName + "' has been inactive for " + inactivityMinutes +
            " minutes (threshold: " + channel.getAlertConfiguration().getInactivityDurationThresholdMinutes() +
            " minutes).";

        mLog.warn("Channel Inactivity Alert: {}", message);
        NotificationManager.getInstance().showNotification("Channel Inactivity Alert", message,
            TrayIcon.MessageType.WARNING);
        dispatchAlert(AlertCategory.CHANNEL_INACTIVITY, message, "CHANNEL_INACTIVITY:" + channelName);

        if(tracker != null)
        {
            tracker.setAlertDispatched(true);
        }
    }

    private void sendCorrelatedInactivityAlert(String tunerName, List<Channel> channels, long currentTime)
    {
        String signature = "CHANNEL_INACTIVITY_TUNER:" + tunerName;
        InactivityTracker firstTracker = mInactivityTrackers.get(channels.get(0).getName());
        if(firstTracker != null && firstTracker.isAlertDispatched())
        {
            return;
        }

        String channelList = channels.stream().map(Channel::getName).collect(Collectors.joining(", "));
        String message = "Critical Alert: Complete loss of signal on tuner '" + tunerName + "'. " +
            channels.size() + " channels are currently inactive: " + channelList + ".";

        mLog.warn("Correlated Channel Inactivity Alert: {}", message);
        NotificationManager.getInstance().showNotification("Critical Channel Inactivity", message,
            TrayIcon.MessageType.ERROR);
        dispatchAlert(AlertCategory.CHANNEL_INACTIVITY, message, signature);

        for(Channel channel : channels)
        {
            InactivityTracker tracker = mInactivityTrackers.get(channel.getName());
            if(tracker != null)
            {
                tracker.setAlertDispatched(true);
            }
        }
    }

    private void notifyInactivityRecovered(Channel channel)
    {
        String channelName = channel.getName();
        String signature = "CHANNEL_INACTIVITY:" + channelName;
        String message = "Channel '" + channelName + "' has resumed normal activity after inactivity.";

        mLog.info("Channel inactivity recovered: {}", channelName);
        NotificationManager.getInstance().showNotification("Channel Recovered", message, TrayIcon.MessageType.INFO);

        if(mOrchestrator != null)
        {
            mOrchestrator.notifyRecovered(signature);
            String tunerName = channel.activeTunerNameProperty().get();
            if(tunerName != null && !tunerName.isEmpty())
            {
                mOrchestrator.notifyRecovered("CHANNEL_INACTIVITY_TUNER:" + tunerName);
            }
        }

        InactivityTracker tracker = mInactivityTrackers.get(channelName);
        if(tracker != null)
        {
            tracker.setAlertDispatched(false);
        }
    }

    private void doAiMonitoringCheck(Channel channel, long currentTime)
    {
        ChannelAlertConfiguration alertConfig = channel.getAlertConfiguration();
        String channelName = channel.getName();
        AIAudioTracker tracker = mAiAudioTrackers.computeIfAbsent(channelName, k -> new AIAudioTracker());

        long lastCheckTime = tracker.getLastCheckTime();
        long hoursSinceLastCheck = TimeUnit.MILLISECONDS.toHours(currentTime - lastCheckTime);

        if(lastCheckTime != 0 && hoursSinceLastCheck < alertConfig.getAiAudioMonitoringCheckInterval())
        {
            return;
        }

        ThreadPool.CACHED.submit(() -> {
            try
            {
                Path dir = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
                if(!Files.exists(dir))
                {
                    return;
                }

                List<Path> channelAudioFiles = listChannelRecordings(dir, channelName);
                if(channelAudioFiles.isEmpty())
                {
                    return;
                }

                Path fileToAnalyze = selectFileForAnalysis(channelAudioFiles, tracker, alertConfig, lastCheckTime);
                if(fileToAnalyze == null)
                {
                    return;
                }

                tracker.setLastCheckTime(System.currentTimeMillis());
                tracker.setLastAnalyzedFileModified(fileToAnalyze.toFile().lastModified());

                RadioCallValidation validation = mAiAudioMonitorAnalyzer.analyze(fileToAnalyze);

                if(validation.isValidTransmission())
                {
                    handleValidAiVerification(channel, tracker);
                }
                else
                {
                    handleInvalidAiVerification(channel, tracker, validation);
                }
            }
            catch(Exception e)
            {
                mLog.error("Error during AI audio monitoring check for channel {}", channelName, e);
            }
        });
    }

    private List<Path> listChannelRecordings(Path dir, String channelName) throws Exception
    {
        String searchStr = "_" + channelName.replace(" ", "_") + "_";
        try(Stream<Path> paths = Files.list(dir))
        {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return (name.endsWith(".wav") || name.endsWith(".mp3")) &&
                        p.getFileName().toString().contains(searchStr);
                })
                .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .collect(Collectors.toList());
        }
    }

    private Path selectFileForAnalysis(List<Path> channelAudioFiles, AIAudioTracker tracker,
                                         ChannelAlertConfiguration alertConfig, long lastCheckTime)
    {
        Path latest = channelAudioFiles.get(channelAudioFiles.size() - 1);
        long latestModified = latest.toFile().lastModified();

        if(alertConfig.isAiAudioMonitoringWaitNewAudio() && tracker.getConsecutiveFailures() > 0)
        {
            if(latestModified <= tracker.getLastAnalyzedFileModified())
            {
                return null;
            }
        }

        if(lastCheckTime > 0 && latestModified <= lastCheckTime)
        {
            return null;
        }

        return latest;
    }

    private void handleValidAiVerification(Channel channel, AIAudioTracker tracker)
    {
        String channelName = channel.getName();

        if(tracker.getConsecutiveFailures() > 0)
        {
            tracker.incrementConsecutiveValid();
            if(tracker.getConsecutiveValid() >= AI_RECOVERY_VALID_THRESHOLD && tracker.isAlertActive())
            {
                String message = "AI audio monitoring recovered on channel '" + channelName +
                    "' after sustained valid transmissions.";
                mLog.info(message);
                NotificationManager.getInstance().showNotification("AI Audio Recovered", message,
                    TrayIcon.MessageType.INFO);

                if(mOrchestrator != null)
                {
                    mOrchestrator.notifyRecovered("AI_AUDIO:" + channelName);
                }
                tracker.setAlertActive(false);
                tracker.setBackoffLevel(0);
            }
        }
        else
        {
            tracker.setConsecutiveValid(0);
        }

        tracker.setConsecutiveFailures(0);
    }

    private void handleInvalidAiVerification(Channel channel, AIAudioTracker tracker, RadioCallValidation validation)
    {
        String channelName = channel.getName();
        tracker.setConsecutiveValid(0);
        int failures = tracker.getConsecutiveFailures() + 1;
        tracker.setConsecutiveFailures(failures);

        mLog.warn("AI audio verification failed for channel '{}' ({}/{}): profile={}, confidence={}",
            channelName, failures, channel.getAlertConfiguration().getAiAudioMonitoringAlertThreshold(),
            validation.getAudioAcousticProfile(), validation.getConfidenceScore());

        if(failures < channel.getAlertConfiguration().getAiAudioMonitoringAlertThreshold())
        {
            return;
        }

        if(!shouldDispatchAiAlert(tracker))
        {
            mLog.info("AI audio alert suppressed by exponential backoff for channel '{}'", channelName);
            return;
        }

        String message = "AI Monitoring Alert: Channel '" + channelName +
            "' has " + failures + " consecutive invalid recordings (" +
            validation.getAudioAcousticProfile() + "). Audio may be static, noise, or unintelligible.";

        mLog.warn(message);
        NotificationManager.getInstance().showNotification("AI Monitoring Alert", message, TrayIcon.MessageType.WARNING);
        dispatchAlert(AlertCategory.AI_DIAGNOSTICS, message, "AI_AUDIO:" + channelName);

        tracker.setAlertActive(true);
        tracker.incrementBackoffLevel();
    }

    private boolean shouldDispatchAiAlert(AIAudioTracker tracker)
    {
        long baseCooldownMs = TimeUnit.MINUTES.toMillis(15);
        long cooldown = baseCooldownMs * (1L << Math.min(tracker.getBackoffLevel(), 4));
        long now = System.currentTimeMillis();
        if(now - tracker.getLastAlertTime() < cooldown)
        {
            return false;
        }
        tracker.setLastAlertTime(now);
        return true;
    }

    private void dispatchAlert(AlertCategory category, String message, String signature)
    {
        if(mOrchestrator != null)
        {
            mOrchestrator.intercept(new SystemAlert(category, message, signature));
        }
    }

    private static class InactivityTracker
    {
        private InactivityMonitorState mState = InactivityMonitorState.NORMAL_OPERATION;
        private long mPendingSince;
        private long mObservationStarted;
        private int mAutoRestartCount;
        private boolean mAlertDispatched;

        public InactivityMonitorState getState()
        {
            return mState;
        }

        public void setState(InactivityMonitorState state)
        {
            mState = state;
        }

        public long getPendingSince()
        {
            return mPendingSince;
        }

        public void setPendingSince(long pendingSince)
        {
            mPendingSince = pendingSince;
        }

        public long getObservationStarted()
        {
            return mObservationStarted;
        }

        public void setObservationStarted(long observationStarted)
        {
            mObservationStarted = observationStarted;
        }

        public int getAutoRestartCount()
        {
            return mAutoRestartCount;
        }

        public void setAutoRestartCount(int autoRestartCount)
        {
            mAutoRestartCount = autoRestartCount;
        }

        public boolean isAlertDispatched()
        {
            return mAlertDispatched;
        }

        public void setAlertDispatched(boolean alertDispatched)
        {
            mAlertDispatched = alertDispatched;
        }
    }

    private static class AIAudioTracker
    {
        private long mLastCheckTime;
        private long mLastAnalyzedFileModified;
        private int mConsecutiveFailures;
        private int mConsecutiveValid;
        private boolean mAlertActive;
        private long mLastAlertTime;
        private int mBackoffLevel;

        public long getLastCheckTime()
        {
            return mLastCheckTime;
        }

        public void setLastCheckTime(long lastCheckTime)
        {
            mLastCheckTime = lastCheckTime;
        }

        public long getLastAnalyzedFileModified()
        {
            return mLastAnalyzedFileModified;
        }

        public void setLastAnalyzedFileModified(long lastAnalyzedFileModified)
        {
            mLastAnalyzedFileModified = lastAnalyzedFileModified;
        }

        public int getConsecutiveFailures()
        {
            return mConsecutiveFailures;
        }

        public void setConsecutiveFailures(int consecutiveFailures)
        {
            mConsecutiveFailures = consecutiveFailures;
        }

        public int getConsecutiveValid()
        {
            return mConsecutiveValid;
        }

        public void setConsecutiveValid(int consecutiveValid)
        {
            mConsecutiveValid = consecutiveValid;
        }

        public void incrementConsecutiveValid()
        {
            mConsecutiveValid++;
        }

        public boolean isAlertActive()
        {
            return mAlertActive;
        }

        public void setAlertActive(boolean alertActive)
        {
            mAlertActive = alertActive;
        }

        public long getLastAlertTime()
        {
            return mLastAlertTime;
        }

        public void setLastAlertTime(long lastAlertTime)
        {
            mLastAlertTime = lastAlertTime;
        }

        public int getBackoffLevel()
        {
            return mBackoffLevel;
        }

        public void setBackoffLevel(int backoffLevel)
        {
            mBackoffLevel = backoffLevel;
        }

        public void incrementBackoffLevel()
        {
            mBackoffLevel++;
        }
    }
}
