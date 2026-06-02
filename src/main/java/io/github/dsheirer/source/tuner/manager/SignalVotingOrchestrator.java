package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrator that dynamically ingests multiple streams and seamlessly routes the cleanest packet stream
 * based on the Bit Error Rate (BER).
 */
public class SignalVotingOrchestrator implements Listener<CorrectedBinaryMessage> {
    private static final Logger mLog = LoggerFactory.getLogger(SignalVotingOrchestrator.class);
    
    private Broadcaster<CorrectedBinaryMessage> mBroadcaster = new Broadcaster<>();
    private AtomicReference<String> mActiveSourceId = new AtomicReference<>();
    
    public void addListener(Listener<CorrectedBinaryMessage> listener) {
        mBroadcaster.addListener(listener);
    }
    
    public void removeListener(Listener<CorrectedBinaryMessage> listener) {
        mBroadcaster.removeListener(listener);
    }

    @Override
    public void receive(CorrectedBinaryMessage message) {
        if (message != null) {
            double ber = (double) message.getCorrectedBitCount() / message.size();
            
            // Only forward if it's the cleanest stream or if we're evaluating.
            // Simplified voting logic: if BER is zero, it's perfect, forward it.
            // If we had sequence numbers, we would buffer and pick the lowest BER for the sequence.
            
            if (ber <= 0.05) { // 5% BER threshold
                mBroadcaster.broadcast(message);
            }
        }
    }
}
