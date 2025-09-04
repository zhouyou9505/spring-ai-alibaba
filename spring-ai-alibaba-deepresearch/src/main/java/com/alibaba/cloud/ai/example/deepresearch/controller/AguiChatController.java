/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import com.alibaba.cloud.ai.example.deepresearch.controller.graph.GraphProcess;
import com.alibaba.cloud.ai.example.deepresearch.model.req.FeedbackRequest;
import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
import com.alibaba.cloud.ai.example.deepresearch.model.response.ReportResponse;
import com.alibaba.cloud.ai.example.deepresearch.util.SearchBeanUtil;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.observation.GraphObservationLifecycleListener;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManagerImpl;
import com.alibaba.cloud.ai.graph.event.manager.EventHandler;
import com.alibaba.cloud.ai.graph.event.context.Context;
import com.alibaba.cloud.ai.graph.event.agent.RunAgentInput;
import com.alibaba.cloud.ai.graph.event.message.BaseMessage;
import com.alibaba.cloud.ai.graph.event.tool.Tool;
import com.alibaba.cloud.ai.graph.event.state.State;
import com.alibaba.fastjson.JSON;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.http.codec.ServerSentEvent;

/**
 * @author yingzi
 * @since 2025/5/17 19:27
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("")
public class AguiChatController {

	private static final Logger logger = LoggerFactory.getLogger(AguiChatController.class);

	private final CompiledGraph compiledGraph;

	private final GraphProcess graphProcess;

	private final SearchBeanUtil searchBeanUtil;

	@Autowired
	public AguiChatController(@Qualifier("deepResearch") StateGraph stateGraph, SearchBeanUtil searchBeanUtil,
                              ObjectProvider<ObservationRegistry> observationRegistry, DeepResearchProperties deepResearchProperties)
			throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().register(SaverEnum.MEMORY.getValue(), new MemorySaver()).build();
		this.compiledGraph = stateGraph.compile(CompileConfig.builder()
			.saverConfig(saverConfig)
			.interruptBefore("human_feedback")
			.withLifecycleListener(new GraphObservationLifecycleListener(
					observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)))
			.build());
		this.compiledGraph.setMaxIterations(deepResearchProperties.getMaxIterations());
		this.searchBeanUtil = searchBeanUtil;
		this.graphProcess = new GraphProcess(this.compiledGraph);
		logger.info("ChatController initialized with graph maxIterations: {}",
				deepResearchProperties.getMaxIterations());
	}

	/**
	 * SSE (Server-Sent Events) endpoint for chat streaming.
	 *
	 * Accepts a JSON string and returns a SseEmitter that streams chat responses.
	 * Supports both initial questions and human feedback handling.
	 */
	@PostMapping(value = "/copilotkit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter chatStream(String inputStr) throws Exception {
		// 打印入参用于调试
		System.out.println("收到请求参数: " + inputStr);

		// 解析JSON并转换字段映射
		com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(inputStr);

		// 创建适配的RunAgentInput
		RunAgentInput input = createAdaptedRunAgentInput(jsonObject);

		// 创建 SseEmitter
		SseEmitter emitter = new SseEmitter(0L); // 无超时

		// 创建回调管理器，传入 EventHandler 实例
		CallbackManager callbackManager = new CallbackManagerImpl(new EventHandler(event -> {
			if (emitter == null) {
				return;
			}
			try {
				logger.info(JSON.toJSONString(event));
				emitter.send(SseEmitter.event()
						.name(event.getClass().getSimpleName())
						.data(event));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}));

		// 设置 CompiledGraph 的回调管理器
		if (compiledGraph != null) {
			compiledGraph.setCallbackManager(callbackManager);
		}
        // 构建输入参数
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("messages",input.messages());
        objectMap.put("threadId", input.threadId());
        objectMap.put("runId", input.runId());
        objectMap.put("tools", input.tools());
        objectMap.put("context", input.context());
        objectMap.put("forwardedProps", input.forwardedProps());

        logger.info("init inputs: {}", objectMap);

		// 异步启动 Graph 执行
		CompletableFuture.runAsync(() -> {
			try {
				// 执行 Graph
                compiledGraph.invoke(objectMap);

			} catch (Exception e) {
				try {
					emitter.completeWithError(e);
				} catch (Exception ex) {
					// 静默处理
				}
			} finally {
				try {
					emitter.complete();
				} catch (Exception e) {
					// 静默处理
				}
			}
		});

		return emitter;
	}

	@DeleteMapping("/stop")
	public ReportResponse<?> stopGraph(@RequestBody GraphId graphId) {
		return graphProcess.stopGraph(graphId) ? ReportResponse.success(graphId.threadId(), "Success", null)
				: ReportResponse.error(graphId.threadId(), "Failure");
	}

	@PostMapping(value = "/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> resume(@RequestBody(required = false) FeedbackRequest humanFeedback)
			throws GraphRunnerException {
		RunnableConfig runnableConfig = RunnableConfig.builder().threadId(humanFeedback.threadId()).build();
		Map<String, Object> objectMap = new HashMap<>();
		objectMap.put("feed_back", humanFeedback.feedBack());
		objectMap.put("feed_back_content", humanFeedback.feedBackContent());

		// Create a unicast sink to emit ServerSentEvents
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		GraphProcess graphProcess = new GraphProcess(this.compiledGraph);

		StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
		OverAllState state = stateSnapshot.state();
		state.withResume();
		state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap, "research_team"));

		AsyncGenerator<NodeOutput> resultFuture = compiledGraph.streamFromInitialNode(state, runnableConfig);
		graphProcess.processStream(new GraphId(humanFeedback.sessionId(), humanFeedback.threadId()), resultFuture,
				sink);

		return sink.asFlux()
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming", e));
	}

	/**
	 * 创建适配的RunAgentInput，处理字段映射
	 */
	private RunAgentInput createAdaptedRunAgentInput(com.alibaba.fastjson.JSONObject jsonObject) {
		String threadId = jsonObject.getString("threadId");
		String runId = jsonObject.getString("runId");

		// 转换 messages
		List<BaseMessage> messages = convertJsonMessages(jsonObject.getJSONArray("messages"));

		// 转换 actions 到 tools
		List<Tool> tools = convertJsonActions(jsonObject.getJSONArray("actions"));

		// 创建空的 State
		State state = new State();

		// 转换 agentStates 到 State
		if (jsonObject.getJSONArray("agentStates") != null) {
			// 这里可以根据需要处理 agentStates
			System.out.println("处理 agentStates: " + jsonObject.getJSONArray("agentStates"));
		}

		// 转换 forwardedParameters 到 forwardedProps
		Object forwardedProps = jsonObject.get("forwardedParameters");

		// 创建空的 context 列表
		List<Context> context = new ArrayList<>();

		return new RunAgentInput(threadId, runId, state, messages, tools, context, forwardedProps);
	}

	/**
	 * 转换JSON messages到BaseMessage列表
	 */
	private List<BaseMessage> convertJsonMessages(com.alibaba.fastjson.JSONArray messagesArray) {
		if (messagesArray == null || messagesArray.isEmpty()) {
			return new ArrayList<>();
		}

		List<BaseMessage> messages = new ArrayList<>();
		for (int i = 0; i < messagesArray.size(); i++) {
			com.alibaba.fastjson.JSONObject msgObj = messagesArray.getJSONObject(i);
			// 根据role创建对应的消息类型
			BaseMessage message = createMessageByRole(msgObj);
			if (message != null) {
				messages.add(message);
			}
		}

		return messages;
	}

	/**
	 * 转换JSON actions到Tool列表
	 */
	private List<Tool> convertJsonActions(com.alibaba.fastjson.JSONArray actionsArray) {
		if (actionsArray == null || actionsArray.isEmpty()) {
			return new ArrayList<>();
		}

		List<Tool> tools = new ArrayList<>();
		for (int i = 0; i < actionsArray.size(); i++) {
			com.alibaba.fastjson.JSONObject actionObj = actionsArray.getJSONObject(i);
			String name = actionObj.getString("name");
			String description = actionObj.getString("description");
			String jsonSchema = actionObj.getString("jsonSchema");

			// 解析JSON Schema并创建ToolParameters
			Tool.ToolParameters parameters = parseJsonSchema(jsonSchema);

			Tool tool = new Tool(name, description, parameters);
			tools.add(tool);
		}

		return tools;
	}

	/**
	 * 解析JSON Schema并创建ToolParameters
	 */
	private Tool.ToolParameters parseJsonSchema(String jsonSchema) {
		try {
			com.alibaba.fastjson.JSONObject schema = JSON.parseObject(jsonSchema);
			String type = schema.getString("type");

			// 解析properties
			Map<String, Tool.ToolProperty> properties = new HashMap<>();
			com.alibaba.fastjson.JSONObject props = schema.getJSONObject("properties");
			if (props != null) {
				for (String key : props.keySet()) {
					com.alibaba.fastjson.JSONObject prop = props.getJSONObject(key);
					String propType = prop.getString("type");
					String propDesc = prop.getString("description");
					if (propDesc == null) propDesc = "";

					properties.put(key, new Tool.ToolProperty(propType, propDesc));
				}
			}

			// 解析required
			List<String> required = new ArrayList<>();
			com.alibaba.fastjson.JSONArray reqArray = schema.getJSONArray("required");
			if (reqArray != null) {
				for (int i = 0; i < reqArray.size(); i++) {
					required.add(reqArray.getString(i));
				}
			}

			return new Tool.ToolParameters(type, properties, required);
		} catch (Exception e) {
			System.err.println("解析JSON Schema失败: " + e.getMessage());
			// 返回默认值
			return new Tool.ToolParameters("object", new HashMap<>(), new ArrayList<>());
		}
	}

	/**
	 * 根据JSONObject创建对应的消息类型
	 */
	private BaseMessage createMessageByRole(com.alibaba.fastjson.JSONObject msgObj) {
		BaseMessage message = null;
		String role = msgObj.getString("role");
		String content = msgObj.getString("content");
		String id = msgObj.getString("id");

		switch (role.toLowerCase()) {
			case "user":
				message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
				break;
			case "assistant":
				List<com.alibaba.cloud.ai.graph.event.tool.ToolCall> toolCalls = new ArrayList<>();
				if (msgObj.containsKey("toolCalls") && msgObj.getJSONArray("toolCalls") != null) {
					com.alibaba.fastjson.JSONArray toolCallsArray = msgObj.getJSONArray("toolCalls");
					if (!toolCallsArray.isEmpty()) {
						toolCalls = JSON.parseArray(toolCallsArray.toJSONString(), com.alibaba.cloud.ai.graph.event.tool.ToolCall.class);
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
				message = new com.alibaba.cloud.ai.graph.event.message.ToolMessage(id, content, "",
						msgObj.getString("toolCallId")
						, msgObj.getString("error"));
				break;
			default:
				System.err.println("未知的消息角色: " + role + "，使用UserMessage作为默认值");
				message = new com.alibaba.cloud.ai.graph.event.message.UserMessage(id, content, "");
				break;
		}

		System.out.println("创建消息 - role: " + role + ", id: " + id + ", content: " + content);
		return message;
	}
}
