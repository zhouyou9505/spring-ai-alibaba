# AG-UI Controller 使用说明

## 概述

`AguiController` 是一个实现了 AG-UI 协议的控制器，用于处理 AI 代理的聊天请求。它支持流式响应和标准的 AG-UI 事件，完全符合 Java AG-UI SDK 标准。

## 功能特性

- ✅ 支持 AG-UI 协议标准
- ✅ 流式聊天响应 (Server-Sent Events)
- ✅ 完整的事件类型支持 (所有标准 AG-UI 事件)
- ✅ 异步处理请求
- ✅ 健康检查和协议信息端点
- ✅ 演示端点 (展示所有事件类型)
- ✅ 支持 RunAgentInput 标准输入格式

## API 端点

### 1. 运行代理端点 (主要端点)

**POST** `/api/agui/run`

处理 AG-UI 代理执行请求，返回 Server-Sent Events 流。这是标准的 AG-UI 端点。

**请求体示例:**
```json
{
  "threadId": "thread-12345",
  "runId": "run-67890",
  "state": null,
  "messages": [
    {
      "id": "msg-1",
      "content": "Hello, how are you?",
      "name": null,
      "role": "user"
    }
  ],
  "tools": null,
  "context": null,
  "forwardedProps": null
}
```

**响应事件流:**
```
event: event
data: {"type":"RUN_STARTED","threadId":"thread-12345","runId":"run-67890","timestamp":1234567890}

event: event
data: {"type":"STEP_STARTED","rawEvent":{"runId":"run-67890","stepId":"agent-processing","stepType":"agent"},"timestamp":1234567890}

event: event
data: {"type":"TEXT_MESSAGE_START","messageId":"msg-abc","rawEvent":{"runId":"run-67890"},"timestamp":1234567890}

event: event
data: {"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-abc","delta":"Hello!","rawEvent":{"runId":"run-67890"},"timestamp":1234567890}

...

event: event
data: {"type":"TEXT_MESSAGE_END","messageId":"msg-abc","rawEvent":{"runId":"run-67890"},"timestamp":1234567890}

event: event
data: {"type":"STEP_FINISHED","rawEvent":{"runId":"run-67890","stepId":"agent-processing","stepType":"agent"},"timestamp":1234567890}

event: event
data: {"type":"RUN_FINISHED","rawEvent":{"runId":"run-67890"},"timestamp":1234567890}
```

### 2. 聊天端点 (简化版)

**POST** `/api/agui/chat`

处理简化的聊天请求，内部转换为 RunAgentInput 格式。

**请求体示例:**
```json
{
  "runId": "chat-12345",
  "threadId": "thread-67890",
  "content": "Hello, how are you?",
  "model": "gpt-4",
  "config": {
    "temperature": 0.7,
    "max_tokens": 1000
  }
}
```

### 3. 演示端点

**POST** `/api/agui/demo`

演示所有 AG-UI 事件类型，支持选择性演示。

**请求体示例:**
```json
{
  "runId": "demo-12345",
  "threadId": "thread-demo",
  "demoType": "full",
  "config": {
    "delay": 200
  }
}
```

**支持的演示类型:**
- `"full"` - 演示所有事件类型 (默认)
- `"lifecycle"` - 仅演示生命周期事件
- `"text"` - 仅演示文本消息事件
- `"tool"` - 仅演示工具调用事件
- `"state"` - 仅演示状态管理事件
- `"thinking"` - 仅演示思考事件
- `"custom"` - 仅演示自定义事件

### 4. 健康检查

**GET** `/api/agui/health`

返回服务健康状态。

**响应示例:**
```json
{
  "status": "healthy",
  "protocol": "AG-UI",
  "version": "1.0.0",
  "timestamp": 1234567890
}
```

### 5. 协议信息

**GET** `/api/agui/protocol`

返回支持的 AG-UI 协议特性。

**响应示例:**
```json
{
  "name": "AG-UI Protocol",
  "version": "1.0.0",
  "features": [
    "agentic_chat",
    "streaming_responses",
    "tool_calls",
    "run_events",
    "step_events",
    "state_management",
    "thinking_events",
    "custom_events"
  ],
  "supported_events": [
    "TEXT_MESSAGE_START", "TEXT_MESSAGE_CONTENT", "TEXT_MESSAGE_END", "TEXT_MESSAGE_CHUNK",
    "THINKING_START", "THINKING_END",
    "THINKING_TEXT_MESSAGE_START", "THINKING_TEXT_MESSAGE_CONTENT", "THINKING_TEXT_MESSAGE_END",
    "TOOL_CALL_START", "TOOL_CALL_ARGS", "TOOL_CALL_END", "TOOL_CALL_CHUNK", "TOOL_CALL_RESULT",
    "STATE_SNAPSHOT", "STATE_DELTA", "MESSAGES_SNAPSHOT",
    "RUN_STARTED", "RUN_FINISHED", "RUN_ERROR",
    "STEP_STARTED", "STEP_FINISHED",
    "RAW", "CUSTOM"
  ]
}
```

