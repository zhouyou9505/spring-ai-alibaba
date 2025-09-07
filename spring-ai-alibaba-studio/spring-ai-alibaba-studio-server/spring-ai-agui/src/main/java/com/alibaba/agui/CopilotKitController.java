package com.alibaba.agui;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.alibaba.agui.sdk.ActionExecutionException;
import com.alibaba.agui.sdk.ActionNotFoundException;
import com.alibaba.agui.sdk.AgentGatewayService;
import com.alibaba.agui.sdk.AgentNotFoundException;
import com.alibaba.agui.sdk.CopilotKitContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/copilotkit")
public class CopilotKitController {

  private final AgentGatewayService gateway;

  public CopilotKitController(AgentGatewayService gateway) {
    this.gateway = gateway;
  }

  /* --------------------- DTOs --------------------- */
  @Data public static class V2AgentExecuteReq {
    Map<String,Object> state;
    List<Map<String,Object>> messages;
    List<Map<String,Object>> actions;
    String threadId;
    String nodeName;
    Map<String,Object> properties;
    String frontendUrl;
    // 注意：v2 没有 config（与 fastapi 对齐）
  }
  @Data public static class V2AgentStateReq {
    String threadId;
    Map<String,Object> properties;
    String frontendUrl;
  }
  @Data public static class V1AgentsExecuteReq {
    String name;
    String threadId;
    String nodeName;
    Map<String,Object> config;            // v1 才有 config 
    Map<String,Object> state;
    List<Map<String,Object>> messages;
    List<Map<String,Object>> actions;
    List<Map<String,Object>> metaEvents;
    Map<String,Object> properties;
    String frontendUrl;
  }
  @Data public static class V1AgentsStateReq {
    String name;
    String threadId;
    Map<String,Object> properties;
    String frontendUrl;
  }
  @Data public static class V1ActionExecuteReq {
    String name;
    Map<String,Object> arguments;
    Map<String,Object> properties;
    String frontendUrl;
  }

