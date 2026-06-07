package io.github.dsheirer.module.decode.nxdn.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
import io.github.dsheirer.dsp.gain.NonClippingGain;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NxdnAudioModule extends AmbeAudioModule
{
    private final static Logger mLog = LoggerFactory.getLogger(NxdnAudioModule.class);
    private boolean mEncryptedCall = false;

    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private volatile GraphicEqualizer mGraphicEQ;

    public NxdnAudioModule(UserPreferences userPreferences, AliasList aliasList)
    {
        super(userPreferences, aliasList, 0); // Timeslot 0
    }

    /**
     * Configures the 5-band graphic equalizer for this audio module.
     *
     * @param enabled true to enable the EQ
     * @param bandGains array of 5 gain values in dB (-12 to +12)
     */
    public void setGraphicEQ(boolean enabled, double[] bandGains)
    {
        // AMBE audio is decoded at 8 kHz
        mGraphicEQ = new GraphicEqualizer(8000.0);
        mGraphicEQ.setEnabled(enabled);

        if(bandGains != null)
        {
            mGraphicEQ.setBandGains(bandGains);
        }

        mLog.debug("NxdnAudioModule graphic EQ configured: enabled={} gains={}",
            enabled, bandGains != null ? java.util.Arrays.toString(bandGains) : "null");
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void receive(IMessage message)
    {
        if(hasAudioCodec())
        {
            // Placeholder for NXDN Message Processing
            // In the future, parse NxdnPayloadMessage and extract AMBE frames
        }
    }

    /**
     * Processes an audio packet by decoding the AMBE audio frames and rebroadcasting them as PCM audio packets.
     */
    public void processAudio(byte[] frame)
    {
        if(!mEncryptedCall)
        {
            float[] audio = getAudioCodec().getAudio(frame);
            audio = mGain.apply(audio);

            GraphicEqualizer eq = mGraphicEQ;
            if(eq != null && eq.isEnabled())
            {
                eq.process(audio);
            }

            addAudio(audio);
        }
    }

    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH)
            {
                closeAudioSegment();
                mEncryptedCall = false;
            }
        }
    }
}
