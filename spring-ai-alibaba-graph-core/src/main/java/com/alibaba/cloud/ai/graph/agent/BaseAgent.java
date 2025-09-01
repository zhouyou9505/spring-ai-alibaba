/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.event.event.AgentStartEvent;
import com.alibaba.cloud.ai.graph.event.event.AgentFinishedEvent;
import com.alibaba.cloud.ai.graph.event.manager.CallbackManager;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;

import org.springframework.scheduling.Trigger;

/**
 * Abstract base class for all agents in the graph system. Contains common properties and
 * methods shared by different agent implementations.
 */
public abstract class BaseAgent {

	/** The agent's name. Must be a unique identifier within the graph. */
	protected String name;

	/**
	 * One line description about the agent's capability. The system can use this for
	 * decision-making when delegating control to different agents.
	 */
	protected String description;

	/** The output key for the agent's result */
	protected String outputKey;

	/** The callback manager for handling lifecycle events */
	protected CallbackManager callbackManager;

	/**
	 * Protected constructor for initializing all base agent properties.
	 * @param name the unique name of the agent
	 * @param description the description of the agent's capability
	 * @param outputKey the output key for the agent's result
	 */
	protected BaseAgent(String name, String description, String outputKey,CallbackManager callbackManager) {
		this.name = name;
		this.description = description;
		this.outputKey = outputKey;
        this.callbackManager = callbackManager;
	}

	/**
	 * Default protected constructor for subclasses that need to initialize properties
	 * differently.
	 */
	protected BaseAgent() {
		// Allow subclasses to initialize properties through other means
	}

	/**
	 * 发送 Agent 开始事件
	 */
	protected void emitAgentStartEvent() {
		if (callbackManager != null) {
			AgentStartEvent event = new AgentStartEvent();
			event.setAgentId(this.name != null ? this.name : "Agent");
			event.setAgentName(this.name);
			callbackManager.onAgentStartEvent(event);
		}
	}

	/**
	 * 发送 Agent 结束事件
	 * @param error 错误信息，如果为null表示成功结束
	 */
	protected void emitAgentFinishedEvent(String error) {
		if (callbackManager != null) {
			AgentFinishedEvent event = new AgentFinishedEvent();
			event.setAgentId(this.name != null ? this.name : "Agent");
			event.setAgentName(this.name);
			if (error != null) {
				event.setError(error);
			}
			callbackManager.onAgentFinishEvent(event);
		}
	}

	/**
	 * Gets the agent's unique name.
	 * @return the unique name of the agent.
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 * @return the description of the agent.
	 */
	public String description() {
		return description;
	}

	/**
	 * Gets the output key for the agent's result.
	 * @return the output key.
	 */
	public String outputKey() {
		return outputKey;
	}

	/**
	 * Abstract a complex agent into a simple node in the graph.
	 * @return the list of sub-agents.
	 */
	public abstract AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException;

	public Optional<OverAllState> invoke(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		// 发送Agent开始事件
		emitAgentStartEvent();

		try {
			// 调用子类的具体实现
			Optional<OverAllState> result = doInvoke(input);

			// 发送Agent成功结束事件
			emitAgentFinishedEvent(null);
			return result;
		} catch (Exception e) {
			// 发送Agent异常结束事件
			emitAgentFinishedEvent(e.getMessage());
			throw e;
		}
	}

	/**
	 * 子类需要实现的具体invoke逻辑
	 * @param input 输入参数
	 * @return 执行结果
	 * @throws GraphStateException 状态异常
	 * @throws GraphRunnerException 运行异常
	 */
	protected abstract Optional<OverAllState> doInvoke(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException;

	/**
	 * Schedule the agent task with trigger.
	 * @param trigger the schedule configuration
	 * @param input the agent input
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public ScheduledAgentTask schedule(Trigger trigger, Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		ScheduleConfig scheduleConfig = ScheduleConfig.builder().trigger(trigger).inputs(input).build();
		return schedule(scheduleConfig);
	}

	/**
	 * Schedule the agent task with trigger.
	 * @param scheduleConfig the schedule configuration
	 * @return a ScheduledAgentTask instance for managing the scheduled task
	 */
	public abstract ScheduledAgentTask schedule(ScheduleConfig scheduleConfig)
			throws GraphStateException, GraphRunnerException;

	public abstract AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException;

}
