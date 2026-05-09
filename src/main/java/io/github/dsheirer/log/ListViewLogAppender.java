package io.github.dsheirer.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.github.dsheirer.eventbus.MyEventBus;

public class ListViewLogAppender extends AppenderBase<ILoggingEvent> {

    public ListViewLogAppender(String name) {
        setName(name);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        eventObject.prepareForDeferredProcessing();
        MyEventBus.getGlobalEventBus().post(eventObject);
    }
}
