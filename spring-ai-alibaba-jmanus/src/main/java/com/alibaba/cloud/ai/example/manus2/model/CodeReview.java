//package com.rowboat.copilot.model;
//
//import lombok.Data;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Map;
//
//@Data
//@Document(collection = "code_reviews")
//public class CodeReview {
//    @Id
//    private String id;
//
//    @Indexed
//    private String userId;
//
//    @Indexed
//    private String snippetId;
//
//    private String title;
//    private String description;
//    private String language;
//    private String code;
//    private List<ReviewComment> comments;
//    private Map<String, Object> analysis;
//    private Map<String, Object> suggestions;
//    private Map<String, Object> metrics;
//    private String status; // pending, in_progress, completed
//    private String errorMessage;
//    private String errorCode;
//    private Map<String, Object> metadata;
//
//    @CreatedDate
//    private Instant createdAt;
//
//    @LastModifiedDate
//    private Instant updatedAt;
//
//    private Instant completedAt;
//    private long processingTimeMs;
//    private Map<String, Integer> modelUsage;
//    private List<String> reviewedFiles;
//    private Map<String, Object> reviewHistory;
//
//    @Data
//    public static class ReviewComment {
//        private String id;
//        private String type; // suggestion, issue, question, praise
//        private String severity; // low, medium, high, critical
//        private String content;
//        private String filePath;
//        private int lineNumber;
//        private String codeContext;
//        private List<String> suggestedFixes;
//        private Map<String, Object> metadata;
//        private Instant createdAt;
//        private Instant updatedAt;
//        private String status; // open, resolved, dismissed
//        private String resolvedBy;
//        private Instant resolvedAt;
//    }
//}
//