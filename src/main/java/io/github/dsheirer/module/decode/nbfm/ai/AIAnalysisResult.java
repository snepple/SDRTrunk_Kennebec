package io.github.dsheirer.module.decode.nbfm.ai;

public class AIAnalysisResult {
    private String issuesFound;
    private String improvements;
    private String explanation;

    private boolean hissReductionEnabled;
    private float hissReductionDb;
    private double hissReductionCorner;
    private boolean lowPassEnabled;
    private double lowPassCutoff;
    private boolean deemphasisEnabled;
    private boolean bassBoostEnabled;
    private float bassBoostDb;
    private boolean agcEnabled;
    private float agcTargetLevel;
    private boolean noiseGateEnabled;
    private float noiseGateThreshold;
    private float noiseGateReduction;
    private float agcMaxGain;

    private boolean squelchTailRemovalEnabled;
    private int squelchTailRemovalMs;
    private int squelchHeadRemovalMs;
    private int noiseGateHoldTime;

    public String getIssuesFound() { return issuesFound; }
    public void setIssuesFound(String issuesFound) { this.issuesFound = issuesFound; }

    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public boolean isHissReductionEnabled() { return hissReductionEnabled; }
    public void setHissReductionEnabled(boolean hissReductionEnabled) { this.hissReductionEnabled = hissReductionEnabled; }

    public float getHissReductionDb() { return hissReductionDb; }
    public void setHissReductionDb(float hissReductionDb) { this.hissReductionDb = hissReductionDb; }

    public double getHissReductionCorner() { return hissReductionCorner; }
    public void setHissReductionCorner(double hissReductionCorner) { this.hissReductionCorner = hissReductionCorner; }

    public boolean isLowPassEnabled() { return lowPassEnabled; }
    public void setLowPassEnabled(boolean lowPassEnabled) { this.lowPassEnabled = lowPassEnabled; }

    public double getLowPassCutoff() { return lowPassCutoff; }
    public void setLowPassCutoff(double lowPassCutoff) { this.lowPassCutoff = lowPassCutoff; }

    public boolean isDeemphasisEnabled() { return deemphasisEnabled; }
    public void setDeemphasisEnabled(boolean deemphasisEnabled) { this.deemphasisEnabled = deemphasisEnabled; }

    public boolean isBassBoostEnabled() { return bassBoostEnabled; }
    public void setBassBoostEnabled(boolean bassBoostEnabled) { this.bassBoostEnabled = bassBoostEnabled; }

    public float getBassBoostDb() { return bassBoostDb; }
    public void setBassBoostDb(float bassBoostDb) { this.bassBoostDb = bassBoostDb; }

    public boolean isAgcEnabled() { return agcEnabled; }
    public void setAgcEnabled(boolean agcEnabled) { this.agcEnabled = agcEnabled; }

    public float getAgcTargetLevel() { return agcTargetLevel; }
    public void setAgcTargetLevel(float agcTargetLevel) { this.agcTargetLevel = agcTargetLevel; }

    public boolean isNoiseGateEnabled() { return noiseGateEnabled; }
    public void setNoiseGateEnabled(boolean noiseGateEnabled) { this.noiseGateEnabled = noiseGateEnabled; }

    public float getNoiseGateThreshold() { return noiseGateThreshold; }
    public void setNoiseGateThreshold(float noiseGateThreshold) { this.noiseGateThreshold = noiseGateThreshold; }

    public float getNoiseGateReduction() { return noiseGateReduction; }
    public void setNoiseGateReduction(float noiseGateReduction) { this.noiseGateReduction = noiseGateReduction; }

    public float getAgcMaxGain() { return agcMaxGain; }
    public void setAgcMaxGain(float agcMaxGain) { this.agcMaxGain = agcMaxGain; }

    public boolean isSquelchTailRemovalEnabled() { return squelchTailRemovalEnabled; }
    public void setSquelchTailRemovalEnabled(boolean squelchTailRemovalEnabled) { this.squelchTailRemovalEnabled = squelchTailRemovalEnabled; }

    public int getSquelchTailRemovalMs() { return squelchTailRemovalMs; }
    public void setSquelchTailRemovalMs(int squelchTailRemovalMs) { this.squelchTailRemovalMs = squelchTailRemovalMs; }

    public int getSquelchHeadRemovalMs() { return squelchHeadRemovalMs; }
    public void setSquelchHeadRemovalMs(int squelchHeadRemovalMs) { this.squelchHeadRemovalMs = squelchHeadRemovalMs; }

    public int getNoiseGateHoldTime() { return noiseGateHoldTime; }
    public void setNoiseGateHoldTime(int noiseGateHoldTime) { this.noiseGateHoldTime = noiseGateHoldTime; }
}
