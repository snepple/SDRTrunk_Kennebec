package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.psk.demod.DifferentialDemodulatorFactory;
import io.github.dsheirer.dsp.psk.demod.DifferentialDemodulatorFloat;
import io.github.dsheirer.dsp.squelch.PowerMonitor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.complex.IQImbalanceCorrector;
import io.github.dsheirer.sample.complex.NoiseBlanker;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * NXDN Decoder for C4FM/4FSK signals.
 * Implements decimation, IQ imbalance correction, pulse shaping, baseband filtering, and differential demodulation.
 */
public class NxdnDecoder extends FeedbackDecoder implements IComplexSamplesListener, ISourceEventListener, ISourceEventProvider, Listener<ComplexSamples> {
    private static final Logger mLog = LoggerFactory.getLogger(NxdnDecoder.class);
    
    // NXDN operates at 2400 (6.25kHz) or 4800 (12.5kHz) baud. We'll default to 4800 for now.
    private static final int SYMBOL_RATE = 4800;
    private static final Map<Double, float[]> BASEBAND_FILTERS = new HashMap<>();

    private final PowerMonitor mPowerMonitor = new PowerMonitor();
    private final IQImbalanceCorrector mIQImbalanceCorrector = new IQImbalanceCorrector();
    private final NoiseBlanker mNoiseBlanker = new NoiseBlanker();

    private final NxdnDemodulator mSymbolProcessor;
    private final NxdnMessageFramer mMessageFramer;

    private DifferentialDemodulatorFloat mDemodulator;
    private IRealDecimationFilter mDecimationFilterI;
    private IRealDecimationFilter mDecimationFilterQ;
    private IRealFilter mBasebandFilterI;
    private IRealFilter mBasebandFilterQ;
    private IRealFilter mPulseShapingFilterI;
    private IRealFilter mPulseShapingFilterQ;

    public NxdnDecoder() {
        super();
        mMessageFramer = new NxdnMessageFramer();
        mSymbolProcessor = new NxdnDemodulator(mMessageFramer, this);
    }

    @Override
    public DecoderType getDecoderType() {
        return DecoderType.NXDN;
    }

    @Override
    public String getProtocolDescription() {
        return "NXDN";
    }

    public void setSampleRate(double sampleRate) {
        if(sampleRate <= SYMBOL_RATE * 2) {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " + SYMBOL_RATE + " symbol rate)");
        }

        mPowerMonitor.setSampleRate((int)sampleRate);

        int decimation = 1;

        //Identify decimation that gets us as close to 4.0 Samples Per Symbol as possible (19.2 kHz for 4800 baud)
        while((sampleRate / decimation) >= 38400) {
            decimation *= 2;
        }

        mDecimationFilterI = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mDecimationFilterQ = DecimationFilterFactory.getRealDecimationFilter(decimation);

        float decimatedSampleRate = (float)sampleRate / decimation;
        int symbolLength = 16;
        float rrcAlpha = 0.2f;

        float[] taps = FilterFactory.getRootRaisedCosine(decimatedSampleRate / SYMBOL_RATE, symbolLength, rrcAlpha);
        mPulseShapingFilterI = new RealFIRFilter(taps);
        mPulseShapingFilterQ = new RealFIRFilter(taps);

        mBasebandFilterI = FilterFactory.getRealFilter(getBasebandFilter(decimatedSampleRate));
        mBasebandFilterQ = FilterFactory.getRealFilter(getBasebandFilter(decimatedSampleRate));
        mDemodulator = DifferentialDemodulatorFactory.getFloatDemodulator(decimatedSampleRate, SYMBOL_RATE);
        mSymbolProcessor.setSamplesPerSymbol(mDemodulator.getSamplesPerSymbol());

        mIQImbalanceCorrector.reset();
        mNoiseBlanker.reset();
    }

    @Override
    public void receive(ComplexSamples samples) {
        samples.correct(mIQImbalanceCorrector);
        mNoiseBlanker.process(samples.i(), samples.q());

        float[] i = mDecimationFilterI.decimateReal(samples.i());
        float[] q = mDecimationFilterQ.decimateReal(samples.q());

        mPowerMonitor.process(i, q);

        i = mBasebandFilterI.filter(i);
        q = mBasebandFilterQ.filter(q);

        i = mPulseShapingFilterI.filter(i);
        q = mPulseShapingFilterQ.filter(q);

        float[] demodulated = mDemodulator.demodulate(i, q);

        mSymbolProcessor.process(demodulated);
    }

    private float[] getBasebandFilter(double sampleRate) {
        if(BASEBAND_FILTERS.containsKey(sampleRate)) {
            return BASEBAND_FILTERS.get(sampleRate);
        }

        FIRFilterSpecification specification = FIRFilterSpecification
                .lowPassBuilder()
                .sampleRate(sampleRate)
                .passBandCutoff(5200)
                .passBandAmplitude(1.0).passBandRipple(0.01)
                .stopBandAmplitude(0.0).stopBandStart(7200)
                .stopBandRipple(0.01).build();

        float[] coefficients = null;

        try {
            coefficients = FilterFactory.getTaps(specification);
            BASEBAND_FILTERS.put(sampleRate, coefficients);
        } catch(Exception fde) {
            mLog.error("Error creating baseband filter", fde);
        }

        if(coefficients == null) {
            throw new IllegalStateException("Unable to design low pass filter for sample rate [" + sampleRate + "]");
        }

        return coefficients;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener() {
        return this::process;
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener) {
        super.setSourceEventListener(listener);
        mPowerMonitor.setSourceEventListener(listener);
    }

    @Override
    public void removeSourceEventListener() {
        super.removeSourceEventListener();
        mPowerMonitor.setSourceEventListener(null);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    private void process(SourceEvent sourceEvent) {
        switch(sourceEvent.getEvent()) {
            case NOTIFICATION_FREQUENCY_CHANGE:
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                mSymbolProcessor.resetPLL();
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
        }
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener() {
        return this;
    }
}
