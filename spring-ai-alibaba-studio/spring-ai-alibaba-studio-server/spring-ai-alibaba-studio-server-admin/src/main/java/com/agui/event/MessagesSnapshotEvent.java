package com.agui.event;

import com.agui.message.BaseMessage;
import com.agui.types.EventType;

import java.util.List;

/**
 * MessagesSnapshot event according to AG-UI specification
 * 
 * Provides a snapshot of all messages in a conversation. This event delivers
 * a complete history of messages in the current conversation.
 * 
 * @see <a href="https://docs.ag-ui.com/concepts/events">AG-UI Events Documentation</a>
 */
public class MessagesSnapshotEvent extends BaseEvent {

    private List<BaseMessage> messages;

    public MessagesSnapshotEvent() {
        super(EventType.MESSAGES_SNAPSHOT);
    }

    public List<BaseMessage> getMessages() {
        return this.messages;
    }

    public void setMessages(final List<BaseMessage> messages) {
        this.messages = messages;
    }
}