  /* --------------------- / (info) --------------------- */
  @RequestMapping(path = "", method = {RequestMethod.GET, RequestMethod.POST})
  public Mono<ResponseEntity<?>> rootInfo(@RequestBody(required=false) Map<String,Object> body,
                                          @RequestHeader HttpHeaders headers) {
    CopilotKitContext ctx = contextOf(body, headers);
    boolean wantsHtml = headers.getAccept().stream().anyMatch(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML));
    return gateway.info(ctx).map(info -> {
      if (wantsHtml) {
        String html = generateInfoHtml(info); // 极简 HTML（下方给出）
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
      }
      return ResponseEntity.ok(info);
    });
  }

  @RequestMapping(path = "/info",  method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Map<String,Object>> info(@RequestHeader HttpHeaders headers) {
    return gateway.info(contextOf(Map.of(), headers));
  }

  /* --------------------- v2: /agent/{name} 执行 (NDJSON) --------------------- */
  @RequestMapping(path="/agent/{name}", produces="application/x-ndjson")
  public Flux<String> executeV2(@PathVariable("name") String agentName,
                                @RequestBody V2AgentExecuteReq req,
                                @RequestHeader HttpHeaders headers) {
    String threadId = StringUtils.hasText(req.threadId)? req.threadId : UUID.randomUUID().toString();
    CopilotKitContext ctx = contextOf(Map.of(
        "properties", nz(req.properties),
        "frontendUrl", req.frontendUrl
    ), headers);
    return gateway.executeAgentStream(
        agentName, threadId, nz(req.state), null, // v2 没 config
        nzList(req.messages), nzList(req.actions), req.nodeName, null, ctx
    ).map(CopilotKitNdjson::encode)
     .onErrorResume(e -> Flux.just(CopilotKitNdjson.encode(errorEvt(e))));
  }

  /* --------------------- v2: /agent/{name}/state --------------------- */
  @RequestMapping(path="/agent/{name}/state", produces=MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, Object>>> getStateV2(@PathVariable("name") String agentName,
                                                              @RequestBody V2AgentStateReq req,
                                                              @RequestHeader HttpHeaders headers) {
    if (!StringUtils.hasText(req.threadId)) {
      return Mono.just(ResponseEntity.badRequest().body(Map.of("error","threadId is required")));
    }
    CopilotKitContext ctx = contextOf(makeBodyMap(req.properties, req.frontendUrl), headers);
    return gateway.getAgentState(agentName, req.threadId, ctx)
        .map(ResponseEntity::ok)
        .onErrorResume(AgentNotFoundException.class, e -> Mono.just(ResponseEntity.status(404).body(Map.of("error", e.getMessage()))))
        .onErrorResume(Exception.class, e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
  }

  /* --------------------- v1: /actions/execute --------------------- */
  @RequestMapping(path="/actions/execute", produces=MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, Object>>> executeAction(@RequestBody V1ActionExecuteReq req,
                                                                 @RequestHeader HttpHeaders headers) {
    if (!StringUtils.hasText(req.name)) {
      return Mono.just(ResponseEntity.badRequest().body(Map.of("error","name is required")));
    }
    CopilotKitContext ctx = contextOf(makeBodyMap(req.properties, req.frontendUrl), headers);
      Mono<ResponseEntity<Map<String, Object>>> responseEntityMono = gateway.executeAction(req.name, nz(req.arguments), ctx)
              .map(ResponseEntity::ok)
              .onErrorResume(ActionNotFoundException.class, e -> Mono.just(ResponseEntity.status(404).body(Map.of("error", e.getMessage()))))
              .onErrorResume(ActionExecutionException.class, e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))))
              .onErrorResume(Exception.class, e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
      return responseEntityMono;
  }

  /* --------------------- v1: /agents/execute (NDJSON) --------------------- */
  @RequestMapping(path="/agents/execute", produces="application/x-ndjson")
  public Flux<String> executeV1(@RequestBody V1AgentsExecuteReq req,
                                @RequestHeader HttpHeaders headers) {
    if (!StringUtils.hasText(req.name))     return Flux.just(ndErr("name is required",400));
    if (req.state==null)                    return Flux.just(ndErr("state is required",400));
    if (req.messages==null)                 return Flux.just(ndErr("messages is required",400));
    String threadId = StringUtils.hasText(req.threadId) ? req.threadId : UUID.randomUUID().toString();
    CopilotKitContext ctx = contextOf(makeBodyMap(req.properties, req.frontendUrl), headers);
    return gateway.executeAgentStream(
        req.name, threadId, nz(req.state), nz(req.config), nzList(req.messages),
        nzList(req.actions), req.nodeName, nzList(req.metaEvents), ctx
    ).map(CopilotKitNdjson::encode)
     .onErrorResume(e -> Flux.just(CopilotKitNdjson.encode(errorEvt(e))));
  }

  /* --------------------- v1: /agents/state --------------------- */
  @RequestMapping(path="/agents/state", produces=MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, Object>>> getStateV1(@RequestBody V1AgentsStateReq req,
                                                              @RequestHeader HttpHeaders headers) {
    if (!StringUtils.hasText(req.threadId)) return Mono.just(ResponseEntity.badRequest().body(Map.of("error","threadId is required")));
    if (!StringUtils.hasText(req.name))     return Mono.just(ResponseEntity.badRequest().body(Map.of("error","name is required")));
    CopilotKitContext ctx = contextOf(makeBodyMap(req.properties, req.frontendUrl), headers);
    return gateway.getAgentState(req.name, req.threadId, ctx)
        .map(ResponseEntity::ok)
        .onErrorResume(AgentNotFoundException.class, e -> Mono.just(ResponseEntity.status(404).body(Map.of("error", e.getMessage()))))
        .onErrorResume(Exception.class, e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
  }

  /* --------------------- helpers --------------------- */
  private static Map<String,Object> nz(Map<String,Object> m){ return m==null? Map.of():m; }
  private static List<Map<String,Object>> nzList(List<Map<String,Object>> l){ return l==null? List.of():l; }
  private static String ndErr(String msg,int code){ return CopilotKitNdjson.encode(Map.of("error", msg, "status", code)); }
  private static CopilotKitContext contextOf(Map<String,Object> body, HttpHeaders headers){
    Map<String,Object> b = (body==null? Map.of():body);
    Map<String, String> h = headers.toSingleValueMap();
    return new CopilotKitContext(
      (Map<String,Object>) b.getOrDefault("properties", Map.of()),
      (String) b.getOrDefault("frontendUrl", null),
      h
    );
  }
  private static Map<String,Object> errorEvt(Throwable e){
    return Map.of("event","on_copilotkit_error","data",Map.of("message", e.getMessage(), "type", e.getClass().getSimpleName()));
  }
  private static String generateInfoHtml(Map<String,Object> info){
    return "<!doctype html><html><head><meta charset='utf-8'><title>CopilotKit Info</title></head>"
         + "<body><h1>CopilotKit Java Endpoint</h1><pre>"+info+"</pre></body></html>";
  }
    // 放到 CopilotKitController 里（或一个 Utils）
    private static Map<String, Object> makeBodyMap(Map<String, Object> properties, String frontendUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("properties", properties != null ? properties : Map.of()); // 永不为 null
        if (frontendUrl != null) body.put("frontendUrl", frontendUrl);      // 仅在非 null 时放入
        return body;
    }

}
