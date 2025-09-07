package com.alibaba.agui.sdk;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * CopilotKitContext 等价于 Python 里的 TypedDict
 * - properties: 前端传来的自定义属性
 * - frontendUrl: 前端页面 URL
 * - headers: HTTP 请求头
 */
public record CopilotKitContext(
        Map<String, Object> properties,
        String frontendUrl,
        Map<String, String> headers
) {}

/**
 * 自定义异常：Agent 没找到
 */




/**
 * 自定义异常：Action 执行失败
 */


/**
 * CopilotKitRemoteEndpoint 等价于 Python 的 sdk.py 里的类
 * - 管理 actions 和 agents
 * - 提供 info/executeAction/executeAgent/getAgentState
 */
