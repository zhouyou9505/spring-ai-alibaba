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
//@Document(collection = "snippets")
//public class Snippet {
//    @Id
//    private String id;
//
//    @Indexed
//    private String userId;
//
//    private String title;
//    private String description;
//    private String language;
//    private String code;
//    private List<String> tags;
//    private List<String> dependencies;
//    private Map<String, Object> metadata;
//    private boolean isPublic;
//    private int viewCount;
//    private int likeCount;
//    private int forkCount;
//    private String parentId; // for forked snippets
//    private List<String> relatedSnippets;
//    private Map<String, Object> testCases;
//    private String status; // draft, published, archived
//    private String errorMessage;
//    private String errorCode;
//
//    @CreatedDate
//    private Instant createdAt;
//
//    @LastModifiedDate
//    private Instant updatedAt;
//
//    private Instant lastViewedAt;
//    private Instant lastModifiedAt;
//    private Map<String, Integer> usageStats;
//    private List<String> recentViewers;
//    private Map<String, Object> versionHistory;
//}