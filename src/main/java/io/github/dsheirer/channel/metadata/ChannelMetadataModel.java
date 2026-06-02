package io.github.dsheirer.channel.metadata;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.sample.Listener;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

public class ChannelMetadataModel
{
    private ObservableList<ChannelMetadata> mChannelMetadata = FXCollections.observableArrayList();
    private ConcurrentHashMap<ChannelMetadata, Channel> mMetadataChannelMap = new ConcurrentHashMap<>();
    private Listener<ChannelAndMetadata> mChannelAddListener;

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
                // channelMetadata.setUpdateEventListener(this);
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

    public int getRow(ChannelMetadata channelMetadata)
    {
        return mChannelMetadata.indexOf(channelMetadata);
    }

    public Channel getChannelFromMetadata(ChannelMetadata channelMetadata)
    {
        return mMetadataChannelMap.get(channelMetadata);
    }

    public ObservableList<ChannelMetadata> getObservableList()
    {
        return mChannelMetadata;
    }

    public void updated(ChannelMetadata channelMetadata, ChannelMetadataField channelMetadataField)
    {
        // Update handling for UI
    }
}
