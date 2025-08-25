package com.alibaba.cloud.ai.studio.admin.manager;

import com.alibaba.cloud.ai.dashscope.event.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AGUI 事件管理器，负责将事件通过 SseEmitter 推送到前端
 * 
 * 按照 AGUI 标准，支持所有事件类型的实时推送：
 * - 生命周期事件：RUN_STARTED, RUN_FINISHED, RUN_ERROR, STEP_STARTED, STEP_FINISHED
 * - 文本消息事件：TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT, TEXT_MESSAGE_END
 * - 工具调用事件：TOOL_CALL_START, TOOL_CALL_ARGS, TOOL_CALL_END, TOOL_CALL_RESULT
 * - 状态管理事件：STATE_SNAPSHOT, STATE_DELTA, MESSAGES_SNAPSHOT
 * - 特殊事件：RAW, CUSTOM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AguiEventManager {
    
    /**
     * 会话ID到SseEmitter的映射
     */
    private final Map<String, SseEmitter> sessionEmitters = new ConcurrentHashMap<>();
    
    /**
     * 注册会话的SseEmitter
     * 
     * @param sessionId 会话ID
     * @param emitter SseEmitter实例
     */
    public void registerSession(String sessionId, SseEmitter emitter) {
        sessionEmitters.put(sessionId, emitter);
        log.info("Registered SSE emitter for session: {}", sessionId);
        
        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            sessionEmitters.remove(sessionId);
            log.info("SSE emitter completed for session: {}", sessionId);
        });
        
        emitter.onTimeout(() -> {
            sessionEmitters.remove(sessionId);
            log.info("SSE emitter timeout for session: {}", sessionId);
        });
        
        emitter.onError((ex) -> {
            sessionEmitters.remove(sessionId);
            log.error("SSE emitter error for session: {}", sessionId, ex);
        });
    }
    
    /**
     * 注销会话
     * 
     * @param sessionId 会话ID
     */
    public void unregisterSession(String sessionId) {
        SseEmitter emitter = sessionEmitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("Unregistered SSE emitter for session: {}", sessionId);
            } catch (Exception e) {
                log.warn("Error completing emitter for session: {}", sessionId, e);
            }
        }
    }
    
    /**
     * 向所有会话发送事件
     * 
     * @param event 要发送的事件
     */
    public void emitEvent(BaseEvent event) {
        if (sessionEmitters.isEmpty()) {
            log.debug("No active sessions to emit event: {}", event.getType());
            return;
        }
        
        log.debug("Emitting event to {} sessions: type={}", sessionEmitters.size(), event.getType());
        
        // 创建事件数据
        String eventData = createEventData(event);
        
        // 向所有会话发送事件
        sessionEmitters.entrySet().removeIf(entry -> {
            String sessionId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            
            try {
                emitter.send(eventData);
                return false; // 不移除
            } catch (IOException e) {
                log.warn("Failed to send event to session: {}, removing emitter", sessionId, e);
                return true; // 移除失败的emitter
            } catch (Exception e) {
                log.error("Unexpected error sending event to session: {}", sessionId, e);
                return true; // 移除出错的emitter
            }
        });
    }
    
    /**
     * 向指定会话发送事件
     * 
     * @param sessionId 会话ID
     * @param event 要发送的事件
     */
    public void emitEventToSession(String sessionId, BaseEvent event) {
        SseEmitter emitter = sessionEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("No emitter found for session: {}", sessionId);
            return;
        }
        
        try {
            String eventData = createEventData(event);
            emitter.send(eventData);
            log.debug("Sent event to session {}: type={}", sessionId, event.getType());
        } catch (IOException e) {
            log.warn("Failed to send event to session: {}", sessionId, e);
            // 移除失败的emitter
            sessionEmitters.remove(sessionId);
        } catch (Exception e) {
            log.error("Unexpected error sending event to session: {}", sessionId, e);
            // 移除出错的emitter
            sessionEmitters.remove(sessionId);
        }
    }
    
    /**
     * 创建事件数据字符串
     * 
     * @param event 事件对象
     * @return 格式化的事件数据字符串
     */
    private String createEventData(BaseEvent event) {
        // 按照 Server-Sent Events 格式创建数据
        StringBuilder sb = new StringBuilder();
        sb.append("data: ");
        
        // 添加事件类型
        sb.append("{\"type\":\"").append(event.getType().getName()).append("\"");
        
        // 添加时间戳
        sb.append(",\"timestamp\":").append(event.getTimestamp());
        
        // 根据事件类型添加特定属性
        addEventSpecificData(sb, event);
        
        sb.append("}\n\n");
        return sb.toString();
    }
    
    /**
     * 根据事件类型添加特定数据
     * 
     * @param sb StringBuilder
     * @param event 事件对象
     */
    private void addEventSpecificData(StringBuilder sb, BaseEvent event) {
        // 这里可以根据具体的事件类型添加相应的属性
        // 为了简化，我们只添加基本属性，具体实现可以根据需要扩展
        
        if (event.getRawEvent() != null) {
            sb.append(",\"rawEvent\":").append(event.getRawEvent().toString());
        }
    }
    
    /**
     * 获取当前活跃会话数量
     * 
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessionEmitters.size();
    }
    
    /**
     * 检查会话是否存在
     * 
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessionEmitters.containsKey(sessionId);
    }
}
