package io.github.dsheirer.channel.metadata;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelMetadataModel implements IChannelMetadataUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMetadataModel.class);

    private ObservableList<ChannelMetadata> mChannelMetadata = FXCollections.observableArrayList();
    private Map<ChannelMetadata,Channel> mMetadataChannelMap = new HashMap();
    private Listener<ChannelAndMetadata> mChannelAddListener;

    public ChannelMetadataModel()
    {
        MyEventBus.getGlobalEventBus().register(this);
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            Platform.runLater(() -> {
                if (!mChannelMetadata.isEmpty()) {
                    ChannelMetadata item = mChannelMetadata.get(0);
                    mChannelMetadata.set(0, item);
                }
            });
        }
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    public int getRow(ChannelMetadata channelMetadata)
    {
        if(mChannelMetadata.contains(channelMetadata))
        {
            return mChannelMetadata.indexOf(channelMetadata);
        }

        return -1;
    }

    public void setChannelAddListener(Listener<ChannelAndMetadata> listener)
    {
        mChannelAddListener = listener;
    }

    public void add(ChannelAndMetadata channelAndMetadata)
    {
        Platform.runLater(() -> {
            for(ChannelMetadata channelMetadata: channelAndMetadata.getChannelMetadata())
            {
                mChannelMetadata.add(channelMetadata);
                mMetadataChannelMap.put(channelMetadata, channelAndMetadata.getChannel());
                channelMetadata.setUpdateEventListener(ChannelMetadataModel.this);
            }

            if(mChannelAddListener != null)
            {
                mChannelAddListener.receive(channelAndMetadata);
            }
        });
    }

    public void updateChannelMetadataToChannelMap(Collection<ChannelMetadata> channelMetadatas, Channel channel)
    {
        for(ChannelMetadata channelMetadata: channelMetadatas)
        {
            mMetadataChannelMap.put(channelMetadata, channel);
        }
    }

    public void remove(ChannelMetadata channelMetadata)
    {
        Platform.runLater(() -> {
            channelMetadata.removeUpdateEventListener();
            mChannelMetadata.remove(channelMetadata);
            mMetadataChannelMap.remove(channelMetadata);
        });
    }

    public ChannelMetadata getChannelMetadata(int row)
    {
        if(row >= 0 && row < mChannelMetadata.size())
        {
            return mChannelMetadata.get(row);
        }

        return null;
    }

    public Channel getChannelFromMetadata(ChannelMetadata channelMetadata)
    {
        return mMetadataChannelMap.get(channelMetadata);
    }

    public ObservableList<ChannelMetadata> getItems() {
        return mChannelMetadata;
    }

    public void updated(ChannelMetadata channelMetadata, ChannelMetadataField field) {
        Platform.runLater(() -> {
            int index = mChannelMetadata.indexOf(channelMetadata);
            if (index >= 0) {
                mChannelMetadata.set(index, channelMetadata);
            }
        });
    }
}
