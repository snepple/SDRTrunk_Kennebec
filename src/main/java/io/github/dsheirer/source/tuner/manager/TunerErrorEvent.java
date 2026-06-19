package io.github.dsheirer.source.tuner.manager;

/**
 * Event broadcast when a tuner enters an error state and begins recovery.  This is posted BEFORE the tuner
 * is stopped so that listeners (e.g. the channel processing manager) can remember which channels were
 * playing on the tuner and restart them once the tuner recovers.
 */
public class TunerErrorEvent
{
    private final DiscoveredTuner mTuner;

    /**
     * Constructs an instance
     * @param tuner that entered an error state
     */
    public TunerErrorEvent(DiscoveredTuner tuner)
    {
        mTuner = tuner;
    }

    /**
     * Discovered tuner that entered an error state
     * @return tuner
     */
    public DiscoveredTuner getTuner()
    {
        return mTuner;
    }
}
