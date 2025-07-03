package com.alibaba.cloud.ai.example.manus2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.example.manus2.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.example.manus2.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agents/")
public class ChatController {
    
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    
    @Value("${api.key:}")
    private String apiKey;
    
    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Hello, World!");
    }
    
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestHeader("Authorization") String authHeader, 
                                 @RequestBody ChatRequest request) {
        
        // Validate API key
        if (!validateApiKey(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid authorization header"));
        }
        
        log.info("Received chat request: {}", request);
        
        try {
            ChatResponse response = chatService.processChatRequest(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping(value = "/chat_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        
        log.info("=== JAVA CHAT_STREAM START ===");
        log.info("Received streaming chat request: {}", request);
        
        return chatService.processChatRequestStreamed(request)
            .map(event -> {
                try {
                    String eventType = event.getType();
                    Object eventData = event.getData();
                    
                    // 详细日志记录
                    log.info("=== JAVA EVENT PROCESSING ===");
                    log.info("Event Type: {}", eventType);
                    log.info("Event Data Class: {}", eventData != null ? eventData.getClass().getSimpleName() : "null");
                    log.info("Event Data: {}", eventData);
                    
                    String data;
                    if ("done".equals(eventType)) {
                        // 对于done事件，需要包装成与Python版本一致的格式
                        Map<String, Object> stateWrapper = new HashMap<>();
                        stateWrapper.put("state", eventData);
                        data = objectMapper.writeValueAsString(stateWrapper);
                    } else {
                        // 对于其他事件，直接序列化
                        data = objectMapper.writeValueAsString(eventData);
                    }
                    
                    log.info("Serialized Data: {}", data);
                    
                    ServerSentEvent<String> sseEvent;
                    if ("error".equals(eventType)) {
                        sseEvent = ServerSentEvent.<String>builder()
                            .event("error")
                            .data(data)
                            .build();
                    } else {
                        sseEvent = ServerSentEvent.<String>builder()
                            .event(eventType)
                            .data(data)
                            .build();
                    }
                    
                    log.info("Generated SSE Event: {}", sseEvent);
                    log.info("=== JAVA EVENT PROCESSING END ===");
                    
                    return sseEvent;
                    
                } catch (Exception e) {
                    log.error("=== JAVA SERIALIZATION ERROR ===");
                    log.error("Error serializing stream event: {}", e.getMessage(), e);
                    return ServerSentEvent.<String>builder()
                        .event("error")
                        .data("{\"error\":\"Serialization error: " + e.getMessage() + "\"}")
                        .build();
                }
            })
            .onErrorResume(e -> {
                log.error("=== JAVA STREAM ERROR ===");
                log.error("Error in streaming: {}", e.getMessage(), e);
                return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\":\"" + e.getMessage() + "\"}")
                    .build());
            })
            .doOnComplete(() -> {
                log.info("=== JAVA CHAT_STREAM COMPLETE ===");
            });
    }
    
    private boolean validateApiKey(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        String token = authHeader.substring(7);
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.equals(token);
    }
} 