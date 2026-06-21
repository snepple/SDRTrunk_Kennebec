package io.github.dsheirer.module.decode.nbfm.ai;

/**
 * Event-bus request asking the live NBFM decoder for a given channel to run an AI filter optimization now
 * (a manual run).  Posted by the channel configuration UI; handled by the matching running NBFMDecoder
 * instance, which runs regardless of the auto schedule but still requires the feature enabled and that
 * buffered call audio is available to analyze.
 */
public class NBFMOptimizeRequest
{
    private final String mChannelName;

    public NBFMOptimizeRequest(String channelName)
    {
        mChannelName = channelName;
    }

    public String getChannelName()
    {
        return mChannelName;
    }
}
