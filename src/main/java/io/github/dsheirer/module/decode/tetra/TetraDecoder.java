package io.github.dsheirer.module.decode.tetra;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TetraDecoder extends FeedbackDecoder implements IComplexSamplesListener {
    private static final Logger mLog = LoggerFactory.getLogger(TetraDecoder.class);
    private MacPduParser mMacParser;
    private Listener<ComplexSamples> mComplexSamplesListener;

    public TetraDecoder() {
        super();
        mMacParser = new MacPduParser();
        mLog.info("Loaded TETRA DSP Decoder Stub");
        mComplexSamplesListener = new Listener<ComplexSamples>() {
            @Override
            public void receive(ComplexSamples samples) {
                // process
            }
        };
    }

    @Override
    public DecoderType getDecoderType() {
        return DecoderType.TETRA;
    }

    @Override
    public String getProtocolDescription() {
        return "TETRA (Stub)";
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener() {
        return mComplexSamplesListener;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void reset() {
    }
}
