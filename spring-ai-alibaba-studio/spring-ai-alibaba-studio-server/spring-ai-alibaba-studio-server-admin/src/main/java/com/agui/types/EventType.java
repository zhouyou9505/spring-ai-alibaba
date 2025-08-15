package com.agui.types;

/**
 * AG-UI Event Types according to official specification
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public enum EventType {
    // Lifecycle Events - Monitor the progression of agent runs
    RUN_STARTED("RUN_STARTED"),
    RUN_FINISHED("RUN_FINISHED"),
    RUN_ERROR("RUN_ERROR"),
    STEP_STARTED("STEP_STARTED"),
    STEP_FINISHED("STEP_FINISHED"),
    
    // Text Message Events - Handle streaming textual content
    TEXT_MESSAGE_START("TEXT_MESSAGE_START"),
    TEXT_MESSAGE_CONTENT("TEXT_MESSAGE_CONTENT"),
    TEXT_MESSAGE_END("TEXT_MESSAGE_END"),
    
    // Tool Call Events - Manage tool executions by agents
    TOOL_CALL_START("TOOL_CALL_START"),
    TOOL_CALL_ARGS("TOOL_CALL_ARGS"),
    TOOL_CALL_END("TOOL_CALL_END"),
    TOOL_CALL_RESULT("TOOL_CALL_RESULT"),
    
    // State Management Events - Synchronize state between agents and UI
    STATE_SNAPSHOT("STATE_SNAPSHOT"),
    STATE_DELTA("STATE_DELTA"),
    MESSAGES_SNAPSHOT("MESSAGES_SNAPSHOT"),
    
    // Special Events - Support custom functionality
    RAW("RAW"),
    CUSTOM("CUSTOM");

    private final String name;

    EventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
