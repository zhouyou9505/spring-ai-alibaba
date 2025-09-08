package com.alibaba.agui.sdk;

import java.util.List;
import java.util.Map;

// CopilotKitExecuteRequest.java
public record CopilotKitExecuteRequest(
    String threadId,
    String name,
    Map<String, Object> state,
    List<CKMessage> messages,
    List<CKAction> actions,
    Map<String, Object> properties,
    Map<String, Object> config
) {
  public record CKMessage(
      String id,
      String createdAt,
      String type,      // "TextMessage"
      String role,      // "system" | "user" | "assistant" ...
      String content
  ) {}

  public record CKAction(
      String name,
      String description,
      Map<String, Object> parameters // JSON Schema
  ) {}
}
