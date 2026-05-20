package io.github.dsheirer.module.decode.event;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodeEventModel extends ClearableHistoryModel<IDecodeEvent> implements Listener<IDecodeEvent>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventModel.class);

    public DecodeEventModel()
    {
        MyEventBus.getGlobalEventBus().register(this);
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DECODE_EVENT || preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            Platform.runLater(() -> {
                if (!getItems().isEmpty()) {
                    IDecodeEvent item = getItems().get(0);
                    getItems().set(0, item);
                }
            });
        }
    }

    public void receive(final IDecodeEvent event)
    {
        Platform.runLater(() -> add(event));
    }
}
