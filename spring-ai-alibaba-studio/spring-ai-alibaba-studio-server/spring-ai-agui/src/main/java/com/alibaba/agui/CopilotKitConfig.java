//package com.alibaba.agui;// package com.example.copilotkit.config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import com.alibaba.agui.sdk.Action;
//import com.alibaba.agui.sdk.Agent;
//import com.alibaba.agui.sdk.CopilotKitRemoteEndpoint;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Configuration
//public class CopilotKitConfig {
//
//  @Bean
//  public CopilotKitRemoteEndpoint copilotSdk() {
//    // ——— 示例：一个假 Agent（请替换成你真实的） ———
//    Agent echoAgent = new Agent() {
//      public String name(){ return "ai_researcher"; }
//      public Map<String,Object> dictRepr(){ return Map.of("name","ai_researcher","description","ai_researcher agent","type","springai"); }
//      public Iterable<String> execute(String threadId, String nodeName, Map<String,Object> state, Map<String,Object> config,
//                                      List<Map<String,Object>> messages, List<Map<String,Object>> actions, List<Map<String,Object>> metaEvents) {
//        // 输出两行 NDJSON 示例：一个 state_sync 事件 + 一个自定义事件
//        String a = "{\"event\":\"on_copilotkit_state_sync\",\"thread_id\":\""+threadId+"\",\"agent_name\":\"echo\",\"node_name\":\"__start__\",\"state\":"+toJson(state)+",\"running\":true,\"active\":true}";
//        String b = "{\"event\":\"on_custom_event\",\"name\":\"echo\",\"data\":{\"messages\":"+toJson(messages)+"}}";
//        return List.of(a,b);
//      }
//      public CompletableFuture<Map<String,Object>> getState(String threadId){
//        return CompletableFuture.completedFuture(Map.of("threadId", threadId, "threadExists", true, "state", Map.of(), "messages", List.of()));
//      }
//      private String toJson(Object o){ try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);} catch(Exception e){ return "null"; } }
//    };
//
//    return new CopilotKitRemoteEndpoint(List.of(), List.of(echoAgent));
//  }
//}
