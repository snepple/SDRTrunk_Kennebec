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
package io.github.dsheirer.module.decode.analog;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.p25.identifier.channel.StandardChannel;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract analog decoder channel state - provides the minimum channel state functionality
 */
public abstract class AnalogDecoderState extends DecoderState implements ISourceEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AnalogDecoderState.class);
    private Listener<SourceEvent> mSourceEventListener = new SourceEventListener();
    private DecodeEvent mDecodeEvent;
    private IChannelDescriptor mChannelDescriptor = null;
    //Tracks the wall-clock start of the in-progress call and whether its decode event has been published to the
    //event table/streaming yet. Used to enforce getMinCallDurationMs() so sub-second static calls never surface.
    private long mCallStartMs = 0;
    private boolean mEventPublished = false;

    public AnalogDecoderState()
    {
    }

    /**
     * Channel name identifier provided by the subclass implementation.
     */
    protected abstract Identifier getChannelNameIdentifier();

    protected abstract Identifier getTalkgroupIdentifier();

    /**
     * Minimum call duration, in milliseconds, before a call is published as a decode event. Calls shorter than this
     * are discarded so brief static bursts that momentarily open the squelch don't flood the Events table (which in
     * turn evicts real calls before their asynchronous transcripts arrive). Default 0 disables the gate; subclasses
     * override to supply a per-channel configured value.
     */
    protected int getMinCallDurationMs()
    {
        return 0;
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET ->
                    {
                        getIdentifierCollection().update(getChannelNameIdentifier());
                        //Re-assert the configured TO talkgroup so it is always present in the identifier collection -
                        //and therefore inherited by every audio segment - independent of call-event timing. For a
                        //conventional analog channel the talkgroup is a fixed per-channel value, and audio segments
                        //can be created from buffered/tail audio at moments when no call event is mid-flight; without
                        //this, the segment carries no TO talkgroup, no alias matches it, and no stream is assigned.
                        getIdentifierCollection().update(getTalkgroupIdentifier());
                    }
            case START ->
                    {
                        if(event.getState() == State.CALL)
                        {
                            startCallEvent();
                        }
                    }
            case END ->
                    {
                        if(event.getState() == State.CALL)
                        {
                            endCallEvent();
                        }
                    }
            case CONTINUATION ->
                    {
                        if(event.getState() == State.CALL)
                        {
                            continueCallEvent();
                        }
                        else
                        {
                            endCallEvent();
                        }
                    }
        }
    }

    /**
     * Creates/starts a decode call evnet.
     */
    private void startCallEvent()
    {
        getIdentifierCollection().update(getTalkgroupIdentifier());

        if(mDecodeEvent == null)
        {
            mCallStartMs = System.currentTimeMillis();
            mEventPublished = false;
            mDecodeEvent = DecodeEvent.builder(DecodeEventType.CALL, mCallStartMs)
                    .channel(mChannelDescriptor)
                    .details(getDecoderType().name())
                    .identifiers(new IdentifierCollection(getIdentifierCollection().getIdentifiers()))
                    .build();

            //When a minimum call duration is configured, defer publishing the event until the call has lasted long
            //enough. This prevents sub-second static bursts from appearing in the Events table at all. With no
            //minimum (0), the event is published immediately to preserve the original behavior.
            if(getMinCallDurationMs() <= 0)
            {
                mEventPublished = true;
                broadcast(mDecodeEvent);
            }
        }
    }

    /**
     * Continues (or starts) the call decode event and updates the current timestamp
     */
    private void continueCallEvent()
    {
        if(mDecodeEvent == null)
        {
            startCallEvent();
        }

        getIdentifierCollection().update(getTalkgroupIdentifier());
        mDecodeEvent.update(System.currentTimeMillis());

        //Publish a deferred event once it crosses the minimum-duration threshold; thereafter keep updating it.
        if(!mEventPublished && (System.currentTimeMillis() - mCallStartMs) >= getMinCallDurationMs())
        {
            mEventPublished = true;
        }

        if(mEventPublished)
        {
            broadcast(mDecodeEvent);
        }
    }

    /**
     * Ends the call decode event
     */
    private void endCallEvent()
    {
        if(mDecodeEvent != null)
        {
            mDecodeEvent.end(System.currentTimeMillis());

            //Publish the completed event only if it qualified (already published, or it met the minimum duration).
            //A call shorter than the configured minimum that never qualified is discarded silently - it never
            //reaches the Events table or streaming.
            if(mEventPublished || (mDecodeEvent.getTimeEnd() - mCallStartMs) >= getMinCallDurationMs())
            {
                broadcast(mDecodeEvent);
            }

            mDecodeEvent = null;
            mEventPublished = false;
        }

        //Remove any user identifiers from the identifier collection
        resetState();
    }

    @Override
    public void start()
    {
        super.start();
        getIdentifierCollection().update(getChannelNameIdentifier());
        //Seed the configured TO talkgroup at channel start so it is present in the identifier collection (and thus
        //in every audio segment) from the outset, ensuring alias/stream routing works even for the first call and
        //for any audio segment created outside an active call-event window.
        getIdentifierCollection().update(getTalkgroupIdentifier());
    }

    /**
     * Resets temporal state at the end of a call. The base implementation removes all USER-class identifiers, which
     * includes the conventional channel's TO talkgroup. For an analog/conventional channel the talkgroup is a fixed
     * per-channel value (its identity for alias and stream routing), so we re-assert it immediately after the reset.
     * This keeps the talkgroup continuously present in the identifier collection - and therefore inherited by every
     * audio segment, including those created from buffered lead-in or squelch-tail audio between call events - so
     * alias/stream matching is not silently lost. Without this, tone-gated channels (whose audio can be emitted
     * around the edges of a call-event window) produced audio segments with no TO talkgroup, no alias match, and no
     * stream assignment.
     */
    @Override
    protected void resetState()
    {
        super.resetState();
        getIdentifierCollection().update(getTalkgroupIdentifier());
    }

    @Override
    public void init() {}
    @Override
    public void receive(IMessage t) {}

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Monitors source events to capture the channel frequency for use in decode events.
     */
    private class SourceEventListener implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE)
            {
                mChannelDescriptor = new StandardChannel(sourceEvent.getValue().longValue());
            }
        }
    }
}