## 事件类型详解

### 🔄 生命周期事件 (Lifecycle Events)

#### RUN_STARTED
当代理运行开始时发送。
```json
{
  "type": "RUN_STARTED",
  "threadId": "thread-12345",
  "runId": "run-67890",
  "timestamp": 1234567890
}
```

#### RUN_FINISHED
当代理运行完成时发送。
```json
{
  "type": "RUN_FINISHED",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

#### RUN_ERROR
当发生错误时发送。
```json
{
  "type": "RUN_ERROR",
  "rawEvent": {
    "runId": "run-67890",
    "error": "Error message"
  },
  "timestamp": 1234567890
}
```

#### STEP_STARTED
当处理步骤开始时发送。
```json
{
  "type": "STEP_STARTED",
  "rawEvent": {
    "runId": "run-67890",
    "stepId": "agent-processing",
    "stepType": "agent"
  },
  "timestamp": 1234567890
}
```

#### STEP_FINISHED
当处理步骤完成时发送。
```json
{
  "type": "STEP_FINISHED",
  "rawEvent": {
    "runId": "run-67890",
    "stepId": "agent-processing",
    "stepType": "agent"
  },
  "timestamp": 1234567890
}
```

### 💬 文本消息事件 (Text Message Events)

#### TEXT_MESSAGE_START
当文本消息开始处理时发送。
```json
{
  "type": "TEXT_MESSAGE_START",
  "messageId": "msg-abc",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

#### TEXT_MESSAGE_CONTENT
流式文本响应的每个块。
```json
{
  "type": "TEXT_MESSAGE_CONTENT",
  "messageId": "msg-abc",
  "delta": "Hello!",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

#### TEXT_MESSAGE_END
当文本消息处理完成时发送。
```json
{
  "type": "TEXT_MESSAGE_END",
  "messageId": "msg-abc",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

### 🧠 思考事件 (Thinking Events)

#### THINKING_START
当AI开始思考时发送。
```json
{
  "type": "THINKING_START",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

#### THINKING_END
当AI完成思考时发送。
```json
{
  "type": "THINKING_END",
  "rawEvent": {
    "runId": "run-67890"
  },
  "timestamp": 1234567890
}
```

### 🛠️ 工具调用事件 (Tool Call Events)

#### TOOL_CALL_START
当工具调用开始时发送。
```json
{
  "type": "TOOL_CALL_START",
  "rawEvent": {
    "runId": "run-67890",
    "toolCallId": "tool-123",
    "toolName": "demo_tool"
  },
  "timestamp": 1234567890
}
```

#### TOOL_CALL_ARGS
工具调用的参数信息。
```json
{
  "type": "TOOL_CALL_ARGS",
  "rawEvent": {
    "runId": "run-67890",
    "toolCallId": "tool-123",
    "args": {
      "param1": "value1",
      "param2": "value2",
      "param3": {"nested": "value"}
    }
  },
  "timestamp": 1234567890
}
```

#### TOOL_CALL_END
当工具调用完成时发送。
```json
{
  "type": "TOOL_CALL_END",
  "rawEvent": {
    "runId": "run-67890",
    "toolCallId": "tool-123",
    "result": "Tool execution completed successfully"
  },
  "timestamp": 1234567890
}
```

### 📊 状态管理事件 (State Management Events)

#### STATE_SNAPSHOT
发送完整的状态快照。
```json
{
  "type": "STATE_SNAPSHOT",
  "rawEvent": {
    "runId": "run-67890",
    "state": {
      "conversationId": "conv-123",
      "userId": "user-456",
      "sessionData": {
        "startTime": 1234567890,
        "messageCount": 5,
        "context": "demo session"
      }
    }
  },
  "timestamp": 1234567890
}
```

#### STATE_DELTA
发送状态变化增量。
```json
{
  "type": "STATE_DELTA",
  "rawEvent": {
    "runId": "run-67890",
    "delta": {
      "operation": "update",
      "path": "sessionData.messageCount",
      "value": 6,
      "previousValue": 5
    }
  },
  "timestamp": 1234567890
}
```

#### MESSAGES_SNAPSHOT
发送消息历史快照。
```json
{
  "type": "MESSAGES_SNAPSHOT",
  "rawEvent": {
    "runId": "run-67890",
    "messages": [
      "Hello, how can I help you?",
      "I need assistance with AG-UI events",
      "I'll demonstrate all event types for you"
    ]
  },
  "timestamp": 1234567890
}
```

### 🎯 特殊事件 (Special Events)

#### RAW
发送原始格式的事件数据。
```json
{
  "type": "RAW",
  "rawEvent": {
    "runId": "run-67890",
    "rawData": "This is raw event data in any format",
    "format": "text"
  },
  "timestamp": 1234567890
}
```

#### CUSTOM
发送自定义事件。
```json
{
  "type": "CUSTOM",
  "rawEvent": {
    "runId": "run-67890",
    "customType": "demo_custom_event",
    "customData": {
      "feature": "custom_events",
      "description": "This demonstrates custom event types",
      "metadata": {
        "version": "1.0.0",
        "author": "AG-UI Demo"
      }
    }
  },
  "timestamp": 1234567890
}
```

## 使用示例

### JavaScript 客户端示例

```javascript
// 运行代理 (标准方式)
fetch('/api/agui/run', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    threadId: 'thread-' + Date.now(),
    runId: 'run-' + Date.now(),
    messages: [{
      id: 'msg-1',
      content: 'Hello, how are you?',
      role: 'user'
    }]
  })
});

