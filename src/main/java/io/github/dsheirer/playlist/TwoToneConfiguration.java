package io.github.dsheirer.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Two Tone configuration mapping
 */
public class TwoToneConfiguration
{
    private StringProperty mAliasProperty = new SimpleStringProperty();
    private StringProperty mTemplateProperty = new SimpleStringProperty("Dispatch Received: {Alias}");
    private StringProperty mZelloChannelProperty = new SimpleStringProperty();
    private DoubleProperty mToneAProperty = new SimpleDoubleProperty(0.0);
    private DoubleProperty mToneBProperty = new SimpleDoubleProperty(0.0);
    private BooleanProperty mLongAToneProperty = new SimpleBooleanProperty(false);
    private BooleanProperty mEnableMqttPublishProperty = new SimpleBooleanProperty(false);
    private StringProperty mMqttTopicProperty = new SimpleStringProperty("");
    private StringProperty mMqttPayloadProperty = new SimpleStringProperty("{\"detector\": \"[DetectorName]\", \"state\": \"ON\", \"time\": \"[Timestamp]\"}");

    private BooleanProperty mEnableZelloAlertProperty = new SimpleBooleanProperty(false);
    private StringProperty mZelloAlertFileProperty = new SimpleStringProperty("");
    private BooleanProperty mEnableZelloTextMessageProperty = new SimpleBooleanProperty(true);

    private DoubleProperty mFrequencyToleranceProperty = new SimpleDoubleProperty(10.0);
    private DoubleProperty mToneDurationMsProperty = new SimpleDoubleProperty(300.0);

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
        copy.setLongATone(isLongATone());
        copy.setEnableMqttPublish(isEnableMqttPublish());
        copy.setMqttTopic(getMqttTopic());
        copy.setMqttPayload(getMqttPayload());
        copy.setEnableZelloAlert(isEnableZelloAlert());
        copy.setZelloAlertFile(getZelloAlertFile());
        copy.setEnableZelloTextMessage(isEnableZelloTextMessage());
        copy.setFrequencyTolerance(getFrequencyTolerance());
        copy.setToneDurationMs(getToneDurationMs());
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
        return mToneAProperty.get();
    }

    public void setToneA(double toneA)
    {
        mToneAProperty.set(toneA);
    }

    @JsonIgnore
    public DoubleProperty toneAProperty()
    {
        return mToneAProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "longATone")
    public boolean isLongATone()
    {
        return mLongAToneProperty.get();
    }

    public void setLongATone(boolean longATone)
    {
        mLongAToneProperty.set(longATone);
    }

    @JsonIgnore
    public BooleanProperty longAToneProperty()
    {
        return mLongAToneProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneB")
    public double getToneB()
    {
        return mToneBProperty.get();
    }

    public void setToneB(double toneB)
    {
        mToneBProperty.set(toneB);
    }

    @JsonIgnore
    public DoubleProperty toneBProperty()
    {
        return mToneBProperty;
    }


    @JacksonXmlProperty(isAttribute = true, localName = "enableMqttPublish")
    public boolean isEnableMqttPublish()
    {
        return mEnableMqttPublishProperty.get();
    }

    public void setEnableMqttPublish(boolean enableMqttPublish)
    {
        mEnableMqttPublishProperty.set(enableMqttPublish);
    }

    @JsonIgnore
    public BooleanProperty enableMqttPublishProperty()
    {
        return mEnableMqttPublishProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mqttTopic")
    public String getMqttTopic()
    {
        return mMqttTopicProperty.get();
    }

    public void setMqttTopic(String mqttTopic)
    {
        mMqttTopicProperty.set(mqttTopic);
    }

    @JsonIgnore
    public StringProperty mqttTopicProperty()
    {
        return mMqttTopicProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "mqttPayload")
    public String getMqttPayload()
    {
        return mMqttPayloadProperty.get();
    }

    public void setMqttPayload(String mqttPayload)
    {
        mMqttPayloadProperty.set(mqttPayload);
    }

    @JsonIgnore
    public StringProperty mqttPayloadProperty()
    {
        return mMqttPayloadProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "enableZelloAlert")
    public boolean isEnableZelloAlert()
    {
        return mEnableZelloAlertProperty.get();
    }

    public void setEnableZelloAlert(boolean enableZelloAlert)
    {
        mEnableZelloAlertProperty.set(enableZelloAlert);
    }

    @JsonIgnore
    public BooleanProperty enableZelloAlertProperty()
    {
        return mEnableZelloAlertProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "zelloAlertFile")
    public String getZelloAlertFile()
    {
        return mZelloAlertFileProperty.get();
    }

    public void setZelloAlertFile(String zelloAlertFile)
    {
        mZelloAlertFileProperty.set(zelloAlertFile);
    }

    @JsonIgnore
    public StringProperty zelloAlertFileProperty()
    {
        return mZelloAlertFileProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "enableZelloTextMessage")
    public boolean isEnableZelloTextMessage()
    {
        return mEnableZelloTextMessageProperty.get();
    }

    public void setEnableZelloTextMessage(boolean enableZelloTextMessage)
    {
        mEnableZelloTextMessageProperty.set(enableZelloTextMessage);
    }

    @JsonIgnore
    public BooleanProperty enableZelloTextMessageProperty()
    {
        return mEnableZelloTextMessageProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "frequencyTolerance")
    public double getFrequencyTolerance()
    {
        return mFrequencyToleranceProperty.get();
    }

    public void setFrequencyTolerance(double frequencyTolerance)
    {
        mFrequencyToleranceProperty.set(frequencyTolerance);
    }

    @JsonIgnore
    public DoubleProperty frequencyToleranceProperty()
    {
        return mFrequencyToleranceProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneDurationMs")
    public double getToneDurationMs()
    {
        return mToneDurationMsProperty.get();
    }

    public void setToneDurationMs(double toneDurationMs)
    {
        mToneDurationMsProperty.set(toneDurationMs);
    }

    @JsonIgnore
    public DoubleProperty toneDurationMsProperty()
    {
        return mToneDurationMsProperty;
    }

    public static Callback<TwoToneConfiguration, Observable[]> extractor()
    {
        return (TwoToneConfiguration config) -> new Observable[]{
            config.aliasProperty(), config.templateProperty(), config.longAToneProperty(), config.zelloChannelProperty(), config.enableMqttPublishProperty(), config.mqttTopicProperty(), config.mqttPayloadProperty(), config.enableZelloAlertProperty(), config.zelloAlertFileProperty(), config.enableZelloTextMessageProperty(), config.frequencyToleranceProperty(), config.toneDurationMsProperty(), config.toneAProperty(), config.toneBProperty()
        };
    }
}
