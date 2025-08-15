package com.agui.event;

import com.agui.types.EventType;

/**
 * StepFinished event according to AG-UI specification
 * 
 * Signals the completion of a step within an agent run. This event indicates that
 * the agent has completed a specific subtask or phase.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class StepFinishedEvent extends BaseEvent {

    private String stepName;

    public StepFinishedEvent() {
        super(EventType.STEP_FINISHED);
    }

    public String getStepName() {
        return this.stepName;
    }

    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }
}
