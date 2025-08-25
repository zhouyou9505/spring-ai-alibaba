package com.alibaba.cloud.ai.studio.admin.manager;

import com.alibaba.cloud.ai.dashscope.event.event.*;
import com.alibaba.cloud.ai.graph.CallbackManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CallbackManager 的实现类，负责将 AGUI 事件发送到 AguiEventManager
 * 
 * 按照 AGUI 标准，处理所有事件类型：
 * - 生命周期事件：RUN_STARTED, RUN_FINISHED, RUN_ERROR, STEP_STARTED, STEP_FINISHED
 * - 文本消息事件：TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT, TEXT_MESSAGE_END
 * - 工具调用事件：TOOL_CALL_START, TOOL_CALL_ARGS, TOOL_CALL_END, TOOL_CALL_RESULT
 * - 状态管理事件：STATE_SNAPSHOT, STATE_DELTA, MESSAGES_SNAPSHOT
 * - 特殊事件：RAW, CUSTOM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackManagerImpl implements CallbackManager {
    
    private final AguiEventManager eventManager;
    
    // ==================== 生命周期事件 ====================
    
    @Override
    public void onRunStartedEvent(RunStartedEvent event) {
        try {
            log.debug("Sending RUN_STARTED event: runId={}, threadId={}", event.getRunId(), event.getThreadId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending RUN_STARTED event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onRunFinishedEvent(RunFinishedEvent event) {
        try {
            log.debug("Sending RUN_FINISHED event: runId={}, threadId={}", event.getRunId(), event.getThreadId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending RUN_FINISHED event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onRunErrorEvent(RunErrorEvent event) {
        try {
            log.debug("Sending RUN_ERROR event: error={}", event.getError());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending RUN_ERROR event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onStepStartedEvent(StepStartedEvent event) {
        try {
            log.debug("Sending STEP_STARTED event: stepId={}, stepName={}", event.getStepId(), event.getStepName());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending STEP_STARTED event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onStepFinishedEvent(StepFinishedEvent event) {
        try {
            log.debug("Sending STEP_FINISHED event: stepId={}, stepName={}", event.getStepId(), event.getStepName());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending STEP_FINISHED event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 文本消息事件 ====================
    
    @Override
    public void onTextMessageStartEvent(TextMessageStartEvent event) {
        try {
            log.debug("Sending TEXT_MESSAGE_START event: messageId={}, role={}", event.getMessageId(), event.getRole());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TEXT_MESSAGE_START event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onTextMessageContentEvent(TextMessageContentEvent event) {
        try {
            log.debug("Sending TEXT_MESSAGE_CONTENT event: messageId={}, delta={}", event.getMessageId(), event.getDelta());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TEXT_MESSAGE_CONTENT event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onTextMessageEndEvent(TextMessageEndEvent event) {
        try {
            log.debug("Sending TEXT_MESSAGE_END event: messageId={}", event.getMessageId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TEXT_MESSAGE_END event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onTextMessageChunkEvent(TextMessageChunkEvent event) {
        try {
            log.debug("Sending TEXT_MESSAGE_CHUNK event: messageId={}, chunk={}", event.getMessageId(), event.getChunk());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TEXT_MESSAGE_CHUNK event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 思考过程事件 ====================
    
    @Override
    public void onThinkingStartEvent(ThinkingStartEvent event) {
        try {
            log.debug("Sending THINKING_START event");
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending THINKING_START event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onThinkingEndEvent(ThinkingEndEvent event) {
        try {
            log.debug("Sending THINKING_END event");
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending THINKING_END event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onThinkingTextMessageStartEvent(ThinkingTextMessageStartEvent event) {
        try {
            log.debug("Sending THINKING_TEXT_MESSAGE_START event: messageId={}, role={}", event.getMessageId(), event.getRole());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending THINKING_TEXT_MESSAGE_START event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onThinkingTextMessageContentEvent(ThinkingTextMessageContentEvent event) {
        try {
            log.debug("Sending THINKING_TEXT_MESSAGE_CONTENT event: messageId={}, delta={}", event.getMessageId(), event.getDelta());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending THINKING_TEXT_MESSAGE_CONTENT event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onThinkingTextMessageEndEvent(ThinkingTextMessageEndEvent event) {
        try {
            log.debug("Sending THINKING_TEXT_MESSAGE_END event: messageId={}", event.getMessageId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending THINKING_TEXT_MESSAGE_END event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 工具调用事件 ====================
    
    @Override
    public void onToolCallStartEvent(ToolCallStartEvent event) {
        try {
            log.debug("Sending TOOL_CALL_START event: toolCallId={}, toolCallName={}", event.getToolCallId(), event.getToolCallName());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TOOL_CALL_START event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onToolCallArgsEvent(ToolCallArgsEvent event) {
        try {
            log.debug("Sending TOOL_CALL_ARGS event: toolCallId={}, delta={}", event.getToolCallId(), event.getDelta());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TOOL_CALL_ARGS event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onToolCallEndEvent(ToolCallEndEvent event) {
        try {
            log.debug("Sending TOOL_CALL_END event: toolCallId={}", event.getToolCallId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TOOL_CALL_END event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onToolCallResultEvent(ToolCallResultEvent event) {
        try {
            log.debug("Sending TOOL_CALL_RESULT event: messageId={}, toolCallId={}", event.getMessageId(), event.getToolCallId());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TOOL_CALL_RESULT event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onToolCallChunkEvent(ToolCallChunkEvent event) {
        try {
            log.debug("Sending TOOL_CALL_CHUNK event: toolCallId={}, chunk={}", event.getToolCallId(), event.getChunk());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending TOOL_CALL_CHUNK event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 状态管理事件 ====================
    
    @Override
    public void onStateSnapshotEvent(StateSnapshotEvent event) {
        try {
            log.debug("Sending STATE_SNAPSHOT event");
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending STATE_SNAPSHOT event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onStateDeltaEvent(StateDeltaEvent event) {
        try {
            log.debug("Sending STATE_DELTA event");
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending STATE_DELTA event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onMessagesSnapshotEvent(MessagesSnapshotEvent event) {
        try {
            log.debug("Sending MESSAGES_SNAPSHOT event: messageCount={}", event.getMessages().size());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending MESSAGES_SNAPSHOT event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 特殊事件 ====================
    
    @Override
    public void onRawEvent(RawEvent event) {
        try {
            log.debug("Sending RAW event: source={}", event.getSource());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending RAW event: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onCustomEvent(CustomEvent event) {
        try {
            log.debug("Sending CUSTOM event: name={}", event.getName());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending CUSTOM event: {}", e.getMessage(), e);
        }
    }
    
    // ==================== 通用事件处理 ====================
    
    @Override
    public void onEvent(BaseEvent event) {
        try {
            log.debug("Sending generic event: type={}", event.getType());
            eventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error sending generic event: {}", e.getMessage(), e);
        }
    }
}
