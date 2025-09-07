package com.alibaba.agui.sdk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Agent {
  String name();
  Map<String, Object> dictRepr();  // name/description/type...
  /** 返回“事件行”可迭代（供 NDJSON/SSE 输出），元素为一行 JSON 字符串（末尾不带 \n） */
  Iterable<String> execute(
      String threadId,
      String nodeName,
      Map<String, Object> state,
      Map<String, Object> config,
      List<Map<String, Object>> messages,
      List<Map<String, Object>> actions,
      List<Map<String, Object>> metaEvents
  ) throws Exception;
  CompletableFuture<Map<String, Object>> getState(String threadId) throws Exception;
}
