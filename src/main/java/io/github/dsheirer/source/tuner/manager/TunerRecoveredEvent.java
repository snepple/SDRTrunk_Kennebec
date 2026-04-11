package io.github.dsheirer.source.tuner.manager;

/**
 * Event broadcasted when a tuner successfully recovers from an error.
 */
public class TunerRecoveredEvent
{
    private DiscoveredTuner mTuner;

    /**
     * Constructs an instance
     * @param tuner that recovered
     */
    public TunerRecoveredEvent(DiscoveredTuner tuner)
    {
        mTuner = tuner;
    }

    /**
     * Discovered tuner that recovered
     * @return tuner
     */
    public DiscoveredTuner getTuner()
    {
        return mTuner;
    }
}
