//package com.rowboat.copilot.model;
//
//import lombok.Data;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Map;
//
//@Data
//@Document(collection = "generation_requests")
//public class GenerationRequest {
//    @Id
//    private String id;
//
//    @Indexed
//    private String userId;
//
//    private String prompt;
//    private String language;
//    private String framework;
//    private List<String> requirements;
//    private Map<String, Object> context;
//    private Map<String, Object> constraints;
//    private Map<String, Object> preferences;
//    private int maxTokens;
//    private double temperature;
//    private boolean includeTests;
//    private boolean includeDocs;
//    private String status; // pending, processing, completed, failed
//    private String result;
//    private String errorMessage;
//    private String errorCode;
//    private Map<String, Object> metadata;
//
//    @CreatedDate
//    private Instant createdAt;
//
//    private Instant startedAt;
//    private Instant completedAt;
//    private long processingTimeMs;
//    private int tokenCount;
//    private Map<String, Integer> modelUsage;
//    private List<String> generatedFiles;
//    private Map<String, Object> evaluationResults;
//}