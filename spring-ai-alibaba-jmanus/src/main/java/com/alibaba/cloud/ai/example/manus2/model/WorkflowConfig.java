//package com.alibaba.cloud.ai.example.manus2.model;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//import java.util.List;
//import java.util.Map;
//
//@Data
//public class WorkflowConfig {
//    private String projectId;
//    private List<Agent> agents;
//    private List<Prompt> prompts;
//    private List<Tool> tools;
//    private String startAgent;
//    private String createdAt;
//    private String lastUpdatedAt;
//    private String name;
//    @JsonProperty("_id")
//    private String id;
//}
//
//@Data
//class Agent {
//    private String name;
//    private String type;
//    private String description;
//    private String instructions;
//    private String model;
//    private boolean toggleAble;
//    private String ragReturnType;
//    private int ragK;
//    private String controlType;
//    private String outputVisibility;
//    private String examples;
//    private Integer order;
//    private boolean disabled;
//    private boolean locked;
//    private Integer maxCallsPerParentAgent;
//}
//
//@Data
//class Tool {
//    private String name;
//    private String description;
//    private Map<String, Object> parameters;
//    @JsonProperty("isLibrary")
//    private boolean isLibrary;
//}
//
//@Data
//class Prompt {
//    private String name;
//    private String content;
//    private String description;
//    private Map<String, Object> metadata;
//}