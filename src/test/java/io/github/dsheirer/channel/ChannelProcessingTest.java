package io.github.dsheirer.channel;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceType;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for channel processing and tuner source subsystems.
 */
@DisplayName("Channel Processing and Tuner Source Tests")
public class ChannelProcessingTest
{
    // =========================================================================
    // 1. Channel creation, configuration, properties
    // =========================================================================
    @Nested
    @DisplayName("Channel Creation and Configuration")
    class ChannelCreationTests
    {
        @Test
        @DisplayName("Default constructor creates a channel with a unique ID")
        void defaultConstructor()
        {
            Channel c1 = new Channel();
            Channel c2 = new Channel();
            assertNotEquals(c1.getChannelID(), c2.getChannelID(),
                "Each channel should receive a unique ID");
        }

        @Test
        @DisplayName("Name constructor sets the channel name")
        void nameConstructor()
        {
            Channel channel = new Channel("TestChannel");
            assertEquals("TestChannel", channel.getName());
        }

        @Test
        @DisplayName("Name+Type constructor sets both name and type")
        void nameAndTypeConstructor()
        {
            Channel channel = new Channel("TrafficCh", ChannelType.TRAFFIC);
            assertEquals("TrafficCh", channel.getName());
            assertEquals(ChannelType.TRAFFIC, channel.getChannelType());
            assertTrue(channel.isTrafficChannel());
            assertFalse(channel.isStandardChannel());
        }

        @Test
        @DisplayName("Default channel type is STANDARD")
        void defaultChannelTypeIsStandard()
        {
            Channel channel = new Channel("Std");
            assertEquals(ChannelType.STANDARD, channel.getChannelType());
            assertTrue(channel.isStandardChannel());
            assertFalse(channel.isTrafficChannel());
        }

        @Test
        @DisplayName("Channel is not processing by default")
        void notProcessingByDefault()
        {
            Channel channel = new Channel("Test");
            assertFalse(channel.isProcessing());
        }

        @Test
        @DisplayName("Channel is not selected by default")
        void notSelectedByDefault()
        {
            Channel channel = new Channel("Test");
            assertFalse(channel.isSelected());
        }

        @Test
        @DisplayName("Auto-start is false by default")
        void autoStartFalseByDefault()
        {
            Channel channel = new Channel("Test");
            assertFalse(channel.getAutoStart());
            assertFalse(channel.isAutoStart());
        }

        @Test
        @DisplayName("Channel frequency correction is zero by default")
        void frequencyCorrectionZeroByDefault()
        {
            Channel channel = new Channel("Test");
            assertEquals(0, channel.getChannelFrequencyCorrection());
        }
    }

    // =========================================================================
    // 2. Channel property getters/setters
    // =========================================================================
    @Nested
    @DisplayName("Channel Property Getters and Setters")
    class ChannelPropertyTests
    {
        @Test
        @DisplayName("System property getter and setter")
        void systemProperty()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getSystem());
            assertFalse(channel.hasSystem());

