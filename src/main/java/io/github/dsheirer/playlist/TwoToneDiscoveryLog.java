package io.github.dsheirer.playlist;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Log entry for an unknown two-tone sequence
 */
public class TwoToneDiscoveryLog
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringProperty mTimestampProperty = new SimpleStringProperty();
    private final StringProperty mToneAProperty = new SimpleStringProperty();
    private final StringProperty mToneBProperty = new SimpleStringProperty();

    public TwoToneDiscoveryLog(double toneA, double toneB)
    {
        mTimestampProperty.set(LocalDateTime.now().format(FORMATTER));
        mToneAProperty.set(String.format("%.2f", toneA));
        mToneBProperty.set(String.format("%.2f", toneB));
    }

    public StringProperty timestampProperty()
    {
        return mTimestampProperty;
    }

    public StringProperty toneAProperty()
    {
        return mToneAProperty;
    }

    public StringProperty toneBProperty()
    {
        return mToneBProperty;
    }
}
