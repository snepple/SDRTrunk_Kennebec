package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;

public class MessageActivityModel extends ClearableHistoryModel<MessageItem> implements Listener<IMessage>
{
    private static final long serialVersionUID = 1L;

    public MessageActivityModel()
    {
    }

    public void receive(final IMessage message)
    {
        if(message instanceof StuffBitsMessage)
        {
            return;
        }

        Platform.runLater(() -> add(new MessageItem(message)));
    }
}
