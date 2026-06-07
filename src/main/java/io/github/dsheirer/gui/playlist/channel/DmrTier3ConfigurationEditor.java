package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;

public class DmrTier3ConfigurationEditor extends ChannelConfigurationEditor {

    public DmrTier3ConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager, UserPreferences userPreferences, IFilterProcessor filterProcessor) {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
    }

    @Override
    public DecoderType getDecoderType() { return DecoderType.DMR_TIER_3; }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config) {}

    @Override
    protected void saveDecoderConfiguration() {}

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config) {}

    @Override
    protected void saveEventLogConfiguration() {}

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config) {}

    @Override
    protected void saveAuxDecoderConfiguration() {}

    @Override
    protected void setRecordConfiguration(RecordConfiguration config) {}

    @Override
    protected void saveRecordConfiguration() {}

    @Override
    protected void setSourceConfiguration(SourceConfiguration config) {}

    @Override
    protected void saveSourceConfiguration() {}
}
