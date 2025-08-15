package com.agui.event;

import com.agui.types.EventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base Event class according to AG-UI specification
 * 
 * All events share a common set of base properties:
 * - type: The specific event type identifier
 * - timestamp: Optional timestamp indicating when the event was created
 * - rawEvent: Optional field containing the original event data if transformed
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        // Lifecycle Events
        @JsonSubTypes.Type(value = RunStartedEvent.class, name = "RUN_STARTED"),
        @JsonSubTypes.Type(value = RunFinishedEvent.class, name = "RUN_FINISHED"),
        @JsonSubTypes.Type(value = RunErrorEvent.class, name = "RUN_ERROR"),
        @JsonSubTypes.Type(value = StepStartedEvent.class, name = "STEP_STARTED"),
        @JsonSubTypes.Type(value = StepFinishedEvent.class, name = "STEP_FINISHED"),
        
        // Text Message Events
        @JsonSubTypes.Type(value = TextMessageStartEvent.class, name = "TEXT_MESSAGE_START"),
        @JsonSubTypes.Type(value = TextMessageContentEvent.class, name = "TEXT_MESSAGE_CONTENT"),
        @JsonSubTypes.Type(value = TextMessageEndEvent.class, name = "TEXT_MESSAGE_END"),
        
        // Tool Call Events
        @JsonSubTypes.Type(value = ToolCallStartEvent.class, name = "TOOL_CALL_START"),
        @JsonSubTypes.Type(value = ToolCallArgsEvent.class, name = "TOOL_CALL_ARGS"),
        @JsonSubTypes.Type(value = ToolCallEndEvent.class, name = "TOOL_CALL_END"),
        @JsonSubTypes.Type(value = ToolCallResultEvent.class, name = "TOOL_CALL_RESULT"),
        
        // State Management Events
        @JsonSubTypes.Type(value = StateSnapshotEvent.class, name = "STATE_SNAPSHOT"),
        @JsonSubTypes.Type(value = StateDeltaEvent.class, name = "STATE_DELTA"),
        @JsonSubTypes.Type(value = MessagesSnapshotEvent.class, name = "MESSAGES_SNAPSHOT"),
        
        // Special Events
        @JsonSubTypes.Type(value = CustomEvent.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = RawEvent.class, name = "RAW")
})
public class BaseEvent {

    private final EventType type;
    private Integer timestamp;
    private Object rawEvent;

    public BaseEvent(final EventType type) {
        this.type = type;
    }

    @JsonIgnore
    public EventType getType() {
        return this.type;
    }

    public Integer getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(final Integer timestamp) {
        this.timestamp = timestamp;
    }

    public Object getRawEvent() {
        return this.rawEvent;
    }

    public void setRawEvent(final Object rawEvent) {
        this.rawEvent = rawEvent;
    }
}
