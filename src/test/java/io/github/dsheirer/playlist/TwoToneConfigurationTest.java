package io.github.dsheirer.playlist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for {@link TwoToneConfiguration} multi-channel Zello support and its backward-compatible
 * persistence.  Uses the same XML mapper configuration that {@code PlaylistManager} uses to save/load playlists.
 */
public class TwoToneConfigurationTest
{
    /**
     * Mirrors the XmlMapper used by PlaylistManager for save/load so the round-trip reflects real persistence.
     */
    private static ObjectMapper playlistMapper()
    {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        return new XmlMapper(xmlModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void effectiveChannels_emptyWhenNothingConfigured()
    {
        TwoToneConfiguration config = new TwoToneConfiguration();
        assertTrue(config.getEffectiveZelloChannels().isEmpty());
    }

    @Test
    public void effectiveChannels_fallsBackToLegacySingleChannel()
    {
        TwoToneConfiguration config = new TwoToneConfiguration();
        config.setZelloChannel("Oakland");
        assertEquals(List.of("Oakland"), config.getEffectiveZelloChannels());
    }

    @Test
    public void effectiveChannels_prefersMultiChannelListOverLegacy()
    {
        TwoToneConfiguration config = new TwoToneConfiguration();
        config.setZelloChannel("Legacy");
        config.setZelloChannels(Arrays.asList("Oakland", "Fairfield"));
        assertEquals(List.of("Oakland", "Fairfield"), config.getEffectiveZelloChannels());
    }

    @Test
    public void effectiveChannels_dedupesAndDropsBlanks()
    {
        TwoToneConfiguration config = new TwoToneConfiguration();
        config.setZelloChannels(Arrays.asList("Oakland", "", "  ", "Oakland", "Fairfield"));
        assertEquals(List.of("Oakland", "Fairfield"), config.getEffectiveZelloChannels());
    }

    @Test
    public void copyOf_copiesMultiChannelList()
    {
        TwoToneConfiguration config = new TwoToneConfiguration();
        config.setAlias("Oakland Fire");
        config.setZelloChannels(Arrays.asList("Oakland", "Fairfield"));

        TwoToneConfiguration copy = config.copyOf();
        assertEquals(List.of("Oakland", "Fairfield"), copy.getZelloChannels());

        //Verify the copy is independent of the original
        copy.getZelloChannels().clear();
        assertEquals(List.of("Oakland", "Fairfield"), config.getZelloChannels());
    }

    @Test
    public void xmlRoundTrip_persistsMultipleChannels() throws Exception
    {
        ObjectMapper mapper = playlistMapper();

        TwoToneConfiguration config = new TwoToneConfiguration();
        config.setAlias("Oakland Fire");
        config.setToneA(349.0);
        config.setLongATone(true);
        config.setZelloChannels(Arrays.asList("Oakland", "Fairfield"));

        PlaylistV2 playlist = new PlaylistV2();
        playlist.setTwoToneConfigurations(Collections.singletonList(config));

        String xml = mapper.writeValueAsString(playlist);
        assertTrue(xml.contains("<zelloChannelEntry>Oakland</zelloChannelEntry>"), xml);
        assertTrue(xml.contains("<zelloChannelEntry>Fairfield</zelloChannelEntry>"), xml);

        PlaylistV2 restored = mapper.readValue(xml, PlaylistV2.class);
        List<TwoToneConfiguration> configs = restored.getTwoToneConfigurations();
        assertEquals(1, configs.size());

        TwoToneConfiguration restoredConfig = configs.get(0);
        assertEquals("Oakland Fire", restoredConfig.getAlias());
        assertEquals(List.of("Oakland", "Fairfield"), restoredConfig.getZelloChannels());
        assertEquals(List.of("Oakland", "Fairfield"), restoredConfig.getEffectiveZelloChannels());
    }

    @Test
    public void xmlBackwardCompatibility_legacySingleChannelStillWorks() throws Exception
    {
        //A playlist saved before multi-channel support only has the legacy zelloChannel attribute.
        String legacyXml = "<playlist version=\"4\">" +
                "<twoTone alias=\"Oakland Fire\" toneA=\"349.0\" longATone=\"true\" zelloChannel=\"Oakland\"/>" +
                "</playlist>";

        PlaylistV2 restored = playlistMapper().readValue(legacyXml, PlaylistV2.class);
        TwoToneConfiguration config = restored.getTwoToneConfigurations().get(0);

        assertTrue(config.getZelloChannels().isEmpty());
        assertEquals("Oakland", config.getZelloChannel());
        assertEquals(List.of("Oakland"), config.getEffectiveZelloChannels());
    }
}
