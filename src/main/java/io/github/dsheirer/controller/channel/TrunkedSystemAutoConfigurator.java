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
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to decoded trunked-system identifier updates for a running channel and auto-fills the channel's
 * System and Site configuration labels when they are empty.
 *
 * This is intentionally non-destructive: it only ever populates labels the user left blank and never
 * overwrites a value the user (or a previous auto-fill) already set, so manual configuration is always
 * respected.  Each label is filled at most once per channel-processing session.
 *
 * Labels are mutated on the JavaFX application thread (they are JavaFX properties), and a configuration
 * change is signalled via the supplied callback so the playlist is persisted.
 */
public class TrunkedSystemAutoConfigurator implements Listener<IdentifierUpdateNotification>
{
    private static final Logger mLog = LoggerFactory.getLogger(TrunkedSystemAutoConfigurator.class);

    private final Channel mChannel;
    private final Runnable mConfigurationChangedCallback;
    private volatile boolean mSystemHandled;
    private volatile boolean mSiteHandled;

    /**
     * Constructs an instance.
     * @param channel whose System/Site labels may be auto-filled.
     * @param configurationChangedCallback invoked (on the JavaFX thread) after a label is filled so the
     * playlist can be persisted.  May be null.
     */
    public TrunkedSystemAutoConfigurator(Channel channel, Runnable configurationChangedCallback)
    {
        mChannel = channel;
        mConfigurationChangedCallback = configurationChangedCallback;

        //Don't auto-fill a label the user has already populated.
        mSystemHandled = !isEmpty(channel.getSystem());
        mSiteHandled = !isEmpty(channel.getSite());
    }

    @Override
    public void receive(IdentifierUpdateNotification notification)
    {
        if((mSystemHandled && mSiteHandled) || notification == null)
        {
            return;
        }

        Identifier identifier = notification.getIdentifier();

        if(identifier == null || identifier.getIdentifierClass() != IdentifierClass.NETWORK)
        {
            return;
        }

        Form form = identifier.getForm();

        if(form == Form.SYSTEM && !mSystemHandled)
        {
            mSystemHandled = true;
            applyLabel(true, identifier.toString());
        }
        else if(form == Form.SITE && !mSiteHandled)
        {
            mSiteHandled = true;
            applyLabel(false, identifier.toString());
        }
    }

    /**
     * Fills the System or Site label on the JavaFX thread if it is still empty.
     * @param system true for the System label, false for the Site label.
     * @param value decoded identifier text.
     */
    private void applyLabel(boolean system, String value)
    {
        if(isEmpty(value))
        {
            return;
        }

        final String trimmed = value.trim();

        Platform.runLater(() -> {
            boolean changed = false;

            if(system)
            {
                if(isEmpty(mChannel.getSystem()))
                {
                    mChannel.setSystem(trimmed);
                    changed = true;
                }
            }
            else if(isEmpty(mChannel.getSite()))
            {
                mChannel.setSite(trimmed);
                changed = true;
            }

            if(changed)
            {
                mLog.info("Auto-filled {} label '{}' for channel '{}' from decoded trunked-system identifier",
                        system ? "System" : "Site", trimmed, mChannel.getName());

                if(mConfigurationChangedCallback != null)
                {
                    mConfigurationChangedCallback.run();
                }
            }
        });
    }

    private static boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
