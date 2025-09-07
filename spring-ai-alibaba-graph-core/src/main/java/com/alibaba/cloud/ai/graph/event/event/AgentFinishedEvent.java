package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents the completion of an agent execution.
 * <p>
 * This event is fired when an agent finishes its execution lifecycle, either
 * successfully or after reaching completion criteria. It captures the final
 * state and results of the agent's execution to enable monitoring, debugging,
 * and result processing.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#AGENT_FINISH} and provides
 * fields to store completion metadata and execution results.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#AGENT_FINISH
 * @see AgentStartEvent
 * @author System Generated
 */
public class AgentFinishedEvent extends BaseEvent {

	private String agentId;

	private String agentName;

    private String error;

	/**
	 * Creates a new AgentFinishEvent with type set to {@link EventType#AGENT_FINISH}.
	 * <p>
	 * The timestamp is automatically set to the current time and all fields
	 * are initialized as null.
	 * </p>
	 */
	public AgentFinishedEvent() {
		super(EventType.AGENT_FINISH);
	}

	/**
	 * Sets the unique identifier of the agent that has finished.
	 * @param agentId the agent identifier. Can be null.
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}

	/**
	 * Returns the unique identifier of the agent that has finished.
	 * @return the agent identifier, can be null
	 */
	public String getAgentId() {
		return this.agentId;
	}

	/**
	 * Sets the name of the agent that has finished.
	 * @param agentName the agent name. Can be null.
	 */
	public void setAgentName(final String agentName) {
		this.agentName = agentName;
	}

	/**
	 * Returns the name of the agent that has finished.
	 * @return the agent name, can be null
	 */
	public String getAgentName() {
		return this.agentName;
	}

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}