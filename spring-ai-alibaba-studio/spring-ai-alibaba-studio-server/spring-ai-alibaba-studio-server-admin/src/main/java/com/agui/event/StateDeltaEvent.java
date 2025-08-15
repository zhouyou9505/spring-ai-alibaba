package com.agui.event;

import com.agui.types.EventType;

/**
 * StateDelta event according to AG-UI specification
 * 
 * Provides a partial update to an agent's state using JSON Patch operations
 * (as defined in RFC 6902). Each delta represents specific changes to apply
 * to the current state model.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class StateDeltaEvent extends BaseEvent {

    private Object delta;

    public StateDeltaEvent() {
        super(EventType.STATE_DELTA);
    }

    public Object getDelta() {
        return this.delta;
    }

    public void setDelta(final Object delta) {
        this.delta = delta;
    }
}
