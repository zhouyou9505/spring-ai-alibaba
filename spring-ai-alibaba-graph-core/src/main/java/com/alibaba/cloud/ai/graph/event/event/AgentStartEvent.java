package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents the initiation of an agent execution.
 * <p>
 * This event is fired when an agent begins its execution lifecycle. It provides
 * essential tracking information including agent identification and context to
 * enable monitoring and correlation with subsequent events in the agent's execution.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#AGENT_START} and provides
 * fields to store agent metadata for tracking purposes.
 * </p>
 *
 * @see com.alibaba.cloud.ai.graph.event.event.BaseEvent
 * @see EventType#AGENT_START
 * @see AgentFinishedEvent
 * @author System Generated
 */
public class AgentStartEvent extends BaseEvent {

	private String agentId;

	private String agentName;

	/**
	 * Creates a new AgentStartEvent with type set to {@link EventType#AGENT_START}.
	 * <p>
	 * The timestamp is automatically set to the current time and all identifier fields
	 * are initialized as null.
	 * </p>
	 */
	public AgentStartEvent() {
		super(EventType.AGENT_START);
	}

	/**
	 * Sets the unique identifier for the agent that is starting.
	 * @param agentId the agent identifier. Can be null.
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}

	/**
	 * Returns the unique identifier for the agent that is starting.
	 * @return the agent identifier, can be null
	 */
	public String getAgentId() {
		return this.agentId;
	}

	/**
	 * Sets the name of the agent that is starting.
	 * @param agentName the agent name. Can be null.
	 */
	public void setAgentName(final String agentName) {
		this.agentName = agentName;
	}

	/**
	 * Returns the name of the agent that is starting.
	 * @return the agent name, can be null
	 */
	public String getAgentName() {
		return this.agentName;
	}


}