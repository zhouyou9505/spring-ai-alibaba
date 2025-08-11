package com.alibaba.cloud.ai.studio.admin.service;

import com.agui.types.RunAgentInput;
import com.alibaba.cloud.ai.studio.admin.controller.AguiController.AguiChatRequest;
import com.alibaba.cloud.ai.studio.admin.controller.AguiController.AguiDemoRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AG-UI Protocol Service Interface
 * 
 * Defines the service methods for handling AG-UI protocol requests.
 * This service handles the business logic for AI agent communication.
 */
public interface AguiService {

    /**
     * Process AG-UI agent request and stream responses
     * 
     * @param request The agent request
     * @param emitter The SSE emitter for streaming responses
     */
    void processAgentRequest(RunAgentInput request, SseEmitter emitter);

    /**
     * Process AG-UI chat request and stream responses
     * 
     * @param request The chat request
     * @param emitter The SSE emitter for streaming responses
     */
    void processChatRequest(AguiChatRequest request, SseEmitter emitter);

    /**
     * Process AG-UI demo request to demonstrate all event types
     * 
     * @param request The demo request
     * @param emitter The SSE emitter for streaming responses
     */
    void processDemoRequest(AguiDemoRequest request, SseEmitter emitter);

    /**
     * Generate a unique run ID for AG-UI requests
     * 
     * @return A unique run identifier
     */
    String generateRunId();

    /**
     * Validate AG-UI request parameters
     * 
     * @param request The request to validate
     * @return true if valid, false otherwise
     */
    boolean validateRequest(AguiChatRequest request);
}
