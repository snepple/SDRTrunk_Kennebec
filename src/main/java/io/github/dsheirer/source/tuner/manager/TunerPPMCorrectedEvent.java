package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.tuner.TunerController;

/**
 * Event broadcast when automatic frequency error correction applies a new PPM value to a tuner,
 * allowing the converged value to be persisted to the tuner configuration so the tuner starts
 * pre-corrected on the next application launch.
 */
public class TunerPPMCorrectedEvent
{
    private final TunerController mTunerController;

    public TunerPPMCorrectedEvent(TunerController tunerController)
    {
        mTunerController = tunerController;
    }

    public TunerController getTunerController()
    {
        return mTunerController;
    }
}
