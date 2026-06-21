package io.github.dsheirer.module.decode.nbfm.ai;

/**
 * Event-bus status update for an NBFM filter optimization run so the UI can show progress and the result.
 * {@link #getState()} is one of STARTED, COMPLETED, SKIPPED or FAILED; {@link #getMessage()} is a short
 * human-readable detail (e.g. the AI's improvements summary, or why the run was skipped/failed).
 */
public class NBFMOptimizeStatus
{
    public static final String STARTED = "STARTED";
    public static final String COMPLETED = "COMPLETED";
    public static final String SKIPPED = "SKIPPED";
    public static final String FAILED = "FAILED";

    private final String mChannelName;
    private final String mState;
    private final String mMessage;

    public NBFMOptimizeStatus(String channelName, String state, String message)
    {
        mChannelName = channelName;
        mState = state;
        mMessage = message;
    }

    public String getChannelName()
    {
        return mChannelName;
    }

    public String getState()
    {
        return mState;
    }

    public String getMessage()
    {
        return mMessage;
    }
}
