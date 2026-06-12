/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.monitor;

import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallBroadcaster;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzBroadcaster;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzConfiguration;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodically validates streaming service credentials (Broadcastify Calls, OpenMHz) so that an
 * expired or revoked API key is surfaced as an alert before listeners notice a dead feed, rather
 * than failing silently at the next upload.
 *
 * Runs shortly after startup and then daily.  Alerts once per configuration until the credential
 * tests good again.
 */
public class StreamingCredentialPreflight
{
    private static final Logger mLog = LoggerFactory.getLogger(StreamingCredentialPreflight.class);
    private static final long INITIAL_DELAY_MINUTES = 5;
    private static final long CHECK_INTERVAL_HOURS = 24;

    private final BroadcastModel mBroadcastModel;
    private ScheduledFuture<?> mCheckFuture;
    private final Set<String> mAlertedConfigurations = new HashSet<>();

    public StreamingCredentialPreflight(BroadcastModel broadcastModel)
    {
        mBroadcastModel = broadcastModel;
    }

    public void start()
    {
        if(mCheckFuture == null)
        {
            mCheckFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkAll, INITIAL_DELAY_MINUTES,
                CHECK_INTERVAL_HOURS * 60, TimeUnit.MINUTES);
            mLog.info("Streaming credential pre-flight checks scheduled (startup + daily)");
        }
    }

    public void stop()
    {
        if(mCheckFuture != null)
        {
            mCheckFuture.cancel(false);
            mCheckFuture = null;
        }
    }

    private void checkAll()
    {
        try
        {
            for(BroadcastConfiguration configuration : mBroadcastModel.getBroadcastConfigurations())
            {
                if(configuration.isEnabled())
                {
                    check(configuration);
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error during streaming credential pre-flight checks", e);
        }
    }

    private void check(BroadcastConfiguration configuration)
    {
        String failure = null;

        try
        {
            if(configuration instanceof BroadcastifyCallConfiguration broadcastify)
            {
                String response = BroadcastifyCallBroadcaster.testConnection(broadcastify);

                if(response == null || !response.toLowerCase().startsWith("ok"))
                {
                    failure = response;
                }
            }
            else if(configuration instanceof OpenMHzConfiguration openMHz)
            {
                String response = OpenMHzBroadcaster.testConnection(openMHz);

                if(!"OK".equals(response))
                {
                    failure = response;
                }
            }
            else
            {
                //Other stream types don't expose a credential test - skip
                return;
            }
        }
        catch(Exception e)
        {
            failure = e.getMessage();
        }

        String name = configuration.getName();

        if(failure != null)
        {
            mLog.warn("Streaming credential pre-flight FAILED for [" + name + "] - " + failure);

            if(mAlertedConfigurations.add(name))
            {
                MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                    SystemHealthAlertEvent.AlertType.INTEGRATION,
                    "Streaming Credential Check Failed",
                    "Credential test for stream '" + name + "' failed [" + failure +
                        "] - the stream may stop working.  Verify the API key/system ID in the streaming editor."));
            }
        }
        else
        {
            if(mAlertedConfigurations.remove(name))
            {
                mLog.info("Streaming credential pre-flight recovered for [" + name + "]");
            }
        }
    }
}
