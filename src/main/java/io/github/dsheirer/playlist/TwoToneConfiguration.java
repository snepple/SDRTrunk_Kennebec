package io.github.dsheirer.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
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
    //Multiple Zello channels (stream names) to send the text message and alert tone to when this detector fires.
    //The legacy single-channel field (mZelloChannelProperty) is retained for backward compatibility with playlists
    //saved before multi-channel support; see getEffectiveZelloChannels().
    private List<String> mZelloChannels = new ArrayList<>();
    private DoubleProperty mToneAProperty = new SimpleDoubleProperty(0.0);
    private DoubleProperty mToneBProperty = new SimpleDoubleProperty(0.0);
    private BooleanProperty mLongAToneProperty = new SimpleBooleanProperty(false);
    private BooleanProperty mEnableMqttPublishProperty = new SimpleBooleanProperty(false);
    private StringProperty mMqttTopicProperty = new SimpleStringProperty("");
    private StringProperty mMqttPayloadProperty = new SimpleStringProperty("{\"detector\": \"[DetectorName]\", \"state\": \"ON\", \"time\": \"[Timestamp]\"}");

    private BooleanProperty mEnableZelloAlertProperty = new SimpleBooleanProperty(false);
    private StringProperty mZelloAlertFileProperty = new SimpleStringProperty("");
    private BooleanProperty mEnableZelloTextMessageProperty = new SimpleBooleanProperty(true);

    private BooleanProperty mEnabledProperty = new SimpleBooleanProperty(true);
    private StringProperty mAlertFilePathProperty = new SimpleStringProperty("");
    private BooleanProperty mShowNotificationProperty = new SimpleBooleanProperty(true);

    private DoubleProperty mFrequencyToleranceProperty = new SimpleDoubleProperty(10.0);
    private DoubleProperty mToneDurationMsProperty = new SimpleDoubleProperty(300.0);

    //Default minimum tone hold durations (the editable per-detector field values).  A tone must be held for at least
    //this long to count, which mitigates voice triggering / false positives: human speech is highly harmonic but
    //cannot hold a constant fundamental for these durations.  Tuned for standard QCII timing: a ~1.0s Tone A -> 0.6s
    //minimum, a ~3.0s Tone B -> 1.5s minimum, and an ~8.0s single long tone (group call) -> 4.5s minimum.  Users can
    //change these per detector; MINIMUM_TONE_DURATION_FLOOR_MS is enforced by the detector so no tone is ever gated
    //below 500ms.
    public static final double DEFAULT_TONE_A_LENGTH_SEC = 0.6;   //two-tone A default
    public static final double DEFAULT_TONE_B_LENGTH_SEC = 1.5;   //two-tone B default
    public static final double DEFAULT_LONG_TONE_LENGTH_SEC = 2.0; //single long-tone A default
    public static final double MINIMUM_TONE_DURATION_FLOOR_MS = 500.0;

    // IAmResponding-style per-tone configuration fields
    private DoubleProperty mToneALengthSecProperty = new SimpleDoubleProperty(DEFAULT_TONE_A_LENGTH_SEC);
    private DoubleProperty mToneBLengthSecProperty = new SimpleDoubleProperty(DEFAULT_TONE_B_LENGTH_SEC);
    private DoubleProperty mToneGapLengthSecProperty = new SimpleDoubleProperty(0.0);
    private DoubleProperty mToneToleranceProperty = new SimpleDoubleProperty(0.02);
    private DoubleProperty mIgnoreDuplicateSecProperty = new SimpleDoubleProperty(60.0);
    
    private BooleanProperty mAutoDiscoveredProperty = new SimpleBooleanProperty(false);

    //RF channel frequency (Hz) this detector was AI-discovered on, and the raw ASR transcript that led to the
    //AI's name deduction.  Captured for the human-review package and as part of the tombstone primary key; 0/empty
    //for manually created detectors.
    private DoubleProperty mDiscoveryFrequencyProperty = new SimpleDoubleProperty(0.0);
    private StringProperty mDiscoveryTranscriptProperty = new SimpleStringProperty("");

    public TwoToneConfiguration()
    {
    }

    public TwoToneConfiguration copyOf()
    {
        TwoToneConfiguration copy = new TwoToneConfiguration();
        copy.setAlias(getAlias());
        copy.setTemplate(getTemplate());
        copy.setZelloChannel(getZelloChannel());
        copy.setZelloChannels(getZelloChannels());
        copy.setToneA(getToneA());
        copy.setToneB(getToneB());
        copy.setLongATone(isLongATone());
        copy.setEnableMqttPublish(isEnableMqttPublish());
        copy.setMqttTopic(getMqttTopic());
        copy.setMqttPayload(getMqttPayload());
        copy.setEnableZelloAlert(isEnableZelloAlert());
        copy.setZelloAlertFile(getZelloAlertFile());
        copy.setEnableZelloTextMessage(isEnableZelloTextMessage());
        copy.setEnabled(isEnabled());
        copy.setAlertFilePath(getAlertFilePath());
        copy.setShowNotification(isShowNotification());
        copy.setFrequencyTolerance(getFrequencyTolerance());
        copy.setToneDurationMs(getToneDurationMs());
        copy.setToneALengthSec(getToneALengthSec());
        copy.setToneBLengthSec(getToneBLengthSec());
        copy.setToneGapLengthSec(getToneGapLengthSec());
        copy.setToneTolerance(getToneTolerance());
        copy.setIgnoreDuplicateSec(getIgnoreDuplicateSec());
        copy.setAutoDiscovered(isAutoDiscovered());
        copy.setDiscoveryFrequency(getDiscoveryFrequency());
        copy.setDiscoveryTranscript(getDiscoveryTranscript());
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

    /**
     * Zello channels (broadcast stream names) that this detector sends text messages and alert audio to when it
     * fires.  Serialized as repeated {@code <zelloChannelEntry>} elements.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "zelloChannelEntry")
    public List<String> getZelloChannels()
    {
        return mZelloChannels;
    }

    public void setZelloChannels(List<String> zelloChannels)
    {
        mZelloChannels = (zelloChannels != null) ? new ArrayList<>(zelloChannels) : new ArrayList<>();
    }

    /**
     * Resolves the effective set of Zello channels (stream names) to alert.  Prefers the multi-channel list and falls
     * back to the legacy single-channel field so that playlists saved before multi-channel support continue to work.
     * @return ordered, de-duplicated list of non-empty channel names (may be empty)
     */
    @JsonIgnore
    public List<String> getEffectiveZelloChannels()
    {
        List<String> result = new ArrayList<>();

        if(mZelloChannels != null)
        {
            for(String channel : mZelloChannels)
            {
                if(channel != null && !channel.trim().isEmpty() && !result.contains(channel))
                {
                    result.add(channel);
                }
            }
        }

        String legacy = getZelloChannel();

        if(result.isEmpty() && legacy != null && !legacy.trim().isEmpty())
        {
            result.add(legacy);
        }

        return result;
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

    @JacksonXmlProperty(isAttribute = true, localName = "toneALengthSec")
    public double getToneALengthSec()
    {
        return mToneALengthSecProperty.get();
    }

    public void setToneALengthSec(double toneALengthSec)
    {
        mToneALengthSecProperty.set(toneALengthSec);
    }

    @JsonIgnore
    public DoubleProperty toneALengthSecProperty()
    {
        return mToneALengthSecProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneBLengthSec")
    public double getToneBLengthSec()
    {
        return mToneBLengthSecProperty.get();
    }

    public void setToneBLengthSec(double toneBLengthSec)
    {
        mToneBLengthSecProperty.set(toneBLengthSec);
    }

    @JsonIgnore
    public DoubleProperty toneBLengthSecProperty()
    {
        return mToneBLengthSecProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneGapLengthSec")
    public double getToneGapLengthSec()
    {
        return mToneGapLengthSecProperty.get();
    }

    public void setToneGapLengthSec(double toneGapLengthSec)
    {
        mToneGapLengthSecProperty.set(toneGapLengthSec);
    }

    @JsonIgnore
    public DoubleProperty toneGapLengthSecProperty()
    {
        return mToneGapLengthSecProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "toneTolerance")
    public double getToneTolerance()
    {
        return mToneToleranceProperty.get();
    }

    public void setToneTolerance(double toneTolerance)
    {
        mToneToleranceProperty.set(toneTolerance);
    }

    @JsonIgnore
    public DoubleProperty toneToleranceProperty()
    {
        return mToneToleranceProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "ignoreDuplicateSec")
    public double getIgnoreDuplicateSec()
    {
        return mIgnoreDuplicateSecProperty.get();
    }

    public void setIgnoreDuplicateSec(double ignoreDuplicateSec)
    {
        mIgnoreDuplicateSecProperty.set(ignoreDuplicateSec);
    }

    @JsonIgnore
    public DoubleProperty ignoreDuplicateSecProperty()
    {
        return mIgnoreDuplicateSecProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "enabled")
    public boolean isEnabled()
    {
        return mEnabledProperty.get();
    }

    public void setEnabled(boolean enabled)
    {
        mEnabledProperty.set(enabled);
    }

    @JsonIgnore
    public BooleanProperty enabledProperty()
    {
        return mEnabledProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "alertFilePath")
    public String getAlertFilePath()
    {
        return mAlertFilePathProperty.get();
    }

    public void setAlertFilePath(String alertFilePath)
    {
        mAlertFilePathProperty.set(alertFilePath);
    }

    @JsonIgnore
    public StringProperty alertFilePathProperty()
    {
        return mAlertFilePathProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "showNotification")
    public boolean isShowNotification()
    {
        return mShowNotificationProperty.get();
    }

    public void setShowNotification(boolean showNotification)
    {
        mShowNotificationProperty.set(showNotification);
    }

    @JsonIgnore
    public BooleanProperty showNotificationProperty()
    {
        return mShowNotificationProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "autoDiscovered")
    public boolean isAutoDiscovered()
    {
        return mAutoDiscoveredProperty.get();
    }

    public void setAutoDiscovered(boolean autoDiscovered)
    {
        mAutoDiscoveredProperty.set(autoDiscovered);
    }

    @JsonIgnore
    public BooleanProperty autoDiscoveredProperty()
    {
        return mAutoDiscoveredProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "discoveryFrequency")
    public double getDiscoveryFrequency()
    {
        return mDiscoveryFrequencyProperty.get();
    }

    public void setDiscoveryFrequency(double discoveryFrequency)
    {
        mDiscoveryFrequencyProperty.set(discoveryFrequency);
    }

    @JsonIgnore
    public DoubleProperty discoveryFrequencyProperty()
    {
        return mDiscoveryFrequencyProperty;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "discoveryTranscript")
    public String getDiscoveryTranscript()
    {
        return mDiscoveryTranscriptProperty.get();
    }

    public void setDiscoveryTranscript(String discoveryTranscript)
    {
        mDiscoveryTranscriptProperty.set(discoveryTranscript != null ? discoveryTranscript : "");
    }

    @JsonIgnore
    public StringProperty discoveryTranscriptProperty()
    {
        return mDiscoveryTranscriptProperty;
    }

    //Detection history: epoch-millis timestamps of each time this detector fired, most-recent first.  Bounded to the
    //most recent MAX_DETECTION_HISTORY entries and persisted with the playlist (serialized as repeated
    //<detectionEntry> elements) so the history survives application restarts.  The list is synchronized because it is
    //appended from the detector processing thread while the JavaFX thread reads it for display.
    private static final int MAX_DETECTION_HISTORY = 200;
    private final List<Long> mDetectionHistory = java.util.Collections.synchronizedList(new ArrayList<>());
    //Channel/alias name each detection was heard on, head-aligned with mDetectionHistory (index 0 = most recent) and
    //guarded by the same monitor (mDetectionHistory).  Serialized as repeated <detectionChannelEntry> elements.
    //Playlists saved before this field have no channel entries, so older detections simply display with no channel;
    //getDetectionChannel(index) returns "" when an entry predates this field.
    private final List<String> mDetectionChannels = new ArrayList<>();

    @JacksonXmlProperty(isAttribute = false, localName = "detectionEntry")
    public List<Long> getDetectionHistory()
    {
        //Return a snapshot copy so serialization (and UI reads) cannot throw a ConcurrentModificationException while
        //the detector thread appends a new detection.  Deserialization populates the list via setDetectionHistory().
        synchronized(mDetectionHistory)
        {
            return new ArrayList<>(mDetectionHistory);
        }
    }

    public void setDetectionHistory(List<Long> detectionHistory)
    {
        synchronized(mDetectionHistory)
        {
            mDetectionHistory.clear();

            if(detectionHistory != null)
            {
                mDetectionHistory.addAll(detectionHistory);
            }
        }
    }

    @JacksonXmlProperty(isAttribute = false, localName = "detectionChannelEntry")
    public List<String> getDetectionChannels()
    {
        synchronized(mDetectionHistory)
        {
            return new ArrayList<>(mDetectionChannels);
        }
    }

    public void setDetectionChannels(List<String> detectionChannels)
    {
        synchronized(mDetectionHistory)
        {
            mDetectionChannels.clear();

            if(detectionChannels != null)
            {
                mDetectionChannels.addAll(detectionChannels);
            }
        }
    }

    /**
     * Returns the channel/alias name for the detection at the given history index, or "" if unknown (e.g. an entry
     * recorded before channel history was tracked).
     */
    @JsonIgnore
    public String getDetectionChannel(int index)
    {
        synchronized(mDetectionHistory)
        {
            return (index >= 0 && index < mDetectionChannels.size()) ? mDetectionChannels.get(index) : "";
        }
    }

    /**
     * Records a two-tone detection event for this detector.  The timestamp and channel name are inserted at the head
     * so the most recent detection is always first, and both lists are trimmed to the most recent
     * MAX_DETECTION_HISTORY entries.  Thread-safe: called from the detector processing thread while the UI thread may
     * read the lists.
     * @param epochMillis the detection time, in epoch milliseconds (e.g. System.currentTimeMillis())
     * @param channelName the channel/alias name the tones were heard on (null/empty if unknown)
     */
    public void recordDetection(long epochMillis, String channelName)
    {
        synchronized(mDetectionHistory)
        {
            mDetectionHistory.add(0, epochMillis);
            mDetectionChannels.add(0, channelName != null ? channelName : "");

            while(mDetectionHistory.size() > MAX_DETECTION_HISTORY)
            {
                mDetectionHistory.remove(mDetectionHistory.size() - 1);
            }
            while(mDetectionChannels.size() > MAX_DETECTION_HISTORY)
            {
                mDetectionChannels.remove(mDetectionChannels.size() - 1);
            }
        }
    }

    /**
     * Backward-compatible overload for callers that do not know the channel name.
     * @param epochMillis the detection time, in epoch milliseconds
     */
    public void recordDetection(long epochMillis)
    {
        recordDetection(epochMillis, null);
    }

    /**
     * Clears all recorded detection history for this detector.
     */
    public void clearDetectionHistory()
    {
        synchronized(mDetectionHistory)
        {
            mDetectionHistory.clear();
            mDetectionChannels.clear();
        }
    }

    // ---- Convenience methods: prefer new fields, fall back to legacy ----

    @JsonIgnore
    public double getEffectiveToneADurationMs()
    {
        double sec = getToneALengthSec();
        return sec > 0 ? sec * 1000.0 : getToneDurationMs();
    }

    @JsonIgnore
    public double getEffectiveToneBDurationMs()
    {
        double sec = getToneBLengthSec();
        return sec > 0 ? sec * 1000.0 : getToneDurationMs();
    }

    @JsonIgnore
    public double getEffectiveToleranceHz(double frequency)
    {
        double fractional = getToneTolerance();
        return fractional > 0 ? frequency * fractional : getFrequencyTolerance();
    }

    public static Callback<TwoToneConfiguration, Observable[]> extractor()
    {
        return (TwoToneConfiguration config) -> new Observable[]{
            config.aliasProperty(), config.templateProperty(), config.longAToneProperty(),
            config.zelloChannelProperty(), config.enableMqttPublishProperty(),
            config.mqttTopicProperty(), config.mqttPayloadProperty(),
            config.enableZelloAlertProperty(), config.zelloAlertFileProperty(),
            config.enableZelloTextMessageProperty(), config.frequencyToleranceProperty(),
            config.toneDurationMsProperty(), config.toneAProperty(), config.toneBProperty(),
            config.enabledProperty(), config.alertFilePathProperty(),
            config.showNotificationProperty(), config.autoDiscoveredProperty(),
            config.toneALengthSecProperty(), config.toneBLengthSecProperty(),
            config.toneGapLengthSecProperty(), config.toneToleranceProperty(),
            config.ignoreDuplicateSecProperty()
        };
    }
}
