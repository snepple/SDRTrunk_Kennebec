package io.github.dsheirer.controller.channel;

/**
 * Structured Gemini classification result for a single radio recording.
 * Mirrors the telemetry daemon's Pydantic schema for type-safe parsing.
 */
public class RadioCallValidation
{
    private boolean mValidTransmission;
    private boolean mContainsHumanSpeech;
    private String mAudioAcousticProfile = "silence";
    private double mConfidenceScore;
    private String mTranscriptSummary = "";

    public RadioCallValidation()
    {
    }

    public RadioCallValidation(boolean validTransmission, boolean containsHumanSpeech, String audioAcousticProfile,
                               double confidenceScore, String transcriptSummary)
    {
        mValidTransmission = validTransmission;
        mContainsHumanSpeech = containsHumanSpeech;
        mAudioAcousticProfile = audioAcousticProfile;
        mConfidenceScore = confidenceScore;
        mTranscriptSummary = transcriptSummary != null ? transcriptSummary : "";
    }

    public boolean isValidTransmission()
    {
        return mValidTransmission;
    }

    public void setValidTransmission(boolean validTransmission)
    {
        mValidTransmission = validTransmission;
    }

    public boolean isContainsHumanSpeech()
    {
        return mContainsHumanSpeech;
    }

    public void setContainsHumanSpeech(boolean containsHumanSpeech)
    {
        mContainsHumanSpeech = containsHumanSpeech;
    }

    public String getAudioAcousticProfile()
    {
        return mAudioAcousticProfile;
    }

    public void setAudioAcousticProfile(String audioAcousticProfile)
    {
        mAudioAcousticProfile = audioAcousticProfile;
    }

    public double getConfidenceScore()
    {
        return mConfidenceScore;
    }

    public void setConfidenceScore(double confidenceScore)
    {
        mConfidenceScore = confidenceScore;
    }

    public String getTranscriptSummary()
    {
        return mTranscriptSummary;
    }

    public void setTranscriptSummary(String transcriptSummary)
    {
        mTranscriptSummary = transcriptSummary;
    }
}
