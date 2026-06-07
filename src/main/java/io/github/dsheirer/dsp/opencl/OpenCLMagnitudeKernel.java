package io.github.dsheirer.dsp.opencl;

import com.aparapi.Kernel;

public class OpenCLMagnitudeKernel extends Kernel {

    private float[] iSamples;
    private float[] qSamples;
    private float[] magnitudes;

    public void setInput(float[] iSamples, float[] qSamples, float[] magnitudes) {
        this.iSamples = iSamples;
        this.qSamples = qSamples;
        this.magnitudes = magnitudes;
    }

    @Override
    public void run() {
        int i = getGlobalId(0);
        float iVal = iSamples[i];
        float qVal = qSamples[i];
        magnitudes[i] = (float) Math.sqrt(iVal * iVal + qVal * qVal);
    }
}
