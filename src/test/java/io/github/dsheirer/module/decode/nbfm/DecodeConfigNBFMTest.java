package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecodeConfigNBFMTest
{
    @Test
    void appliesToneSquelchDefaultsWhenCtcssToneIsAddedToEnabledFilter()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();

        config.setToneFilterEnabled(true);
        config.addToneFilter(new ChannelToneFilter(ChannelToneFilter.ToneType.CTCSS, CTCSSCode.TONE_1A.name(), ""));

        assertTrue(config.isToneRequireNoiseSquelch());
        assertEquals(DecodeConfigNBFM.DEFAULT_TONE_MIN_CALL_DURATION_MS, config.getToneMinCallDurationMs());
        assertEquals(DecodeConfigNBFM.DEFAULT_TONE_FILTERED_MIN_CALL_DURATION_MS, config.getMinCallDurationMs());
    }

    @Test
    void appliesToneSquelchDefaultsWhenDcsFilterIsEnabledAfterToneIsAdded()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();

        config.addToneFilter(new ChannelToneFilter(ChannelToneFilter.ToneType.DCS, DCSCode.N114.name(), ""));
        config.setToneFilterEnabled(true);

        assertTrue(config.isToneRequireNoiseSquelch());
        assertEquals(DecodeConfigNBFM.DEFAULT_TONE_MIN_CALL_DURATION_MS, config.getToneMinCallDurationMs());
        assertEquals(DecodeConfigNBFM.DEFAULT_TONE_FILTERED_MIN_CALL_DURATION_MS, config.getMinCallDurationMs());
    }

    @Test
    void preservesExplicitToneSquelchSettingsWhenToneIsAdded()
    {
        DecodeConfigNBFM config = new DecodeConfigNBFM();
        config.setToneRequireNoiseSquelch(false);
        config.setToneMinCallDurationMs(900);
        config.setMinCallDurationMs(1200);

        config.setToneFilterEnabled(true);
        config.addToneFilter(new ChannelToneFilter(ChannelToneFilter.ToneType.CTCSS, CTCSSCode.TONE_1B.name(), ""));

        assertFalse(config.isToneRequireNoiseSquelch());
        assertEquals(900, config.getToneMinCallDurationMs());
        assertEquals(1200, config.getMinCallDurationMs());
    }
}
