package com.alibaba.cloud.ai.graph.event.manager;

import com.alibaba.cloud.ai.graph.event.event.*;

/**
 * Implementation of CallbackManager interface.
 * <p>
 * This class implements all event handling methods by delegating to the EventHandler.
 * Each method directly calls eventHandler.emitEvent() without any try-catch blocks.
 * </p>
 */
public class CallbackManagerImpl implements CallbackManager {
    
    private final EventHandler eventHandler;
    
    public CallbackManagerImpl(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
    
    // 执行生命周期事件
    @Override
    public void onRunStartedEvent(RunStartedEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onRunFinishedEvent(RunFinishedEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onRunErrorEvent(RunErrorEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 步骤执行事件
    @Override
    public void onStepStartedEvent(StepStartedEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onStepFinishedEvent(StepFinishedEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 文本消息事件
    @Override
    public void onTextMessageStartEvent(TextMessageStartEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onTextMessageContentEvent(TextMessageContentEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onTextMessageEndEvent(TextMessageEndEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onTextMessageChunkEvent(TextMessageChunkEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 思考过程事件
    @Override
    public void onThinkingStartEvent(ThinkingStartEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onThinkingEndEvent(ThinkingEndEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onThinkingTextMessageStartEvent(ThinkingTextMessageStartEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onThinkingTextMessageContentEvent(ThinkingTextMessageContentEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onThinkingTextMessageEndEvent(ThinkingTextMessageEndEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 工具调用事件
    @Override
    public void onToolCallStartEvent(ToolCallStartEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onToolCallArgsEvent(ToolCallArgsEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onToolCallEndEvent(ToolCallEndEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onToolCallChunkEvent(ToolCallChunkEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onToolCallResultEvent(ToolCallResultEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 状态管理事件
    @Override
    public void onStateSnapshotEvent(StateSnapshotEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onStateDeltaEvent(StateDeltaEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onMessagesSnapshotEvent(MessagesSnapshotEvent event) {
        eventHandler.emitEvent(event);
    }
    
    // 通用事件
    @Override
    public void onRawEvent(RawEvent event) {
        eventHandler.emitEvent(event);
    }
    
    @Override
    public void onCustomEvent(CustomEvent event) {
        eventHandler.emitEvent(event);
    }
}
