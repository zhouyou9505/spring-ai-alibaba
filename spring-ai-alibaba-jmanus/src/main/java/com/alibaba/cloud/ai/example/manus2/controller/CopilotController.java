package com.alibaba.cloud.ai.example.manus2.controller;

import com.alibaba.cloud.ai.example.manus2.model.ApiRequest;
import com.alibaba.cloud.ai.example.manus2.model.ApiResponse;
import com.alibaba.cloud.ai.example.manus2.service.CopilotService;
import com.alibaba.cloud.ai.example.manus2.service.StreamingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequestMapping("/")
@RestController
@RequiredArgsConstructor
public class CopilotController {
    private final CopilotService copilotService;
    private final StreamingService streamingService;
    private final ObjectMapper objectMapper;

    @Value("${copilot.api.key:}")
    private String apiKey;

//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> health() {
//        return ResponseEntity.ok(Map.of("status", "ok"));
//    }

    @SneakyThrows
    @PostMapping(value = "/chat_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestBody ApiRequest request,
            HttpServletRequest httpRequest
    ) {
//        validateApiKey(httpRequest);
        validateRequest(request);

        return streamingService.getStreamingResponse(
                        request.getMessages(),
                        request.getWorkflowSchema(),
                        request.getCurrentWorkflowConfig(),
                        request.getContext(),
                        request.getDataSources()
                ).filter(content -> content != null && !content.trim().isEmpty())
                .map(content -> {
                    try {
                        String json = objectMapper.writeValueAsString(Map.of("content", content));
                        return ServerSentEvent.<String>builder()
                                .data(json)
                                .build();
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing stream content", e);
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\":\"Serialization error\"}")
                                .build();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error during streaming", e);
                    String errorJson;
                    try {
                        errorJson = objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
                    } catch (JsonProcessingException ex) {
                        errorJson = "{\"error\":\"Unknown error\"}";
                    }
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build());
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()));
    }

    @PostMapping("/edit_agent_instructions")
    public ResponseEntity<ApiResponse> editAgentInstructions(
            @RequestBody ApiRequest request,
            HttpServletRequest httpRequest
    ) throws IOException {
        validateApiKey(httpRequest);
        validateRequest(request);

        String response = copilotService.getResponse(
            request.getMessages(),
            request.getWorkflowSchema(),
            request.getCurrentWorkflowConfig(),
            request.getContext(),
            request.getDataSources(),
            copilotService.getEditAgentInstructions()
        );

        return ResponseEntity.ok(new ApiResponse(response));
    }

    private void validateApiKey(HttpServletRequest request) {
        if (apiKey != null && !apiKey.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ") || 
                !authHeader.substring(7).equals(apiKey)) {
                throw new RuntimeException("Invalid API key");
            }
        }
    }

    private void validateRequest(ApiRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new RuntimeException("Messages cannot be empty");
        }
        if (request.getWorkflowSchema() == null || request.getWorkflowSchema().isEmpty()) {
            throw new RuntimeException("Workflow schema cannot be empty");
        }
    }
} 