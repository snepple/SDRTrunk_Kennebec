package io.github.dsheirer.alias.action.twotone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasAction;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.message.IMessage;

public class TwoToneAction extends AliasAction {
    private String mDetectorName;

    public TwoToneAction() {
    }

    public TwoToneAction(String detectorName) {
        mDetectorName = detectorName;
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "detectorName")
    public String getDetectorName() {
        return mDetectorName;
    }

    public void setDetectorName(String detectorName) {
        mDetectorName = detectorName;
        updateValueProperty();
    }

    @Override
    @JsonIgnore
    public AliasActionType getType() {
        return AliasActionType.TWO_TONE;
    }

    @Override
    public void execute(Alias alias, IMessage message) {
        // Audio routing is handled dynamically by TwoToneDetector intercepting the stream based on this action
    }

    @Override
    public void dismiss(boolean reset) {
        // No persistent state to dismiss
    }

    @Override
    public String toString() {
        return "Two-Tone Decoder: " + (mDetectorName != null ? mDetectorName : "None");
    }
}
