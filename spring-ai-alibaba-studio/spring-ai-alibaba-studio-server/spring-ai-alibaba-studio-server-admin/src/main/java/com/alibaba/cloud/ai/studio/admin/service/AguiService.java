package com.alibaba.cloud.ai.studio.admin.service;

import com.agui.types.RunAgentInput;
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
     * Validate AG-UI request parameters
     * 
     * @param request The request to validate
     * @return true if valid, false otherwise
     */
}
