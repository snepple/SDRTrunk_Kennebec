package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class PassThroughSourceManagerTest
{
    @Test
    void duplicateFrequencySourcesRemainDistinctWhenOneStops() throws Exception
    {
        PassThroughSourceManager manager = new PassThroughSourceManager(new TestTunerController());
        ChannelSpecification specification = new ChannelSpecification(12000.0, 12500, 3000.0, 6500.0);
        TunerChannel firstChannel = new TunerChannel(154_325_000L, 12500);
        TunerChannel secondChannel = new TunerChannel(154_325_000L, 12500);

        TunerChannelSource firstSource = manager.getSource(firstChannel, specification, "duplicate-frequency-1");
        TunerChannelSource secondSource = manager.getSource(secondChannel, specification, "duplicate-frequency-2");

        assertNotNull(firstSource);
        assertNotNull(secondSource);
        assertNotSame(firstSource, secondSource);
        assertEquals(2, manager.getTunerChannelCount());
        assertEquals(1, manager.getTunerChannels().size());

        manager.process(SourceEvent.stopSampleStreamRequest(firstSource));

        assertEquals(1, manager.getTunerChannelCount());
        assertEquals(1, manager.getTunerChannels().size());
        assertEquals(154_325_000L, manager.getTunerChannels().first().getFrequency());

        manager.process(SourceEvent.stopSampleStreamRequest(secondSource));

        assertEquals(0, manager.getTunerChannelCount());
        assertEquals(0, manager.getTunerChannels().size());
    }

    private static class TestTunerController extends TunerController
    {
        TestTunerController()
        {
            super(null);
        }

        @Override
        public void start() throws SourceException
        {
        }

        @Override
        public void stop()
        {
        }

        @Override
        public TunerType getTunerType()
        {
            return TunerType.TEST;
        }

        @Override
        public int getBufferSampleCount()
        {
            return 1024;
        }

        @Override
        public double getCurrentSampleRate()
        {
            return 2_400_000.0;
        }

        @Override
        public void setTunedFrequency(long frequency)
        {
        }

        @Override
        public long getTunedFrequency()
        {
            return 154_325_000L;
        }
    }
}
