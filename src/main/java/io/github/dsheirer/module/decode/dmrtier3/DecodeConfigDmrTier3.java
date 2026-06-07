package io.github.dsheirer.module.decode.dmrtier3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigDmrTier3 extends DecodeConfiguration
{
    public DecodeConfigDmrTier3()
    {
    }

    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    @Override
    public int[] getTimeslots()
    {
        return new int[]{1, 2};
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR_TIER_3;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0);
    }
}
