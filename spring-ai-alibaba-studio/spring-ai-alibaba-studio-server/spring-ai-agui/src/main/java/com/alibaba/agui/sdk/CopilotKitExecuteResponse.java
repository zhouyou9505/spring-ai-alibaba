package com.alibaba.agui.sdk;

import java.util.List;
import java.util.Map;

// CopilotKitExecuteResponse.java
public record CopilotKitExecuteResponse(
        String threadId,
        String name,
        List<CKMessage> messages,
        List<Object> actions,      // 简化：如果有 Tool 调用，你也可以把调用记录塞回去
        List<Object> metaEvents,   // 可为空列表
        Map<String, Object> state  // 回传或更新后的 state
) {
  public record CKMessage(
      String id,
      String createdAt,
      String type,     // "TextMessage"
      String role,     // "assistant"
      String content
  ) {}
}
