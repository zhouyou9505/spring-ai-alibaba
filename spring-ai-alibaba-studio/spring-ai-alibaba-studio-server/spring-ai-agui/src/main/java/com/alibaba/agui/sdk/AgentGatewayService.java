//package com.alibaba.agui.sdk;// package com.example.copilotkit.service;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//public class AgentGatewayService {
//
//  private final CopilotKitRemoteEndpoint sdk;
//
//  public AgentGatewayService(CopilotKitRemoteEndpoint sdk) {
//    this.sdk = sdk; // 通过 @Configuration 注入，或用 @Bean 构造
//  }
//
//  public Mono<Map<String,Object>> info(CopilotKitContext ctx){
//    return Mono.fromCallable(() -> sdk.info(ctx));
//  }
//
//  public Mono<Map<String,Object>> executeAction(
//      String actionName, Map<String,Object> arguments, CopilotKitContext ctx){
//    return Mono.fromFuture(sdk.executeAction(ctx, actionName, arguments));
//  }
//
//  public Flux<Map<String,Object>> executeAgentStream(
//      String agentName, String threadId, Map<String,Object> state, Map<String,Object> config,
//      List<Map<String,Object>> messages, List<Map<String,Object>> actions,
//      String nodeName, List<Map<String,Object>> metaEvents,
//      CopilotKitContext ctx
//  ){
//    return Flux.fromIterable(sdk.executeAgent(ctx, agentName, threadId, state, config, messages, actions, nodeName, metaEvents))
//               .map(AgentGatewayService::parseJsonLineSafe); // 统一内部事件：每行 JSON→Map
//  }
//
//  public Mono<Map<String,Object>> getAgentState(String agentName, String threadId, CopilotKitContext ctx){
//    CompletableFuture<Map<String, Object>> f = sdk.getAgentState(ctx, threadId, agentName);
//    return Mono.fromFuture(f);
//  }
//
//  // —— 小工具：把 agent 输出的 JSON 行 decode 成 Map（便于下游适配成 NDJSON / SSE）——
//  private static final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
//  private static Map<String,Object> parseJsonLineSafe(String line){
//    try { return om.readValue(line, Map.class); }
//    catch(Exception e){ return Map.of("event","on_copilotkit_error","data", Map.of("message", e.getMessage())); }
//  }
//}
