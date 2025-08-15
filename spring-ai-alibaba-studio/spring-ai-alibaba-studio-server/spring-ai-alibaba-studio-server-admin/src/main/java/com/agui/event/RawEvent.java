package com.agui.event;

import com.agui.types.EventType;

/**
 * Raw event according to AG-UI specification
 * 
 * Used to pass through events from external systems. This event acts as a container
 * for events originating from external systems or sources that don't natively follow
 * the Agent UI Protocol.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class RawEvent extends BaseEvent {

    private Object event;
    private String source;

    public RawEvent() {
        super(EventType.RAW);
    }

    public Object getEvent() {
        return this.event;
    }

    public void setEvent(final Object event) {
        this.event = event;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(final String source) {
        this.source = source;
    }
}
