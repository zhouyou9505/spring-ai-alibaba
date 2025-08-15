package com.agui.event;

import com.agui.types.EventType;

/**
 * RunFinished event according to AG-UI specification
 * 
 * Signals the successful completion of an agent run. This event indicates that
 * an agent has successfully completed all its work for the current run.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class RunFinishedEvent extends BaseEvent {

    private String threadId;
    private String runId;
    private Object result;

    public RunFinishedEvent() {
        super(EventType.RUN_FINISHED);
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

    public Object getResult() {
        return this.result;
    }

    public void setResult(final Object result) {
        this.result = result;
    }
}
