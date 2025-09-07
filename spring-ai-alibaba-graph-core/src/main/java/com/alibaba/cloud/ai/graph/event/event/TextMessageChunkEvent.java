/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.event.event;

import com.alibaba.cloud.ai.graph.event.type.EventType;

/**
 * An event that represents a chunk of text message content with role information.
 * <p>
 * This event is used to deliver incremental content updates for text messages,
 * including both the content delta and the role of the message sender. This is
 * similar to {@link TextMessageContentEvent} but includes role information.
 * </p>
 * <p>
 * The event automatically sets its type to {@link EventType#TEXT_MESSAGE_CHUNK} and
 * provides fields to identify the target message, sender role, and content delta.
 * </p>
 *
 * @see BaseEvent
 * @see EventType#TEXT_MESSAGE_CHUNK
 * @see TextMessageContentEvent
 */
public class TextMessageChunkEvent extends BaseEvent {

    private String messageId;
    private String role;
    private String delta;

    /**
     * Creates a new TextMessageChunkEvent with type set to {@link EventType#TEXT_MESSAGE_CHUNK}.
     * <p>
     * The timestamp is automatically set to the current time and all fields
     * are initialized as null.
     * </p>
     */
    public TextMessageChunkEvent() {
        super(EventType.TEXT_MESSAGE_CHUNK);
    }

    /**
     * Sets the unique identifier of the message this chunk belongs to.
     *
     * @param messageId the message identifier. Can be null.
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    /**
     * Returns the unique identifier of the message this chunk belongs to.
     *
     * @return the message identifier, can be null
     */
    public String getMessageId() {
        return this.messageId;
    }

    /**
     * Sets the role of the message sender (e.g., "user", "assistant", "system").
     *
     * @param role the sender role. Can be null.
     */
    public void setRole(final String role) {
        this.role = role;
    }

    /**
     * Returns the role of the message sender.
     *
     * @return the sender role, can be null
     */
    public String getRole() {
        return this.role;
    }

    /**
     * Sets the incremental text content for this chunk.
     *
     * @param delta the text content delta/chunk. Can be null.
     */
    public void setDelta(final String delta) {
        this.delta = delta;
    }

    /**
     * Returns the incremental text content for this chunk.
     *
     * @return the text content delta/chunk, can be null
     */
    public String getDelta() {
        return this.delta;
    }
}