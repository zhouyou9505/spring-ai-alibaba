package com.agui.event;

import com.agui.types.EventType;

/**
 * TextMessageContent event according to AG-UI specification
 * 
 * Represents a chunk of content in a streaming text message.
 * Each event contains a small chunk of text in the delta property
 * that should be appended to previously received chunks.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class TextMessageContentEvent extends BaseEvent {

    private final String messageId;
    private final String delta;

    public TextMessageContentEvent(final String messageId, final String delta) {
        super(EventType.TEXT_MESSAGE_CONTENT);
        this.messageId = messageId;
        this.delta = delta;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getDelta() {
        return this.delta;
    }
}
