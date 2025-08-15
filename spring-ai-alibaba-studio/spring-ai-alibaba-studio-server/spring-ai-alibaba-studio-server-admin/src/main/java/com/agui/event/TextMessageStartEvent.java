package com.agui.event;

import com.agui.types.EventType;

public class TextMessageStartEvent extends BaseEvent {

    private String messageId;
    private String role;

    public TextMessageStartEvent(String messageId, String role) {
        super(EventType.TEXT_MESSAGE_START);
        this.messageId = messageId;
        this.role = role;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(final String role) {
        this.role = role;
    }
}
