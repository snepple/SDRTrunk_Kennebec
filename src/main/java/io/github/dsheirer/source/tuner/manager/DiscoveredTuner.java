/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.eventbus.MyEventBus;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.dsheirer.util.ThreadPool;

import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A discovered tuner that may be accessible for use.
 */
public abstract class DiscoveredTuner implements ITunerErrorListener
{
    private Logger mLog = LoggerFactory.getLogger(DiscoveredTuner.class);
    private volatile TunerStatus mTunerStatus = TunerStatus.ENABLED;
    private boolean mEnabled = true;
    private String mErrorMessage;
    private List<IDiscoveredTunerStatusListener> mListeners = new CopyOnWriteArrayList();
    protected Tuner mTuner;
    protected TunerConfiguration mTunerConfiguration;
    private volatile ScheduledFuture<?> mRecoveryTask;
    private AtomicInteger mRecoveryAttempts = new AtomicInteger(0);
    //Guards recovery scheduling so two errors arriving on different threads in the same instant cannot each
    //schedule a recovery task (the earlier was orphaned and kept restarting channels every 3 minutes forever).
    private final Object mRecoveryLock = new Object();

    /**
     * Tuner Class
     */
    public abstract TunerClass getTunerClass();

    /**
     * Current status of the discovered tuner
     */
    public TunerStatus getTunerStatus()
    {
        return mTunerStatus;
    }

    /**
     * Logs current state of the tuner
     */
    public void logState()
    {
        mLog.info(getDiagnosticReport());
    }

    /**
     * Generates a state report for this tuner.
     * @return
     */
    public String getDiagnosticReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered Tuner: ").append(getId());
        sb.append("\n\tClass:").append(getClass());

        if(hasTuner())
        {
            sb.append("\n\tTuner Class:").append(getTuner().getClass());
            sb.append("\n\tTuner Controller Class:").append(getTuner().getTunerController().getClass());
            sb.append("\n\tFrequency:").append(getTuner().getTunerController().getFrequency());
            sb.append("\n\tError:").append(getErrorMessage());
            sb.append("\n\tChannel Manager Class:").append(getTuner().getChannelSourceManager().getClass());
            sb.append("\n\tChannel Manager:").append(getTuner().getChannelSourceManager().getStateDescription());
        }
        else
        {
            sb.append("\n\tTuner - no tuner");
        }

