package com.alibaba.agui.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CopilotKitRemoteEndpoint {

    private final List<Action> actions;
    private final List<Agent> agents;

    public CopilotKitRemoteEndpoint(List<Action> actions, List<Agent> agents) {
        this.actions = actions != null ? actions : new ArrayList<>();
        this.agents = agents != null ? agents : new ArrayList<>();
    }

    /**
     * info: 返回可用的 actions 与 agents 列表
     */
    public Map<String, Object> info(CopilotKitContext context) {
        List<Map<String, Object>> actionList = new ArrayList<>();
        for (Action action : actions) {
            actionList.add(action.dictRepr());
        }
        List<Map<String, Object>> agentList = new ArrayList<>();
        for (Agent agent : agents) {
            agentList.add(agent.dictRepr());
        }
        return Map.of(
                "sdkVersion", "java-0.1.0",
                "actions", actionList,
                "agents", agentList
        );
    }

    /**
     * 执行 Action
     */
    public CompletableFuture<Map<String, Object>> executeAction(
            CopilotKitContext context, String name, Map<String, Object> arguments) {
        Action action = actions.stream().filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new ActionNotFoundException(name));
        try {
            Object result = action.execute(arguments);
            return CompletableFuture.completedFuture(Map.of("result", result));
        } catch (Exception e) {
            throw new ActionExecutionException(name, e);
        }
    }

    /**
     * 执行 Agent（返回的是流式事件 Flux/NDJSON，你在 Spring Controller 已经封装）
     */
    public Iterable<String> executeAgent(
            CopilotKitContext context,
            String name,
            String threadId,
            Map<String, Object> state,
            Map<String, Object> config,
            List<Map<String, Object>> messages,
            List<Map<String, Object>> actions,
            String nodeName,
            List<Map<String, Object>> metaEvents
    ) {
        Agent agent = agents.stream().filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new AgentNotFoundException(name));
        try {
            return agent.execute(threadId, nodeName, state, config, messages, actions, metaEvents);
        } catch (Exception e) {
            throw new AgentExecutionException(name, e);
        }
    }

    /**
     * 获取 Agent 状态
     */
    public CompletableFuture<Map<String, Object>> getAgentState(
            CopilotKitContext context, String threadId, String name) {
        Agent agent = agents.stream().filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new AgentNotFoundException(name));
        try {
            return agent.getState(threadId);
        } catch (Exception e) {
            throw new AgentExecutionException(name, e);
        }
    }
}
