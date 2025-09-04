package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents the completion of a specific step within a process or
 * workflow.
 * <p>
 * This event is fired when an individual step in a larger execution sequence completes
 * successfully. It provides granular tracking of progress within multi-step operations,
 * allowing for detailed monitoring and debugging of complex workflows.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#STEP_FINISHED} and captures
 * the name of the completed step for identification purposes.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#STEP_FINISHED
 * @see StepStartedEvent
 * @author Pascal Wilbrink
 */
public class StepFinishedEvent extends BaseEvent {

	private String stepName;

	private String stepId;

	/**
	 * Creates a new StepFinishedEvent with type set to {@link EventType#STEP_FINISHED}.
	 * <p>
	 * The timestamp is automatically set to the current time and the step name is
	 * initialized as null.
	 * </p>
	 */
	public StepFinishedEvent() {
		super(EventType.STEP_FINISHED);
	}

	/**
	 * Sets the name of the step that has finished.
	 * @param stepName the name of the completed step. Can be null.
	 */
	public void setStepName(final String stepName) {
		this.stepName = stepName;
	}

	/**
	 * Returns the name of the step that has finished.
	 * @return the name of the completed step, can be null
	 */
	public String getStepName() {
		return this.stepName;
	}

	/**
	 * Sets the unique identifier for this step.
	 * @param stepId the step identifier. Can be null.
	 */
	public void setStepId(final String stepId) {
		this.stepId = stepId;
	}

	/**
	 * Returns the unique identifier for this step.
	 * @return the step identifier, can be null
	 */
	public String getStepId() {
		return this.stepId;
	}

}