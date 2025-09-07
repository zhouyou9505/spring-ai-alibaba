package com.alibaba.cloud.ai.graph.event.manager;


import com.alibaba.cloud.ai.graph.event.event.AgentStartEvent;
import com.alibaba.cloud.ai.graph.event.event.AgentFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.RetrieverStartEvent;
import com.alibaba.cloud.ai.graph.event.event.RetrieverFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.RetrieverErrorEvent;
import com.alibaba.cloud.ai.graph.event.event.CustomEvent;
import com.alibaba.cloud.ai.graph.event.event.MessagesSnapshotEvent;
import com.alibaba.cloud.ai.graph.event.event.RawEvent;
import com.alibaba.cloud.ai.graph.event.event.RunErrorEvent;
import com.alibaba.cloud.ai.graph.event.event.RunFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.RunStartedEvent;
import com.alibaba.cloud.ai.graph.event.event.StateDeltaEvent;
import com.alibaba.cloud.ai.graph.event.event.StateSnapshotEvent;
import com.alibaba.cloud.ai.graph.event.event.StepFinishedEvent;
import com.alibaba.cloud.ai.graph.event.event.StepStartedEvent;
import com.alibaba.cloud.ai.graph.event.event.TextMessageContentEvent;
import com.alibaba.cloud.ai.graph.event.event.TextMessageEndEvent;
import com.alibaba.cloud.ai.graph.event.event.TextMessageStartEvent;
import com.alibaba.cloud.ai.graph.event.event.ToolCallArgsEvent;
import com.alibaba.cloud.ai.graph.event.event.ToolCallEndEvent;
import com.alibaba.cloud.ai.graph.event.event.ToolCallResultEvent;
import com.alibaba.cloud.ai.graph.event.event.ToolCallStartEvent;

/**
 * Callback manager interface for handling all event types.
 * <p>
 * This interface defines methods for handling all 25 event types supported by the AGUI system.
 * Each method corresponds to a specific event type and is called when that event occurs.
 * </p>
 */
public interface CallbackManager {
    
    // 代理生命周期事件
    void onAgentStartEvent(AgentStartEvent event);
    void onAgentFinishEvent(AgentFinishedEvent event);
    
    // 检索器生命周期事件
    void onRetrieverStartEvent(RetrieverStartEvent event);
    void onRetrieverEndEvent(RetrieverFinishedEvent event);
    void onRetrieverErrorEvent(RetrieverErrorEvent event);
    
    // 执行生命周期事件
    void onRunStartedEvent(RunStartedEvent event);
    void onRunFinishedEvent(RunFinishedEvent event);
    void onRunErrorEvent(RunErrorEvent event);
    
    // 步骤执行事件
    void onStepStartedEvent(StepStartedEvent event);
    void onStepFinishedEvent(StepFinishedEvent event);
    
    // 文本消息事件
    void onTextMessageStartEvent(TextMessageStartEvent event);
    void onTextMessageContentEvent(TextMessageContentEvent event);
    void onTextMessageEndEvent(TextMessageEndEvent event);

    // 工具调用事件
    void onToolCallStartEvent(ToolCallStartEvent event);
    void onToolCallArgsEvent(ToolCallArgsEvent event);
    void onToolCallEndEvent(ToolCallEndEvent event);
    void onToolCallResultEvent(ToolCallResultEvent event);
    
    // 状态管理事件
    void onStateSnapshotEvent(StateSnapshotEvent event);
    void onStateDeltaEvent(StateDeltaEvent event);
    void onMessagesSnapshotEvent(MessagesSnapshotEvent event);
    
    // 通用事件
    void onRawEvent(RawEvent event);
    void onCustomEvent(CustomEvent event);
}
