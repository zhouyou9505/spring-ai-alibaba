package com.agui.event;

import com.agui.types.EventType;

/**
 * Custom event according to AG-UI specification
 * 
 * Used for application-specific custom events. This event provides an extension
 * mechanism for implementing features not covered by the standard event types.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class CustomEvent extends BaseEvent {

    private String name;
    private Object value;

    public CustomEvent() {
        super(EventType.CUSTOM);
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }
}
