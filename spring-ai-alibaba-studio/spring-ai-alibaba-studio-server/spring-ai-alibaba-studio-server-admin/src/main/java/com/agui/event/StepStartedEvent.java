package com.agui.event;

import com.agui.types.EventType;

/**
 * StepStarted event according to AG-UI specification
 * 
 * Signals the start of a step within an agent run. This event indicates that
 * the agent is beginning a specific subtask or phase of its processing.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class StepStartedEvent extends BaseEvent {

    private String stepName;

    public StepStartedEvent() {
        super(EventType.STEP_STARTED);
    }

    public String getStepName() {
        return this.stepName;
    }

    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }
}
