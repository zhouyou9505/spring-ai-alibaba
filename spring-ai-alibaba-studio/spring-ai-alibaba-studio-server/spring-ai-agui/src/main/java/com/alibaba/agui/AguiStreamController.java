package com.alibaba.agui;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.event.agent.AgentOrchestrator;
import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
import com.alibaba.cloud.ai.graph.event.context.Context;
import com.alibaba.cloud.ai.graph.event.event.BaseEvent;
import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
import com.alibaba.cloud.ai.graph.event.message.MessageMapper;
import com.alibaba.cloud.ai.graph.event.state.State;
import com.alibaba.cloud.ai.graph.event.tool.Tool;
import com.alibaba.cloud.ai.graph.event.tool.Tool.ToolParameters;
import com.alibaba.cloud.ai.graph.event.tool.Tool.ToolProperty;
import com.alibaba.cloud.ai.graph.event.tool.ToolCall;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RestController
@RequestMapping("/copilotkit")
@Tag(name = "CopilotKit ServiceAdapter Controller", description = "CopilotKit ServiceAdapter streaming controller")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@Slf4j
public class AguiStreamController {

    @Resource
    private ChatModel chatModel;

    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AguiStreamController() {
        this.messageMapper = new MessageMapper();
    }


    /**
     * CopilotKit ServiceAdapter 主入口（SSE 流）
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @Operation(summary = "CopilotKit ServiceAdapter Endpoint (SSE)", description = "Main ServiceAdapter endpoint for CopilotKit integration, streaming SSE")
    public Flux<ServerSentEvent<BaseEvent>> copilotKitServiceAdapter(
            @RequestBody String requestBody,
            HttpServletResponse response
    ) {

        // SSE 必要响应头
        setCorsHeaders(response);
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no"); // Nginx 不要缓冲

        // 解析为 RunAgentInput（把 CopilotKit payload 映射到 AG-UI）
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(requestBody);
        RunAgentInput input = createAdaptedRunAgentInput(jsonObject);

        // 准备 Tools & Messages
        List<ToolCallback> toolCallbacks = Arrays.asList(ToolCallbacks.from(new Tools()));
        List<org.springframework.ai.chat.messages.AbstractMessage> springMessages =
                convertMessagesToSpringMessages(input.messages());

        AgentOrchestrator orchestrator = new AgentOrchestrator();

        // 主执行流
        Flux<ServerSentEvent<BaseEvent>> runFlux = orchestrator.run(callbackManager -> {
            try {
                ReactAgent agent = ReactAgent.builder()
                        .name("agui_stream_agent")
                        .model(chatModel)
                        .tools(toolCallbacks)
                        .callManager(callbackManager)
                        .build();

                Map<String, Object> graphInputs = new HashMap<>();
                graphInputs.put("messages", springMessages);
                graphInputs.put("threadId", input.threadId());
                graphInputs.put("runId", input.runId());
                graphInputs.put("tools", input.tools());
                graphInputs.put("context", input.context());

                // 真正运行
                agent.invoke(graphInputs);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, input);

        return runFlux;
    }

    /**
     * CopilotKit Actions Execute 端点
     */
    @PostMapping(
            path = "/actions/execute",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "CopilotKit Actions Execute Endpoint", description = "Execute CopilotKit actions")
    public ResponseEntity<Map<String, Object>> executeAction(@RequestBody Map<String, Object> requestBody) {
        try {
            String actionName = (String) requestBody.get("name");
            Map<String, Object> arguments = (Map<String, Object>) requestBody.get("arguments");
            

            // 简单的 action 执行逻辑
            Map<String, Object> result = new HashMap<>();
            result.put("action", actionName);
            result.put("arguments", arguments);
            result.put("result", "Action executed successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private RunAgentInput createAdaptedRunAgentInput(com.alibaba.fastjson.JSONObject json) {
        String threadId = json.getString("threadId");
        String runId = json.getString("runId");

        // messages
        List<BaseMessage> messages = convertJsonMessages(json.getJSONArray("messages"));

        State state = new State();

        // context（可把 properties/config 放进去）
        List<Context> context = new ArrayList<>();
        if (json.getJSONObject("properties") != null) {
            context.add(new Context("properties", json.getJSONObject("properties")));
        }
        if (json.getJSONObject("config") != null) {
            context.add(new Context("config", json.getJSONObject("config")));
        }

        Object forwardedProps = json.get("forwardedParameters");

        return new RunAgentInput(threadId, runId, state, messages, null, context, forwardedProps);
    }

    private List<BaseMessage> convertJsonMessages(com.alibaba.fastjson.JSONArray arr) {
        if (arr == null || arr.isEmpty()) return new ArrayList<>();
        List<BaseMessage> messages = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            com.alibaba.fastjson.JSONObject msgObj = arr.getJSONObject(i);
            BaseMessage m = createMessageByRole(msgObj);
            if (m != null) messages.add(m);
        }
        return messages;
    }

    private List<Tool> convertJsonActions(com.alibaba.fastjson.JSONArray actionsArray) {
        if (actionsArray == null || actionsArray.isEmpty()) return new ArrayList<>();
        List<Tool> tools = new ArrayList<>();

        for (int i = 0; i < actionsArray.size(); i++) {
            com.alibaba.fastjson.JSONObject a = actionsArray.getJSONObject(i);
            String name = a.getString("name");
            String description = a.getString("description");

            com.alibaba.fastjson.JSONObject params = a.getJSONObject("parameters"); // ← 关键：用 parameters
            ToolParameters toolParams = toToolParameters(params);

            tools.add(new Tool(name, description, toolParams));
        }
        return tools;
    }

    private ToolParameters toToolParameters(com.alibaba.fastjson.JSONObject schema) {
        if (schema == null) return new ToolParameters("object", new HashMap<>(), new ArrayList<>());

        String type = Optional.ofNullable(schema.getString("type")).orElse("object");

        Map<String, ToolProperty> properties = new HashMap<>();
        com.alibaba.fastjson.JSONObject props = schema.getJSONObject("properties");
        if (props != null) {
            for (String key : props.keySet()) {
                com.alibaba.fastjson.JSONObject prop = props.getJSONObject(key);
                String propType = prop.getString("type");
                String propDesc = Optional.ofNullable(prop.getString("description")).orElse("");
                properties.put(key, new ToolProperty(
                        Optional.ofNullable(propType).orElse("string"),
                        propDesc
                ));
            }
        }

        List<String> required = new ArrayList<>();
        com.alibaba.fastjson.JSONArray req = schema.getJSONArray("required");
        if (req != null) {
            for (int i = 0; i < req.size(); i++) required.add(req.getString(i));
        }

        return new ToolParameters(type, properties, required);
    }

    private List<org.springframework.ai.chat.messages.AbstractMessage> convertMessagesToSpringMessages(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();
        return messages.stream().map(messageMapper::toSpringMessage).collect(Collectors.toList());
    }

    private BaseMessage createMessageByRole(com.alibaba.fastjson.JSONObject msgObj) {
        BaseMessage message;
        String role = Optional.ofNullable(msgObj.getString("role")).orElse("user");
        String content = Optional.ofNullable(msgObj.getString("content")).orElse("");
        String id = Optional.ofNullable(msgObj.getString("id")).orElse(UUID.randomUUID().toString());

        switch (role.toLowerCase()) {
            case "assistant":
                List<ToolCall> toolCalls = new ArrayList<>();
                if (msgObj.containsKey("toolCalls") && msgObj.getJSONArray("toolCalls") != null) {
                    com.alibaba.fastjson.JSONArray toolCallsArray = msgObj.getJSONArray("toolCalls");
                    if (!toolCallsArray.isEmpty()) {
                        toolCalls = JSON.parseArray(toolCallsArray.toJSONString(), ToolCall.class);
                    }
                }
                message = new com.alibaba.cloud.ai.graph.event.message.AssistantMessage(id, content, "", toolCalls);
                break;
            case "system":
                message = new com.alibaba.cloud.ai.graph.event.message.SystemMessage(id, content, "");
                break;
            case "developer":
                message = new com.alibaba.cloud.ai.graph.event.message.DeveloperMessage(id, content, "");
                break;
            case "tool":
                message = new com.alibaba.cloud.ai.graph.event.message.ToolMessage(
                        id, content, "",
                        msgObj.getString("toolCallId"),
                        msgObj.getString("error")
                );
                break;
            case "user":
            default:
                message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
                break;
        }

        return message;
    }

    public static class Tools {

        @org.springframework.ai.tool.annotation.Tool( description = "Send an email to someone")
        public String sendEmail(
                @ToolParam( description = "destination address") String to,
                @ToolParam( description = "subject of the email") String subject,
                @ToolParam( description = "body of the email") String body
        ) {
            // This is a placeholder for the actual implementation
            return format("mail sent to %s with subject %s", to, subject);
        }

        @org.springframework.ai.tool.annotation.Tool( description = "Get the weather in location")
        public String queryWeather(@ToolParam( description = "The query to use in your search.") String query) {
            return "Cold, with a low of 13 degrees";
        }
    }
}
