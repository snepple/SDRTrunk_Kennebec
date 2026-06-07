package io.github.dsheirer.module.decode.tetra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigTetra extends DecodeConfiguration
{
    public DecodeConfigTetra()
    {
    }

    @Override
    public int getTimeslotCount()
    {
        return 4;
    }

    @Override
    public int[] getTimeslots()
    {
        return new int[]{1, 2, 3, 4};
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.TETRA;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        return new ChannelSpecification(50000.0, 25000, 12500.0, 14000.0);
    }
}
