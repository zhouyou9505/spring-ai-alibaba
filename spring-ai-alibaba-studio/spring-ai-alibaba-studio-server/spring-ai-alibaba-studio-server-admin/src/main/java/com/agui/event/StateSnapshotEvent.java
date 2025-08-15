package com.agui.event;

import com.agui.types.EventType;

/**
 * StateSnapshot event according to AG-UI specification
 * 
 * Provides a complete snapshot of an agent's state. This event delivers a comprehensive
 * representation of the agent's current state.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class StateSnapshotEvent extends BaseEvent {

    private Object snapshot;

    public StateSnapshotEvent() {
        super(EventType.STATE_SNAPSHOT);
    }

    public Object getSnapshot() {
        return this.snapshot;
    }

    public void setSnapshot(final Object snapshot) {
        this.snapshot = snapshot;
    }
}
