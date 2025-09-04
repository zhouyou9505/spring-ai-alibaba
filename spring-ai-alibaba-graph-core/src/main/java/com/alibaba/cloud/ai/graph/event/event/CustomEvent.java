package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;


public class CustomEvent extends BaseEvent {

	/**
	 * Creates a new CustomEvent with type set to {@link EventType#CUSTOM}.
	 * <p>
	 * The timestamp is automatically set to the current time.
	 * </p>
	 */
	public CustomEvent() {
		super(EventType.CUSTOM);
	}

}