        return sb.toString();
    }

    /**
     * Sets the status of the discovered tuner and notifies registered listeners of the status change.
     * @param tunerStatus to set
     */
    private void setTunerStatus(TunerStatus tunerStatus)
    {
        setTunerStatus(tunerStatus, true);
    }

    /**
     * Sets the status of the discovered tuner and optionally notifies registered listeners of the status change.
     * @param tunerStatus to set
     * @param notifyListeners true to notify and false to not notify
     */
    private void setTunerStatus(TunerStatus tunerStatus, boolean notifyListeners)
    {
        if(mTunerStatus != tunerStatus)
        {
            TunerStatus previous = mTunerStatus;
            mTunerStatus = tunerStatus;

            if(notifyListeners)
            {
                broadcast(this, previous, mTunerStatus);
            }
        }
    }

    /**
     * Indicates if this discovered tuner is enabled and usable.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Sets the enabled state of this discovered tuner
     */
    public void setEnabled(boolean enabled)
    {
        //If there was a change in state
        if(mEnabled ^ enabled)
        {
            mErrorMessage = null;

            mEnabled = enabled;

            if(mEnabled)
            {
                start();
                setTunerStatus(TunerStatus.ENABLED);
            }
            else
            {
                stop();
                setTunerStatus(TunerStatus.DISABLED);
            }
        }
    }

    /**
     * Indicates if this discovered tuner is available and usable.  Use this method to check if a discovered tuner is
     * available prior to access the tuner directly via the getTuner() method.
     */
    public boolean isAvailable()
    {
        return getTunerStatus().isAvailable();
    }

    /**
     * An identifier for this discovered tuner where this identifier when combined with the discovered tuner type
     * is a globally unique value.
     *
     * Note: this identifier will be used to persist the enabled/disabled state of each discoverable tuner.  Therefore,
     * this identifier must be consistent across application run cycles in order to correctly manage disabled tuners.
     * @return globally unique string identifier for the discovered tuner.
     */
    public abstract String getId();

    /**
     * @return the friendly name of this tuner, if configured, or the default id.
     */
    public String getName()
    {
        if(hasTunerConfiguration() && getTunerConfiguration().getFriendlyName() != null && !getTunerConfiguration().getFriendlyName().trim().isEmpty())
        {
            return getTunerConfiguration().getFriendlyName().trim();
        }

        return getId();
    }

    /**
     * Access a started and initialized.
     *
     * Use the isAvailable() method to check if this tuner is available prior to invoking this method to avoid a
     * null tuner instance.
     *
     * @return started and initialized tuner
     */
    public Tuner getTuner()
    {
        return mTuner;
    }

    /**
     * Indicates if this discovered tuner is started and has a fully constructed tuner instance
     */
    public boolean hasTuner()
    {
        return getTuner() != null;
    }

    /**
     * Tuner configuration for this tuner
     */
    public TunerConfiguration getTunerConfiguration()
    {
        return mTunerConfiguration;
    }

    /**
     * Sets the tuner configuration for this tuner.
     */
    public void setTunerConfiguration(TunerConfiguration tunerConfiguration)
    {
        mTunerConfiguration = tunerConfiguration;

        if(hasTuner())
        {
            try
            {
                getTuner().getTunerController().apply(mTunerConfiguration);
            }
            catch(SourceException se)
            {
                mLog.error("Error applying tuner configuration [" + mTunerConfiguration.getClass() +
                        "] to discovered tuner [" + getId() + "}", se);
            }
        }
    }

    /**
     * Indicates if this discovered tuner has a tuner configuration
     */
    public boolean hasTunerConfiguration()
    {
        return mTunerConfiguration != null;
    }

    /**
     * Adds a tuner status change listener to monitor this discovered tuner for changes in status.
     */
    public void addTunerStatusListener(IDiscoveredTunerStatusListener listener)
    {
        if(!mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a tuner status change listener from monitoring this discovered tuner for changes in status.
     */
    public void removeTunerStatusListener(IDiscoveredTunerStatusListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Broadcasts the tuner status change to all registered listeners.
     * @param tuner that was changed
     * @param previous tuner status
     * @param current tuner status
     */
    private void broadcast(DiscoveredTuner tuner, TunerStatus previous, TunerStatus current)
    {
        for(IDiscoveredTunerStatusListener listener: mListeners)
        {
            listener.tunerStatusUpdated(tuner, previous, current);
        }
    }

    /**
     * Sets this tuner to an error state and applies the error message
     * @param errorMessage to set
     */
    @Override
    public void setErrorMessage(String errorMessage)
    {
        if(errorMessage == null)
        {
            mErrorMessage = null;
            return;
        }

        //Serialize the whole error-handling decision.  Without this lock, two errors arriving on different
        //threads in the same instant (e.g. "Buffers Exhausted" and "Tuner Stalled" reported by separate
        //channels when the tuner hiccups) could both pass the "already recovering?" check and each schedule a
        //fixed-rate recovery task.  Only the last was tracked by mRecoveryTask; the other became an orphan that
        //fired every 3 minutes forever, "recovering" an already-healthy tuner and restarting every channel.
        synchronized(mRecoveryLock)
        {
            //If a recovery task is already active, just record the latest error.  The active recovery task owns
            //the retry schedule, and a re-entrant error from a failed restart attempt must not schedule another.
            if (mRecoveryTask != null && !mRecoveryTask.isDone()) {
                mErrorMessage = errorMessage;
                return;
            }

            mErrorMessage = errorMessage;
            //Notify listeners (e.g. ChannelProcessingManager via PlaylistManager) BEFORE stopping the tuner, so
            //the channels currently playing on it can be remembered and automatically restarted on recovery.
            MyEventBus.getGlobalEventBus().post(new TunerErrorEvent(this));
            stop();
            setTunerStatus(TunerStatus.RECOVERING);
            mRecoveryAttempts.set(0);

            if ("USB Error - Device Disconnected".equals(errorMessage)) {
                mLog.info("Tuner Error - Device Disconnected - Initiating Recovery - " + getId());
                mRecoveryTask = ThreadPool.SCHEDULED.schedule(new DisconnectRecoveryRunnable(), 5, TimeUnit.SECONDS);
                return;
            }

            //All other errors (buffer exhaustion, tuner stall, driver/API errors): attempt automatic recovery
            //rather than permanently disabling the tuner.  Restart attempts are cheap, and a transient error
            //should not require a human to restart the application to restore reception.
            mLog.info("Tuner Error - Initiating Recovery - " + getId() + " Error: " + errorMessage);
            mRecoveryTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(new RecoveryRunnable(), 180, 180, TimeUnit.SECONDS);
        }

        if ("USB Error - Transfer Buffers Exhausted".equals(errorMessage) && this instanceof DiscoveredUSBTuner) {
            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.source.tuner.ui.USBAlertEvent(
                "USB bus overload detected. Buffer overruns or dropped samples occurred on this bus. " +
                "Please reduce the sample rate of specific tuners on this overloaded bus or move one or more tuners to a different physical USB port on your computer."
            ));
        }
    }

    @Override
    public void tunerRemoved()
    {
        setTunerStatus(TunerStatus.REMOVED);
    }

    /**
     * Indicates if this tuner has an error message.
     */
    public boolean hasErrorMessage()
    {
        return mErrorMessage != null;
    }

    /**
     * Optional error message.
     * @return error message if there is one, or null if there is not.
     */
    public String getErrorMessage()
    {
        return mErrorMessage;
    }

    /**
     * Fully instantiate and start this discovered tuner to make it usable within the application.  Implementations
     * should attempt to instantiate the tuner and assign it to mTuner variable.  If there is an error, invoke the
     * setErrorMessage() to signal the tuner is unusable.
     */
    public abstract void start();

    /**
     * Attempts to restart a tuner that's currently in an error state
     */
    public void restart()
    {
        if(getTunerStatus() == TunerStatus.ERROR || getTunerStatus() == TunerStatus.RECOVERING)
        {
            mErrorMessage = null;

            if(isEnabled())
            {
                //Change status to enabled so that we can attempt to start, but don't notify listeners yet.
                setTunerStatus(TunerStatus.ENABLED);
                start();
            }
            else
            {
                setTunerStatus(TunerStatus.DISABLED);
            }
        }
    }

    /**
     * Stop this discovered tuner, notify registered listeners/consumers and release any resources that it is using.
     */
    public void stop()
    {
        if(hasTuner())
        {
            mLog.info("Stopping Tuner: " + getId());
            getTuner().stop();
            mTuner = null;
        }
    }

    /**
     * Cancels the active recovery task (if any) under the recovery lock so that cancellation cannot race with a
     * concurrent error scheduling a replacement task.  Clears the reference so the next error starts fresh.
     */
    private void cancelRecoveryTask()
    {
        synchronized(mRecoveryLock)
        {
            if(mRecoveryTask != null)
            {
                mRecoveryTask.cancel(false);
                mRecoveryTask = null;
            }
        }
    }

    /**
     * Periodic tuner recovery task.  Attempts a restart every 3 minutes.  After 5 failed attempts it slows to a
     * 10-minute retry cadence and posts a system health alert, but never permanently gives up - an unattended
     * system should continue trying to restore reception indefinitely.
     */
    private class RecoveryRunnable implements Runnable {
        private static final int SLOW_MODE_ATTEMPT_THRESHOLD = 5;
        private static final long SLOW_MODE_INTERVAL_SECONDS = 600;

        @Override
        public void run() {
            TunerStatus status = getTunerStatus();

            //Stop recovery if the tuner was removed or deliberately disabled
            if (status == TunerStatus.REMOVED || status == TunerStatus.DISABLED) {
                cancelRecoveryTask();
                return;
            }

            //If the tuner is already enabled at entry, this task has nothing to recover - it is a stale/orphaned
            //schedule.  Cancel it quietly and, crucially, do NOT post a recovered event (which would restart
            //every channel on a perfectly healthy tuner).
            if (status == TunerStatus.ENABLED) {
                cancelRecoveryTask();
                return;
            }

            int attempt = mRecoveryAttempts.incrementAndGet();
            mLog.info("Attempting tuner recovery for " + getId() + " - Attempt " + attempt);

            try {
                restart();

                if (getTunerStatus() == TunerStatus.ENABLED) {
                    mLog.info("Successfully recovered tuner " + getId());
                    cancelRecoveryTask();
                    mRecoveryAttempts.set(0);
                    MyEventBus.getGlobalEventBus().post(new TunerRecoveredEvent(DiscoveredTuner.this));
                } else if (attempt == SLOW_MODE_ATTEMPT_THRESHOLD) {
                    mLog.error("Failed to recover tuner " + getId() + " after " + attempt +
                        " attempts - continuing recovery attempts every " + (SLOW_MODE_INTERVAL_SECONDS / 60) + " minutes.");
                    MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.health.SystemHealthAlertEvent(
                        io.github.dsheirer.health.SystemHealthAlertEvent.AlertType.HARDWARE,
                        "Tuner Recovery Degraded",
                        "Tuner " + getId() + " has failed " + attempt + " recovery attempts [" + getErrorMessage() +
                            "] - recovery will continue every " + (SLOW_MODE_INTERVAL_SECONDS / 60) + " minutes."));

                    synchronized(mRecoveryLock) {
                        if (mRecoveryTask != null) {
                            mRecoveryTask.cancel(false);
                        }
                        mRecoveryTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(this, SLOW_MODE_INTERVAL_SECONDS,
                            SLOW_MODE_INTERVAL_SECONDS, TimeUnit.SECONDS);
                    }
                } else {
                    mLog.warn("Tuner recovery attempt " + attempt + " failed for " + getId());
                }
            } catch (Exception e) {
                mLog.error("Error during tuner recovery attempt " + attempt + " for " + getId(), e);
            }
        }
    }

    /**
     * Device disconnect recovery task.  Retries every 5 seconds for the first 15 minutes, then every 5 minutes
     * until 45 minutes, then every 10 minutes indefinitely - a tuner replugged at any point recovers without
     * requiring an application restart.
     */
    private class DisconnectRecoveryRunnable implements Runnable {
        private long mStartTime = System.currentTimeMillis();
        private boolean mLongOutageNotified = false;

        @Override
        public void run() {
            TunerStatus status = getTunerStatus();

            //Stop recovery if the tuner was removed or deliberately disabled
            if (status == TunerStatus.REMOVED || status == TunerStatus.DISABLED) {
                return;
            }

            //Already enabled at entry means another path recovered the tuner - this chain is stale.  Stop
            //without posting a recovered event so we don't needlessly restart channels on a healthy tuner.
            if (status == TunerStatus.ENABLED) {
                return;
            }

            long elapsedMillis = System.currentTimeMillis() - mStartTime;
            long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);

            mLog.info("Attempting device disconnect recovery for " + getId() + " - Elapsed: " + elapsedMinutes + " minutes");

            try {
                restart();

                if (getTunerStatus() == TunerStatus.ENABLED) {
                    mLog.info("Successfully recovered disconnected tuner " + getId());
                    io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new TunerRecoveredEvent(DiscoveredTuner.this));
                    return;
                }
            } catch (Exception e) {
                mLog.error("Error during disconnect recovery attempt for " + getId(), e);
            }

            if (elapsedMinutes >= 45 && !mLongOutageNotified) {
                mLongOutageNotified = true;
                mLog.error("Failed to recover disconnected tuner " + getId() + " after 45 minutes - " +
                    "recovery will continue every 10 minutes.");
                MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.health.SystemHealthAlertEvent(
                    io.github.dsheirer.health.SystemHealthAlertEvent.AlertType.HARDWARE,
                    "Tuner Disconnected",
                    "Tuner " + getId() + " has been disconnected for over 45 minutes - recovery attempts will " +
                        "continue every 10 minutes."));
            }

            long nextDelay;
            if (elapsedMinutes < 15) {
                nextDelay = 5;        //5 seconds
            } else if (elapsedMinutes < 45) {
                nextDelay = 300;      //5 minutes
            } else {
                nextDelay = 600;      //10 minutes
            }

            mLog.warn("Disconnect recovery failed for " + getId() + " - retrying in " + nextDelay + " seconds");
            mRecoveryTask = ThreadPool.SCHEDULED.schedule(this, nextDelay, TimeUnit.SECONDS);
        }
    }
}