            channel.setSystem("P25System");
            assertEquals("P25System", channel.getSystem());
            assertTrue(channel.hasSystem());
        }

        @Test
        @DisplayName("Site property getter and setter")
        void siteProperty()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getSite());
            assertFalse(channel.hasSite());

            channel.setSite("SiteAlpha");
            assertEquals("SiteAlpha", channel.getSite());
            assertTrue(channel.hasSite());
        }

        @Test
        @DisplayName("Name property getter and setter")
        void nameProperty()
        {
            Channel channel = new Channel("Original");
            assertEquals("Original", channel.getName());

            channel.setName("Renamed");
            assertEquals("Renamed", channel.getName());
        }

        @Test
        @DisplayName("State property getter and setter")
        void stateProperty()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getState());
            assertFalse(channel.hasState());

            channel.setState("California");
            assertEquals("California", channel.getState());
            assertTrue(channel.hasState());
        }

        @Test
        @DisplayName("County property getter and setter")
        void countyProperty()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getCounty());
            assertFalse(channel.hasCounty());

            channel.setCounty("Orange");
            assertEquals("Orange", channel.getCounty());
            assertTrue(channel.hasCounty());
        }

        @Test
        @DisplayName("Agency property getter and setter")
        void agencyProperty()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getAgency());
            assertFalse(channel.hasAgency());

            channel.setAgency("Fire Department");
            assertEquals("Fire Department", channel.getAgency());
            assertTrue(channel.hasAgency());
        }

        @Test
        @DisplayName("Alias list name getter and setter")
        void aliasListName()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getAliasListName());

            channel.setAliasListName("MyAliasList");
            assertEquals("MyAliasList", channel.getAliasListName());
        }

        @Test
        @DisplayName("Auto-start getter and setter")
        void autoStart()
        {
            Channel channel = new Channel("Test");
            assertFalse(channel.getAutoStart());

            channel.setAutoStart(true);
            assertTrue(channel.getAutoStart());
            assertTrue(channel.isAutoStart());

            channel.setAutoStart(false);
            assertFalse(channel.getAutoStart());
        }

        @Test
        @DisplayName("Auto-start order getter and setter")
        void autoStartOrder()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getAutoStartOrder());
            assertFalse(channel.hasAutoStartOrder());

            channel.setAutoStartOrder(5);
            assertEquals(5, channel.getAutoStartOrder());
            assertTrue(channel.hasAutoStartOrder());

            channel.setAutoStartOrder(null);
            assertNull(channel.getAutoStartOrder());
            assertFalse(channel.hasAutoStartOrder());
        }

        @Test
        @DisplayName("Image path getter and setter")
        void imagePath()
        {
            Channel channel = new Channel("Test");
            assertNull(channel.getImagePath());

            channel.setImagePath("/images/channel.png");
            assertEquals("/images/channel.png", channel.getImagePath());
        }

        @Test
        @DisplayName("Frequency correction can be reset to zero")
        void resetFrequencyCorrection()
        {
            Channel channel = new Channel("Test");
            // Simulate receiving a correction event
            SourceEvent correctionEvent = SourceEvent.frequencyCorrectionChange(150);
            // The receive() method only handles NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE, not
            // NOTIFICATION_FREQUENCY_CORRECTION_CHANGE. So we verify reset behavior on default.
            assertEquals(0, channel.getChannelFrequencyCorrection());
            channel.resetFrequencyCorrection();
            assertEquals(0, channel.getChannelFrequencyCorrection());
        }
    }

    // =========================================================================
    // 3. Channel short title and toString
    // =========================================================================
    @Nested
    @DisplayName("Channel Display Strings")
    class ChannelDisplayTests
    {
        @Test
        @DisplayName("toString contains system, site, name, and channel ID")
        void toStringFormat()
        {
            Channel channel = new Channel("Control");
            channel.setSystem("P25");
            channel.setSite("Tower1");

            String str = channel.toString();
            assertTrue(str.contains("P25"), "Should contain system");
            assertTrue(str.contains("Tower1"), "Should contain site");
            assertTrue(str.contains("Control"), "Should contain name");
        }

        @Test
        @DisplayName("toString uses defaults when system/site are null")
        void toStringDefaults()
        {
            Channel channel = new Channel("MyCh");
            String str = channel.toString();
            assertTrue(str.contains("SYSTEM"), "Should contain SYSTEM placeholder");
            assertTrue(str.contains("SITE"), "Should contain SITE placeholder");
            assertTrue(str.contains("MyCh"), "Should contain channel name");
        }

        @Test
        @DisplayName("Short title truncates long names to 10 characters")
        void shortTitleTruncation()
        {
            Channel channel = new Channel("VeryLongChannelName");
            channel.setSystem("LongSystemName123");
            channel.setSite("LongSiteName456");

            String title = channel.getShortTitle();
            // Each part should be truncated to 10 chars + ".."
            assertTrue(title.contains("LongSystem.."), "System should be truncated");
            assertTrue(title.contains("LongSiteNa.."), "Site should be truncated");
            assertTrue(title.contains("VeryLongCh.."), "Name should be truncated");
        }

        @Test
        @DisplayName("Short title uses full names when under 10 characters")
        void shortTitleNoTruncation()
        {
            Channel channel = new Channel("Ch1");
            channel.setSystem("Sys");
            channel.setSite("Site");

            String title = channel.getShortTitle();
            assertEquals("Sys/Site/Ch1", title);
        }

        @Test
        @DisplayName("Short title shows placeholders when properties are null")
        void shortTitleNullProperties()
        {
            Channel channel = new Channel();
            // Name is null from default constructor
            String title = channel.getShortTitle();
            assertTrue(title.contains("No System"));
            assertTrue(title.contains("No Site"));
            assertTrue(title.contains("No Channel"));
        }
    }

    // =========================================================================
    // 4. Channel equality and hashCode
    // =========================================================================
    @Nested
    @DisplayName("Channel Equality")
    class ChannelEqualityTests
    {
        @Test
        @DisplayName("Same channel instance is equal to itself")
        void reflexiveEquality()
        {
            Channel channel = new Channel("Test");
            assertEquals(channel, channel);
        }

        @Test
        @DisplayName("Different channels are not equal (different IDs)")
        void differentChannelsNotEqual()
        {
            Channel c1 = new Channel("Test");
            Channel c2 = new Channel("Test");
            assertNotEquals(c1, c2, "Different channels should have different IDs");
        }

        @Test
        @DisplayName("Channel is not equal to null")
        void notEqualToNull()
        {
            Channel channel = new Channel("Test");
            assertNotEquals(null, channel);
        }

        @Test
        @DisplayName("Channel is not equal to a different type")
        void notEqualToDifferentType()
        {
            Channel channel = new Channel("Test");
            assertNotEquals("string", channel);
        }

        @Test
        @DisplayName("hashCode is consistent with equals")
        void hashCodeConsistency()
        {
            Channel c1 = new Channel("Test");
            assertEquals(c1.hashCode(), c1.hashCode());
        }
    }

    // =========================================================================
    // 5. Channel copyOf
    // =========================================================================
    @Nested
    @DisplayName("Channel Deep Copy")
    class ChannelCopyTests
    {
        @Test
        @DisplayName("copyOf creates a new channel with same properties but different ID")
        void copyOfCreatesNewInstance()
        {
            Channel original = new Channel("OrigCh");
            original.setSystem("System1");
            original.setSite("SiteA");
            original.setState("TX");
            original.setCounty("Harris");
            original.setAgency("PD");
            original.setAliasListName("AliasList1");
            original.setAutoStart(true);
            original.setAutoStartOrder(3);

            Channel copy = original.copyOf();

            assertNotEquals(original.getChannelID(), copy.getChannelID(),
                "Copy should have a different ID");
            assertEquals("OrigCh", copy.getName());
            assertEquals("System1", copy.getSystem());
            assertEquals("SiteA", copy.getSite());
            assertEquals("TX", copy.getState());
            assertEquals("Harris", copy.getCounty());
            assertEquals("PD", copy.getAgency());
            assertEquals("AliasList1", copy.getAliasListName());
            assertTrue(copy.getAutoStart());
            assertEquals(3, copy.getAutoStartOrder());
        }

        @Test
        @DisplayName("copyOf with null auto-start order preserves null")
        void copyOfNullAutoStartOrder()
        {
            Channel original = new Channel("Ch");
            original.setAutoStartOrder(null);

            Channel copy = original.copyOf();
            assertNull(copy.getAutoStartOrder());
        }
    }

    // =========================================================================
    // 6. Channel source configuration
    // =========================================================================
    @Nested
    @DisplayName("Channel Source Configuration")
    class ChannelSourceConfigTests
    {
        @Test
        @DisplayName("Default source configuration is not null")
        void defaultSourceConfigNotNull()
        {
            Channel channel = new Channel("Test");
            assertNotNull(channel.getSourceConfiguration());
        }

        @Test
        @DisplayName("Setting source configuration updates the channel")
        void setSourceConfiguration()
        {
            Channel channel = new Channel("Test");
            SourceConfigTuner tunerConfig = new SourceConfigTuner();
            tunerConfig.setFrequency(460_000_000L);

            channel.setSourceConfiguration(tunerConfig);
            assertSame(tunerConfig, channel.getSourceConfiguration());
        }

        @Test
        @DisplayName("Setting null source configuration does not overwrite existing")
        void setNullSourceConfigPreservesExisting()
        {
            Channel channel = new Channel("Test");
            SourceConfigTuner tunerConfig = new SourceConfigTuner();
            tunerConfig.setFrequency(460_000_000L);
            channel.setSourceConfiguration(tunerConfig);

            channel.setSourceConfiguration(null);
            // Should still have the tuner config since null is rejected
            assertSame(tunerConfig, channel.getSourceConfiguration());
        }

        @Test
        @DisplayName("Setting tuner source configuration populates frequency list")
        void tunerSourcePopulatesFrequencyList()
        {
            Channel channel = new Channel("Test");
            SourceConfigTuner tunerConfig = new SourceConfigTuner();
            tunerConfig.setFrequency(851_000_000L);
            channel.setSourceConfiguration(tunerConfig);

            assertNotNull(channel.getFrequencyList());
            assertEquals(1, channel.getFrequencyList().size());
            assertEquals(851_000_000L, channel.getFrequencyList().get(0));
        }
    }

    // =========================================================================
    // 7. TunerChannel tests
    // =========================================================================
    @Nested
    @DisplayName("TunerChannel Frequency and Bandwidth")
    class TunerChannelTests
    {
        @Test
        @DisplayName("Constructor sets frequency and bandwidth")
        void constructorSetsValues()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertEquals(460_000_000L, tc.getFrequency());
            assertEquals(12500, tc.getBandwidth());
        }

        @Test
        @DisplayName("Frequency setter updates the frequency")
        void setFrequency()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            tc.setFrequency(851_000_000L);
            assertEquals(851_000_000L, tc.getFrequency());
        }

        @Test
        @DisplayName("Bandwidth setter updates the bandwidth")
        void setBandwidth()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            tc.setBandwidth(25000);
            assertEquals(25000, tc.getBandwidth());
        }

        @Test
        @DisplayName("getMinFrequency returns frequency minus half bandwidth")
        void minFrequency()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertEquals(460_000_000L - 6250, tc.getMinFrequency());
        }

        @Test
        @DisplayName("getMaxFrequency returns frequency plus half bandwidth")
        void maxFrequency()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertEquals(460_000_000L + 6250, tc.getMaxFrequency());
        }

        @Test
        @DisplayName("Center frequency calculation for symmetric bandwidth")
        void centerFrequencySymmetric()
        {
            TunerChannel tc = new TunerChannel(100_000_000L, 20000);
            long expectedMin = 100_000_000L - 10000;
            long expectedMax = 100_000_000L + 10000;
            assertEquals(expectedMin, tc.getMinFrequency());
            assertEquals(expectedMax, tc.getMaxFrequency());
            // Center should be exactly the frequency
            assertEquals(tc.getFrequency(), (tc.getMinFrequency() + tc.getMaxFrequency()) / 2);
        }

        @Test
        @DisplayName("Overlaps returns true when ranges fully overlap")
        void overlapsFullOverlap()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            // Range fully encompasses the channel
            assertTrue(tc.overlaps(459_000_000L, 461_000_000L));
        }

        @Test
        @DisplayName("Overlaps returns true when range partially overlaps on low end")
        void overlapsPartialLow()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            // Range overlaps the lower portion
            assertTrue(tc.overlaps(459_990_000L, 460_000_000L));
        }

        @Test
        @DisplayName("Overlaps returns true when range partially overlaps on high end")
        void overlapsPartialHigh()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            // Range overlaps the upper portion
            assertTrue(tc.overlaps(460_000_000L, 460_010_000L));
        }

        @Test
        @DisplayName("Overlaps returns true when channel fully encompasses the range")
        void overlapsChannelEncompassesRange()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 1_000_000);
            // Channel is wide enough to fully contain the test range
            assertTrue(tc.overlaps(460_000_000L - 100, 460_000_000L + 100));
        }

        @Test
        @DisplayName("Overlaps returns false when range is completely below channel")
        void overlapsNoOverlapBelow()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertFalse(tc.overlaps(450_000_000L, 459_990_000L));
        }

        @Test
        @DisplayName("Overlaps returns false when range is completely above channel")
        void overlapsNoOverlapAbove()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertFalse(tc.overlaps(460_010_000L, 470_000_000L));
        }

        @Test
        @DisplayName("Overlaps returns true at exact boundary match")
        void overlapsExactBoundary()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            long minFreq = tc.getMinFrequency();
            long maxFreq = tc.getMaxFrequency();
            // Range exactly matches channel boundaries
            assertTrue(tc.overlaps(minFreq, maxFreq));
        }

        @Test
        @DisplayName("compareTo orders by frequency")
        void compareToOrdering()
        {
            TunerChannel low = new TunerChannel(100_000_000L, 12500);
            TunerChannel high = new TunerChannel(200_000_000L, 12500);
            assertTrue(low.compareTo(high) < 0);
            assertTrue(high.compareTo(low) > 0);
        }

        @Test
        @DisplayName("compareTo returns 0 for same frequency")
        void compareToEqual()
        {
            TunerChannel tc1 = new TunerChannel(460_000_000L, 12500);
            TunerChannel tc2 = new TunerChannel(460_000_000L, 25000);
            assertEquals(0, tc1.compareTo(tc2));
        }

        @Test
        @DisplayName("equals returns true for same frequency")
        void equalsTrue()
        {
            TunerChannel tc1 = new TunerChannel(460_000_000L, 12500);
            TunerChannel tc2 = new TunerChannel(460_000_000L, 25000);
            assertEquals(tc1, tc2);
        }

        @Test
        @DisplayName("equals returns false for different frequency")
        void equalsFalse()
        {
            TunerChannel tc1 = new TunerChannel(460_000_000L, 12500);
            TunerChannel tc2 = new TunerChannel(461_000_000L, 12500);
            assertNotEquals(tc1, tc2);
        }

        @Test
        @DisplayName("equals returns false for non-TunerChannel object")
        void equalsNonTunerChannel()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            assertNotEquals("not a tuner channel", tc);
        }

        @Test
        @DisplayName("toString contains frequency and range info")
        void toStringContainsInfo()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 12500);
            String str = tc.toString();
            assertTrue(str.contains("460000000"), "Should contain frequency");
            assertTrue(str.contains(String.valueOf(tc.getMinFrequency())), "Should contain min frequency");
            assertTrue(str.contains(String.valueOf(tc.getMaxFrequency())), "Should contain max frequency");
        }

        @Test
        @DisplayName("Zero bandwidth results in min==max==frequency")
        void zeroBandwidth()
        {
            TunerChannel tc = new TunerChannel(460_000_000L, 0);
            assertEquals(460_000_000L, tc.getMinFrequency());
            assertEquals(460_000_000L, tc.getMaxFrequency());
        }
    }

    // =========================================================================
    // 8. SourceEvent tests
    // =========================================================================
    @Nested
    @DisplayName("SourceEvent Factory Methods and Properties")
    class SourceEventTests
    {
        @Test
        @DisplayName("frequencyChange creates a NOTIFICATION_FREQUENCY_CHANGE event")
        void frequencyChangeEvent()
        {
            SourceEvent event = SourceEvent.frequencyChange(null, 460_000_000L);
            assertEquals(SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE, event.getEvent());
            assertEquals(460_000_000L, event.getValue().longValue());
        }

        @Test
        @DisplayName("frequencyChange with description creates event with correct type")
        void frequencyChangeWithDescription()
        {
            SourceEvent event = SourceEvent.frequencyChange(null, 460_000_000L, "test desc");
            assertEquals(SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE, event.getEvent());
            assertEquals(460_000_000L, event.getValue().longValue());
        }

        @Test
        @DisplayName("sampleRateChange creates a NOTIFICATION_SAMPLE_RATE_CHANGE event")
        void sampleRateChangeEvent()
        {
            SourceEvent event = SourceEvent.sampleRateChange(2_400_000.0);
            assertEquals(SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE, event.getEvent());
            assertEquals(2_400_000.0, event.getValue().doubleValue(), 0.001);
        }

        @Test
        @DisplayName("sampleRateChange with description preserves sample rate value")
        void sampleRateChangeWithDescription()
        {
            SourceEvent event = SourceEvent.sampleRateChange(48000.0, "48kHz rate");
            assertEquals(SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE, event.getEvent());
            assertEquals(48000.0, event.getValue().doubleValue(), 0.001);
        }

        @Test
        @DisplayName("channelSampleRateChange creates correct event type")
        void channelSampleRateChange()
        {
            SourceEvent event = SourceEvent.channelSampleRateChange(48000.0);
            assertEquals(SourceEvent.Event.NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE, event.getEvent());
            assertEquals(48000.0, event.getValue().doubleValue(), 0.001);
        }

        @Test
        @DisplayName("frequencyRequest creates a REQUEST_FREQUENCY_CHANGE event")
        void frequencyRequestEvent()
        {
            SourceEvent event = SourceEvent.frequencyRequest(851_000_000L);
            assertEquals(SourceEvent.Event.REQUEST_FREQUENCY_CHANGE, event.getEvent());
            assertEquals(851_000_000L, event.getValue().longValue());
        }

        @Test
        @DisplayName("frequencyCorrectionChange creates correct event")
        void frequencyCorrectionChange()
        {
            SourceEvent event = SourceEvent.frequencyCorrectionChange(500L);
            assertEquals(SourceEvent.Event.NOTIFICATION_FREQUENCY_CORRECTION_CHANGE, event.getEvent());
            assertEquals(500L, event.getValue().longValue());
        }

        @Test
        @DisplayName("channelCountChange creates correct event")
        void channelCountChange()
        {
            SourceEvent event = SourceEvent.channelCountChange(5);
            assertEquals(SourceEvent.Event.NOTIFICATION_CHANNEL_COUNT_CHANGE, event.getEvent());
            assertEquals(5, event.getValue().intValue());
        }

        @Test
        @DisplayName("lockedSampleRateState creates correct event")
        void lockedSampleRateState()
        {
            SourceEvent event = SourceEvent.lockedSampleRateState();
            assertEquals(SourceEvent.Event.NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED, event.getEvent());
        }

        @Test
        @DisplayName("unlockedSampleRateState creates correct event")
        void unlockedSampleRateState()
        {
            SourceEvent event = SourceEvent.unlockedSampleRateState();
            assertEquals(SourceEvent.Event.NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED, event.getEvent());
        }

        @Test
        @DisplayName("frequencyRotationRequest creates correct event")
        void frequencyRotationRequest()
        {
            SourceEvent event = SourceEvent.frequencyRotationRequest();
            assertEquals(SourceEvent.Event.REQUEST_FREQUENCY_ROTATION, event.getEvent());
        }

        @Test
        @DisplayName("frequencySelectionRequest creates correct event")
        void frequencySelectionRequest()
        {
            SourceEvent event = SourceEvent.frequencySelectionRequest();
            assertEquals(SourceEvent.Event.REQUEST_FREQUENCY_SELECTION, event.getEvent());
        }

        @Test
        @DisplayName("recordingFileLoaded creates correct event")
        void recordingFileLoaded()
        {
            SourceEvent event = SourceEvent.recordingFileLoaded();
            assertEquals(SourceEvent.Event.NOTIFICATION_RECORDING_FILE_LOADED, event.getEvent());
        }

        @Test
        @DisplayName("frequencyErrorMeasurement creates correct event")
        void frequencyErrorMeasurement()
        {
            SourceEvent event = SourceEvent.frequencyErrorMeasurement(250L);
            assertEquals(SourceEvent.Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR, event.getEvent());
            assertEquals(250L, event.getValue().longValue());
        }

        @Test
        @DisplayName("frequencyErrorMeasurementSyncLocked creates correct event")
        void frequencyErrorMeasurementSyncLocked()
        {
            SourceEvent event = SourceEvent.frequencyErrorMeasurementSyncLocked(100L, "sync locked");
            assertEquals(SourceEvent.Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED, event.getEvent());
            assertEquals(100L, event.getValue().longValue());
        }

        @Test
        @DisplayName("carrierOffsetMeasurement creates correct event")
        void carrierOffsetMeasurement()
        {
            SourceEvent event = SourceEvent.carrierOffsetMeasurement(1500L);
            assertEquals(SourceEvent.Event.NOTIFICATION_CARRIER_OFFSET_FREQUENCY, event.getEvent());
            assertEquals(1500L, event.getValue().longValue());
        }

        @Test
        @DisplayName("squelchThreshold creates correct event")
        void squelchThreshold()
        {
            SourceEvent event = SourceEvent.squelchThreshold(null, -80.0);
            assertEquals(SourceEvent.Event.NOTIFICATION_SQUELCH_THRESHOLD, event.getEvent());
            assertEquals(-80.0, event.getValue().doubleValue(), 0.001);
        }

        @Test
        @DisplayName("squelchAutoTrack creates correct event with 1 for true")
        void squelchAutoTrackTrue()
        {
            SourceEvent event = SourceEvent.squelchAutoTrack(true);
            assertEquals(SourceEvent.Event.NOTIFICATION_SQUELCH_AUTO_TRACK, event.getEvent());
            assertEquals(1, event.getValue().intValue());
        }

        @Test
        @DisplayName("squelchAutoTrack creates correct event with 0 for false")
        void squelchAutoTrackFalse()
        {
            SourceEvent event = SourceEvent.squelchAutoTrack(false);
            assertEquals(SourceEvent.Event.NOTIFICATION_SQUELCH_AUTO_TRACK, event.getEvent());
            assertEquals(0, event.getValue().intValue());
        }

        @Test
        @DisplayName("Event has no source when created without one")
        void noSource()
        {
            SourceEvent event = SourceEvent.frequencyRequest(100L);
            assertFalse(event.hasSource());
            assertNull(event.getSource());
        }

        @Test
        @DisplayName("hasValue returns true when value is present")
        void hasValue()
        {
            SourceEvent event = SourceEvent.frequencyRequest(100L);
            assertTrue(event.hasValue());
        }

        @Test
        @DisplayName("toString produces non-null output")
        void toStringNotNull()
        {
            SourceEvent event = SourceEvent.frequencyChange(null, 460_000_000L);
            assertNotNull(event.toString());
            assertTrue(event.toString().contains("NOTIFICATION_FREQUENCY_CHANGE"));
        }

        @Test
        @DisplayName("Notification events are correctly identified as notification events")
        void isNotificationEvent()
        {
            SourceEvent event = SourceEvent.frequencyChange(null, 460_000_000L);
            assertTrue(event.isNotificationEvent());
        }
    }

    // =========================================================================
    // 9. SourceConfigTuner tests
    // =========================================================================
    @Nested
    @DisplayName("SourceConfigTuner Configuration")
    class SourceConfigTunerTests
    {
        @Test
        @DisplayName("Default frequency is 0")
        void defaultFrequency()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            assertEquals(0L, config.getFrequency());
        }

        @Test
        @DisplayName("Frequency getter and setter")
        void frequencyGetterSetter()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(460_000_000L);
            assertEquals(460_000_000L, config.getFrequency());
        }

        @Test
        @DisplayName("Source type is TUNER")
        void sourceType()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            assertEquals(SourceType.TUNER, config.getSourceType());
        }

        @Test
        @DisplayName("Constructor from TunerChannel copies frequency")
        void constructFromTunerChannel()
        {
            TunerChannel tc = new TunerChannel(851_000_000L, 12500);
            SourceConfigTuner config = new SourceConfigTuner(tc);
            assertEquals(851_000_000L, config.getFrequency());
            assertEquals(SourceType.TUNER, config.getSourceType());
        }

        @Test
        @DisplayName("Preferred tuner is null by default")
        void preferredTunerDefault()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            assertNull(config.getPreferredTuner());
            assertFalse(config.hasPreferredTuner());
        }

        @Test
        @DisplayName("Preferred tuner getter and setter")
        void preferredTunerGetterSetter()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setPreferredTuner("RTL-SDR v3 SN:00000001");
            assertEquals("RTL-SDR v3 SN:00000001", config.getPreferredTuner());
            assertTrue(config.hasPreferredTuner());
        }

        @Test
        @DisplayName("getDescription returns frequency in MHz format")
        void descriptionFormat()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(460_125_000L);
            String desc = config.getDescription();
            assertTrue(desc.contains("460.12500"), "Description should contain formatted frequency: " + desc);
            assertTrue(desc.contains("MHz"), "Description should contain MHz");
        }

        @Test
        @DisplayName("getDescription includes preferred tuner when set")
        void descriptionWithPreferredTuner()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(460_125_000L);
            config.setPreferredTuner("MyTuner");
            String desc = config.getDescription();
            assertTrue(desc.contains("PREFERRED TUNER:MyTuner"), "Should include preferred tuner: " + desc);
        }

        @Test
        @DisplayName("getTunerChannel creates a TunerChannel with the correct frequency and bandwidth")
        void getTunerChannel()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(460_000_000L);
            TunerChannel tc = config.getTunerChannel(12500);
            assertEquals(460_000_000L, tc.getFrequency());
            assertEquals(12500, tc.getBandwidth());
        }

        @Test
        @DisplayName("toString matches getDescription")
        void toStringMatchesDescription()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(460_000_000L);
            assertEquals(config.getDescription(), config.toString());
        }

        @Test
        @DisplayName("Large frequency values are handled correctly")
        void largeFrequency()
        {
            SourceConfigTuner config = new SourceConfigTuner();
            config.setFrequency(1_296_000_000L); // 1.296 GHz
            assertEquals(1_296_000_000L, config.getFrequency());
        }
    }

    // =========================================================================
    // 10. ChannelType enum tests
    // =========================================================================
    @Nested
    @DisplayName("ChannelType Enum Values")
    class ChannelTypeTests
    {
        @Test
        @DisplayName("STANDARD enum value exists")
        void standardExists()
        {
            assertNotNull(ChannelType.STANDARD);
        }

        @Test
        @DisplayName("TRAFFIC enum value exists")
        void trafficExists()
        {
            assertNotNull(ChannelType.TRAFFIC);
        }

        @Test
        @DisplayName("Exactly two ChannelType values exist")
        void exactlyTwoValues()
        {
            assertEquals(2, ChannelType.values().length);
        }

        @Test
        @DisplayName("valueOf works for STANDARD")
        void valueOfStandard()
        {
            assertEquals(ChannelType.STANDARD, ChannelType.valueOf("STANDARD"));
        }

        @Test
        @DisplayName("valueOf works for TRAFFIC")
        void valueOfTraffic()
        {
            assertEquals(ChannelType.TRAFFIC, ChannelType.valueOf("TRAFFIC"));
        }

        @Test
        @DisplayName("valueOf throws for invalid name")
        void valueOfInvalid()
        {
            assertThrows(IllegalArgumentException.class, () -> ChannelType.valueOf("INVALID"));
        }
    }

    // =========================================================================
    // 11. SourceEvent.Event enum coverage
    // =========================================================================
    @Nested
    @DisplayName("SourceEvent.Event Enum")
    class SourceEventEnumTests
    {
        @Test
        @DisplayName("NOTIFICATION_EVENTS set is not empty")
        void notificationEventsNotEmpty()
        {
            assertFalse(SourceEvent.Event.NOTIFICATION_EVENTS.isEmpty());
        }

        @Test
        @DisplayName("Key notification events exist in the enum")
        void keyNotificationEventsExist()
        {
            assertNotNull(SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE);
            assertNotNull(SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE);
            assertNotNull(SourceEvent.Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE);
            assertNotNull(SourceEvent.Event.NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE);
            assertNotNull(SourceEvent.Event.NOTIFICATION_STOP_SAMPLE_STREAM);
            assertNotNull(SourceEvent.Event.NOTIFICATION_TUNER_SHUTDOWN);
            assertNotNull(SourceEvent.Event.NOTIFICATION_ERROR_STATE);
        }

        @Test
        @DisplayName("Key request events exist in the enum")
        void keyRequestEventsExist()
        {
            assertNotNull(SourceEvent.Event.REQUEST_FREQUENCY_CHANGE);
            assertNotNull(SourceEvent.Event.REQUEST_FREQUENCY_ROTATION);
            assertNotNull(SourceEvent.Event.REQUEST_START_SAMPLE_STREAM);
            assertNotNull(SourceEvent.Event.REQUEST_STOP_SAMPLE_STREAM);
        }

        @Test
        @DisplayName("NOTIFICATION_FREQUENCY_CHANGE is in the NOTIFICATION_EVENTS set")
        void frequencyChangeInNotificationSet()
        {
            assertTrue(SourceEvent.Event.NOTIFICATION_EVENTS
                .contains(SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE));
        }

        @Test
        @DisplayName("NOTIFICATION_SAMPLE_RATE_CHANGE is in the NOTIFICATION_EVENTS set")
        void sampleRateChangeInNotificationSet()
        {
            assertTrue(SourceEvent.Event.NOTIFICATION_EVENTS
                .contains(SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE));
        }
    }

    // =========================================================================
    // 12. SourceType enum coverage
    // =========================================================================
    @Nested
    @DisplayName("SourceType Enum")
    class SourceTypeTests
    {
        @Test
        @DisplayName("TUNER source type exists")
        void tunerExists()
        {
            assertEquals("Tuner", SourceType.TUNER.toString());
        }

        @Test
        @DisplayName("NONE source type exists")
        void noneExists()
        {
            assertEquals("No Source", SourceType.NONE.toString());
        }

        @Test
        @DisplayName("RECORDING source type exists")
        void recordingExists()
        {
            assertEquals("IQ Recording", SourceType.RECORDING.toString());
        }

        @Test
        @DisplayName("getTypes returns TUNER, TUNER_MULTIPLE_FREQUENCIES, and MIXER")
        void getTypes()
        {
            SourceType[] types = SourceType.getTypes();
            assertEquals(3, types.length);
            assertEquals(SourceType.TUNER, types[0]);
            assertEquals(SourceType.TUNER_MULTIPLE_FREQUENCIES, types[1]);
            assertEquals(SourceType.MIXER, types[2]);
        }
    }
}
