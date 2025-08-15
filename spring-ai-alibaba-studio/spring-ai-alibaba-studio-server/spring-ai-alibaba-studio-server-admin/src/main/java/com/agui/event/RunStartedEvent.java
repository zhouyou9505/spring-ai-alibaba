package com.agui.event;

import com.agui.types.EventType;

/**
 * RunStarted event according to AG-UI specification
 * 
 * Signals the start of an agent run. This event establishes a new execution context
 * identified by a unique runId. It serves as a marker for frontends to initialize
 * UI elements such as progress indicators or loading states.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class RunStartedEvent extends BaseEvent {

    private String threadId;
    private String runId;

    public RunStartedEvent() {
        super(EventType.RUN_STARTED);
    }

    public String getThreadId() {
        return this.threadId;
    }

    public void setThreadId(final String threadId) {
        this.threadId = threadId;
    }

    public String getRunId() {
        return this.runId;
    }

    public void setRunId(final String runId) {
        this.runId = runId;
    }
}
