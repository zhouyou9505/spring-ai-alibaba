package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents a complete snapshot of the application state.
 * <p>
 * This event is fired when a full capture of the current system state is needed,
 * typically for synchronization, debugging, or state restoration purposes. Unlike delta
 * events, this represents the complete state at a specific point in time.
 * </p>
 */
public class StateSnapshotEvent extends BaseEvent {

	/**
	 * Creates a new StateSnapshotEvent with type set to {@link EventType#STATE_SNAPSHOT}.
	 * <p>
	 * The timestamp is automatically set to the current time.
	 * </p>
	 */
	public StateSnapshotEvent() {
		super(EventType.STATE_SNAPSHOT);
	}

}