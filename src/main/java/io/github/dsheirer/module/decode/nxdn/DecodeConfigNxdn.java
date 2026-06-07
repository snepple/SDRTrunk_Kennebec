package io.github.dsheirer.module.decode.nxdn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class DecodeConfigNxdn extends DecodeConfiguration
{
    public DecodeConfigNxdn()
    {
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    @Override
    public int[] getTimeslots()
    {
        return new int[]{1};
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    public DecoderType getDecoderType()
    {
        return DecoderType.NXDN;
    }

    @JsonIgnore
    @Override
    public ChannelSpecification getChannelSpecification()
    {
        // 12.5 kHz or 6.25 kHz. Let's use parameters suitable for 4800 baud C4FM
        return new ChannelSpecification(50000.0, 12500, 6500.0, 7200.0);
    }
}
