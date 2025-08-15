package com.agui.event;

import com.agui.types.EventType;

/**
 * RunError event according to AG-UI specification
 * 
 * Signals an error during an agent run. This event indicates that the agent
 * encountered an error it could not recover from, causing the run to terminate prematurely.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class RunErrorEvent extends BaseEvent {

    private String message;
    private String code;

    public RunErrorEvent() {
        super(EventType.RUN_ERROR);
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
