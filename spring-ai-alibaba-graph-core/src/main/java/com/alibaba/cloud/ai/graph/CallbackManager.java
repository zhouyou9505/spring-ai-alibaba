package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.dashscope.event.event.*;

/**
 * CallbackManager 接口，用于处理 AGUI 协议中的所有事件类型
 * 
 * 按照 AGUI 标准，支持以下事件类型：
 * - 生命周期事件：RUN_STARTED, RUN_FINISHED, RUN_ERROR, STEP_STARTED, STEP_FINISHED
 * - 文本消息事件：TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT, TEXT_MESSAGE_END
 * - 工具调用事件：TOOL_CALL_START, TOOL_CALL_ARGS, TOOL_CALL_END, TOOL_CALL_RESULT
 * - 状态管理事件：STATE_SNAPSHOT, STATE_DELTA, MESSAGES_SNAPSHOT
 * - 特殊事件：RAW, CUSTOM
 */
public interface CallbackManager {
    
    // ==================== 生命周期事件 ====================
    
    /**
     * 处理运行开始事件
     */
    void onRunStartedEvent(RunStartedEvent event);
    
    /**
     * 处理运行完成事件
     */
    void onRunFinishedEvent(RunFinishedEvent event);
    
    /**
     * 处理运行错误事件
     */
    void onRunErrorEvent(RunErrorEvent event);
    
    /**
     * 处理步骤开始事件
     */
    void onStepStartedEvent(StepStartedEvent event);
    
    /**
     * 处理步骤完成事件
     */
    void onStepFinishedEvent(StepFinishedEvent event);
    
    // ==================== 文本消息事件 ====================
    
    /**
     * 处理文本消息开始事件
     */
    void onTextMessageStartEvent(TextMessageStartEvent event);
    
    /**
     * 处理文本消息内容事件
     */
    void onTextMessageContentEvent(TextMessageContentEvent event);
    
    /**
     * 处理文本消息结束事件
     */
    void onTextMessageEndEvent(TextMessageEndEvent event);
    
    /**
     * 处理文本消息块事件
     */
    void onTextMessageChunkEvent(TextMessageChunkEvent event);
    
    // ==================== 思考过程事件 ====================
    
    /**
     * 处理思考开始事件
     */
    void onThinkingStartEvent(ThinkingStartEvent event);
    
    /**
     * 处理思考结束事件
     */
    void onThinkingEndEvent(ThinkingEndEvent event);
    
    /**
     * 处理思考文本消息开始事件
     */
    void onThinkingTextMessageStartEvent(ThinkingTextMessageStartEvent event);
    
    /**
     * 处理思考文本消息内容事件
     */
    void onThinkingTextMessageContentEvent(ThinkingTextMessageContentEvent event);
    
    /**
     * 处理思考文本消息结束事件
     */
    void onThinkingTextMessageEndEvent(ThinkingTextMessageEndEvent event);
    
    // ==================== 工具调用事件 ====================
    
    /**
     * 处理工具调用开始事件
     */
    void onToolCallStartEvent(ToolCallStartEvent event);
    
    /**
     * 处理工具调用参数事件
     */
    void onToolCallArgsEvent(ToolCallArgsEvent event);
    
    /**
     * 处理工具调用结束事件
     */
    void onToolCallEndEvent(ToolCallEndEvent event);
    
    /**
     * 处理工具调用结果事件
     */
    void onToolCallResultEvent(ToolCallResultEvent event);
    
    /**
     * 处理工具调用块事件
     */
    void onToolCallChunkEvent(ToolCallChunkEvent event);
    
    // ==================== 状态管理事件 ====================
    
    /**
     * 处理状态快照事件
     */
    void onStateSnapshotEvent(StateSnapshotEvent event);
    
    /**
     * 处理状态增量事件
     */
    void onStateDeltaEvent(StateDeltaEvent event);
    
    /**
     * 处理消息快照事件
     */
    void onMessagesSnapshotEvent(MessagesSnapshotEvent event);
    
    // ==================== 特殊事件 ====================
    
    /**
     * 处理原始事件
     */
    void onRawEvent(RawEvent event);
    
    /**
     * 处理自定义事件
     */
    void onCustomEvent(CustomEvent event);
    
    // ==================== 通用事件处理 ====================
    
    /**
     * 处理所有事件的通用方法
     */
    void onEvent(BaseEvent event);
}
