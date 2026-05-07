/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.controller.channel.map;

import io.github.dsheirer.controller.channel.map.ChannelMapEvent.Event;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.animation.AnimationTimer;
import java.util.concurrent.ConcurrentLinkedQueue;


import java.util.ArrayList;
import java.util.List;

public class ChannelMapModel
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMapModel.class);

    private ObservableList<ChannelMap> mChannelMaps = FXCollections.observableArrayList(ChannelMap.extractor());
    private Broadcaster<ChannelMapEvent> mEventBroadcaster = new Broadcaster<>();
    private ConcurrentLinkedQueue<ChannelMap> mPendingAdds = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<ChannelMap> mPendingRemoves = new ConcurrentLinkedQueue<>();
    private AnimationTimer mBatchUpdater;

    public ChannelMapModel()
    {
        mBatchUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!mPendingAdds.isEmpty() || !mPendingRemoves.isEmpty()) {
                    List<ChannelMap> adds = new ArrayList<>();
                    List<ChannelMap> removes = new ArrayList<>();

                    ChannelMap cm;
                    while ((cm = mPendingAdds.poll()) != null) adds.add(cm);
                    while ((cm = mPendingRemoves.poll()) != null) removes.add(cm);

                    if (!removes.isEmpty()) mChannelMaps.removeAll(removes);
                    if (!adds.isEmpty()) {
                        // Filter out duplicates before adding to the model
                        List<ChannelMap> uniqueAdds = new ArrayList<>();
                        for(ChannelMap add : adds) {
                            if(!mChannelMaps.contains(add) && !uniqueAdds.contains(add)) {
                                uniqueAdds.add(add);
                            }
                        }
                        if(!uniqueAdds.isEmpty()) mChannelMaps.addAll(uniqueAdds);
                    }
                }
            }
        };
        javafx.application.Platform.runLater(() -> mBatchUpdater.start());
    }

    /**
     * Removes all channel maps from this model and broadcasts a remove/delete event for each.
     */
    public void clear()
    {
        List<ChannelMap> channelMaps = new ArrayList<>(mChannelMaps);

        for(ChannelMap channelMap: channelMaps)
        {
            removeChannelMap(channelMap);
        }
    }

    /**
     * Returns an unmodifiable list of channel maps currently in the model
     */
    public ObservableList<ChannelMap> getChannelMaps()
    {
        return mChannelMaps;
    }

    /**
     * Returns the channel map with a matching name or null
     */
    public ChannelMap getChannelMap(String name)
    {
        for(ChannelMap channelMap : mChannelMaps)
        {
            if(channelMap.getName().equalsIgnoreCase(name))
            {
                return channelMap;
            }
        }

        return null;
    }

    /**
     * Adds a listener to receive notifications when channel map updates occur
     */
    public void addListener(Listener<ChannelMapEvent> listener)
    {
        mEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the channel map update notification listener
     */
    public void removeListener(Listener<ChannelMapEvent> listener)
    {
        mEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts a channel map change.
     *
     * Note: use the add/remove methods to add or remove channel maps from this
     * model.  When using those methods, an add or delete event will automatically
     * be generated.
     */
    public void broadcast(ChannelMapEvent event)
    {
        if(event.getEvent() == ChannelMapEvent.Event.CHANGE ||
            event.getEvent() == ChannelMapEvent.Event.RENAME)
        {
            int index = mChannelMaps.indexOf(event.getChannelMap());

            if(index >= 0)
            {
            }
        }

        mEventBroadcaster.broadcast(event);
    }

    /**
     * Adds a list of channel maps to this model
     */
    public void addChannelMaps(List<ChannelMap> channelMaps)
    {
        for(ChannelMap channelMap : channelMaps)
        {
            addChannelMap(channelMap);
        }
    }

    /**
     * Adds the channel map to this model
     */
    public void addChannelMap(ChannelMap channelMap)
    {
        mPendingAdds.add(channelMap);
        broadcast(new ChannelMapEvent(channelMap, Event.ADD));
    }

    /**
     * Removes the channel map from this model
     */
    public void removeChannelMap(ChannelMap channelMap)
    {
        mPendingRemoves.add(channelMap);
        broadcast(new ChannelMapEvent(channelMap, Event.DELETE));
    }
}
