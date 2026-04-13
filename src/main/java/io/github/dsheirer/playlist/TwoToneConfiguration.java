package io.github.dsheirer.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;

/**
 * Two Tone configuration mapping
 */
public class TwoToneConfiguration
{
    private StringProperty mAliasProperty = new SimpleStringProperty();
    private StringProperty mTemplateProperty = new SimpleStringProperty();
    private StringProperty mZelloChannelProperty = new SimpleStringProperty();
    private double mToneA;
    private double mToneB;

    public TwoToneConfiguration()
    {
    }

    public TwoToneConfiguration copyOf()
    {
        TwoToneConfiguration copy = new TwoToneConfiguration();
        copy.setAlias(getAlias());
        copy.setTemplate(getTemplate());
        copy.setZelloChannel(getZelloChannel());
        copy.setToneA(getToneA());
        copy.setToneB(getToneB());
        return copy;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "alias")
    public String getAlias()
    {
        return mAliasProperty.get();
    }

    public void setAlias(String alias)
    {
        mAliasProperty.set(alias);
    }

    @JsonIgnore
    public StringProperty aliasProperty()
    {
        return mAliasProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "template")
    public String getTemplate()
    {
        return mTemplateProperty.get();
    }

    public void setTemplate(String template)
    {
        mTemplateProperty.set(template);
    }

    @JsonIgnore
    public StringProperty templateProperty()
    {
        return mTemplateProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "zelloChannel")
    public String getZelloChannel()
    {
        return mZelloChannelProperty.get();
    }

    public void setZelloChannel(String zelloChannel)
    {
        mZelloChannelProperty.set(zelloChannel);
    }

    @JsonIgnore
    public StringProperty zelloChannelProperty()
    {
        return mZelloChannelProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneA")
    public double getToneA()
    {
        return mToneA;
    }

    public void setToneA(double toneA)
    {
        mToneA = toneA;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneB")
    public double getToneB()
    {
        return mToneB;
    }

    public void setToneB(double toneB)
    {
        mToneB = toneB;
    }

    public static Callback<TwoToneConfiguration, Observable[]> extractor()
    {
        return (TwoToneConfiguration config) -> new Observable[]{
            config.aliasProperty(), config.templateProperty(), config.zelloChannelProperty()
        };
    }
}