// 简化聊天
fetch('/api/agui/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    runId: 'chat-' + Date.now(),
    threadId: 'thread-' + Date.now(),
    content: 'Hello, how are you?'
  })
});

// 演示所有事件类型
fetch('/api/agui/demo', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    runId: 'demo-' + Date.now(),
    threadId: 'thread-demo',
    demoType: 'full'
  })
});

// 监听事件流
const eventSource = new EventSource('/api/agui/run');

eventSource.addEventListener('event', (event) => {
  const data = JSON.parse(event.data);
  
  switch (data.type) {
    case 'RUN_STARTED':
      console.log('Agent started:', data.runId);
      break;
      
    case 'STEP_STARTED':
      console.log('Step started:', data.rawEvent.stepId);
      break;
      
    case 'TEXT_MESSAGE_START':
      console.log('Message started:', data.messageId);
      break;
      
    case 'TEXT_MESSAGE_CONTENT':
      console.log('Received chunk:', data.delta);
      break;
      
    case 'TEXT_MESSAGE_END':
      console.log('Message ended:', data.messageId);
      break;
      
    case 'STEP_FINISHED':
      console.log('Step finished:', data.rawEvent.stepId);
      break;
      
    case 'RUN_FINISHED':
      console.log('Agent finished');
      eventSource.close();
      break;
      
    case 'RUN_ERROR':
      console.error('Error:', data.rawEvent.error);
      eventSource.close();
      break;
      
    default:
      console.log('Unknown event type:', data.type, data);
      break;
  }
});
```

### cURL 示例

```bash
# 健康检查
curl -X GET http://localhost:8080/api/agui/health

# 协议信息
curl -X GET http://localhost:8080/api/agui/protocol

# 运行代理 (标准方式)
curl -X POST http://localhost:8080/api/agui/run \
  -H "Content-Type: application/json" \
  -d '{
    "threadId": "thread-123",
    "runId": "run-456",
    "messages": [{
      "id": "msg-1",
      "content": "Hello",
      "role": "user"
    }]
  }'

# 简化聊天
curl -X POST http://localhost:8080/api/agui/chat \
  -H "Content-Type: application/json" \
  -d '{
    "runId": "chat-123",
    "threadId": "thread-456",
    "content": "Hello"
  }'

# 演示所有事件类型
curl -X POST http://localhost:8080/api/agui/demo \
  -H "Content-Type: application/json" \
  -d '{
    "runId": "demo-123",
    "threadId": "thread-demo",
    "demoType": "full"
  }'

# 演示特定事件类型
curl -X POST http://localhost:8080/api/agui/demo \
  -H "Content-Type: application/json" \
  -d '{
    "runId": "demo-456",
    "threadId": "thread-demo",
    "demoType": "thinking"
  }'
```

## 配置说明

### 环境变量

确保以下配置正确：

```properties
# 服务器端口
server.port=8080

# 跨域配置
spring.web.cors.allowed-origins=*
```

### 依赖要求

确保项目中包含以下依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## 扩展说明

### 添加新的 AI 模型

在 `AguiServiceImpl` 中修改 `generateSampleResponse` 方法，集成实际的 AI 模型调用。

### 添加工具调用支持

实现完整的工具调用流程，支持 AI 代理的工具调用功能。

### 添加记忆功能

在 `RunAgentInput` 中利用 `state` 字段，支持对话历史记忆。

### 自定义事件类型

使用 `RAW` 和 `CUSTOM` 事件类型发送自定义数据格式。

## 故障排除

### 常见问题

1. **SSE 连接失败**
   - 检查防火墙设置
   - 确认服务器支持 SSE

2. **事件不显示**
   - 检查浏览器控制台错误
   - 确认事件监听器正确设置

3. **响应延迟**
   - 检查 AI 模型响应时间
   - 调整流式响应的块大小

4. **事件类型不匹配**
   - 确认客户端支持所有事件类型
   - 检查事件数据结构

5. **RunAgentInput 格式错误**
   - 确认 threadId 和 runId 已设置
   - 检查 messages 数组格式

### 日志调试

启用 DEBUG 日志级别：

```properties
logging.level.com.alibaba.cloud.ai.studio.admin=DEBUG
```

## 参考资源

- [AG-UI 协议文档](https://docs.ag-ui.com/llms-full.txt)
- [Java AG-UI SDK](https://github.com/ag-ui/ag-ui-java)
- [Spring Boot SSE 文档](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/async.html#sse)
- [Server-Sent Events 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html) 