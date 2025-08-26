package com.alibaba.cloud.ai.graph.event.manager;

import com.alibaba.cloud.ai.graph.event.event.BaseEvent;

import java.util.function.Consumer;

/**
 * Event handler interface for processing events.
 * <p>
 * This interface provides a mechanism for handling events through a consumer pattern.
 * The default implementation simply forwards events to the provided consumer.
 * </p>
 */
public class EventHandler {

    
	private Consumer<BaseEvent> consumer;

	public EventHandler(Consumer<BaseEvent> consumer) {
		this.consumer = consumer;
	}

    /**
     * 发送事件
     * @param event 事件对象
     */
     void emitEvent(BaseEvent event) {
        if (event != null && consumer != null) {
            consumer.accept(event);
        }
    }
}
