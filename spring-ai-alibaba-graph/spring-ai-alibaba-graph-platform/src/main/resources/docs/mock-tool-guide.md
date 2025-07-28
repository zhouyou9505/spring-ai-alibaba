# MockTool 使用指南

## 概述

MockTool 是 Spring AI Alibaba Graph 平台提供的一个智能模拟工具，用于处理工具不存在或不可用的情况。它能够根据工具名称、参数和描述生成真实的模拟响应，确保工作流能够正常运行。

## 特性

### 1. 智能模拟
- 根据工具名称和描述生成真实的模拟响应
- 支持自定义模拟指令
- 使用 LLM 生成符合上下文的响应

### 2. 优雅降级
- 当实际工具不可用时自动使用 MockTool
- 保持工作流的连续性和稳定性
- 提供详细的日志记录

### 3. 灵活配置
- 支持自定义工具名称、参数和描述
- 可配置模拟指令
- 与现有工具系统无缝集成

## 使用方法

### 1. 自动使用

当 Agent 没有配置工具时，系统会自动使用 MockTool：

```json
{
  "agentId": "research_agent",
  "name": "研究Agent",
  "type": "react",
  "config": {
    "prompt": "请研究以下主题：{topic}",
    "maxIterations": 8
    // 没有配置 tools，将自动使用 MockTool
  }
}
```

### 2. 显式配置

也可以显式配置 MockTool：

```json
{
  "agentId": "research_agent",
  "name": "研究Agent",
  "type": "react",
  "config": {
    "prompt": "请研究以下主题：{topic}",
    "maxIterations": 8,
    "tools": [
      {
        "name": "mock_tool",
        "description": "模拟工具执行",
        "parameters": {
          "toolName": "web_search",
          "args": "{query}",
          "description": "搜索网络信息",
          "mockInstructions": "模拟网络搜索，返回相关搜索结果"
        }
      }
    ]
  }
}
```

## 工具参数

MockTool 接受以下参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| toolName | String | 是 | 要模拟的工具名称 |
| args | String | 是 | 工具参数 |
| description | String | 是 | 工具描述 |
| mockInstructions | String | 是 | 模拟指令 |

## 示例场景

### 场景 1：网络搜索工具不可用

```json
{
  "agentId": "search_agent",
  "name": "搜索Agent",
  "type": "react",
  "config": {
    "prompt": "请搜索关于 {topic} 的信息",
    "maxIterations": 5
  }
}
```

当 Agent 尝试调用网络搜索工具时，MockTool 会：
1. 接收工具调用请求
2. 生成符合上下文的搜索结果
3. 返回模拟的搜索结果

### 场景 2：数据分析工具不可用

```json
{
  "agentId": "analysis_agent",
  "name": "分析Agent",
  "type": "react_with_human",
  "config": {
    "prompt": "请分析以下数据：{data}",
    "maxIterations": 10
  }
}
```

MockTool 会：
1. 模拟数据分析过程
2. 生成分析报告
3. 提供可视化建议

## 配置选项

### 1. 全局配置

在 `application.yml` 中配置 MockTool：

```yaml
spring:
  ai:
    mock-tool:
      enabled: true
      default-instructions: "生成真实的模拟响应"
      log-level: INFO
```

### 2. Agent 级别配置

在 Agent 配置中指定 MockTool 参数：

```json
{
  "agentId": "custom_agent",
  "name": "自定义Agent",
  "type": "react",
  "config": {
    "prompt": "请处理：{input}",
    "maxIterations": 5,
    "mockToolConfig": {
      "defaultInstructions": "生成专业的模拟响应",
      "enableLogging": true,
      "responseFormat": "json"
    }
  }
}
```

## 最佳实践

### 1. 合理使用
- 仅在开发或测试环境中使用 MockTool
- 生产环境中应配置真实的工具
- 定期检查工具可用性

### 2. 配置优化
- 为不同工具提供详细的描述
- 使用具体的模拟指令
- 配置合适的响应格式

### 3. 监控和日志
- 启用详细日志记录
- 监控 MockTool 的使用频率
- 定期评估模拟质量

## 故障排除

### 常见问题

1. **MockTool 响应质量不佳**
   - 检查工具描述是否详细
   - 优化模拟指令
   - 调整 LLM 参数

2. **工具调用失败**
   - 确认 MockTool 已正确注册
   - 检查参数格式
   - 查看错误日志

3. **性能问题**
   - 减少不必要的工具调用
   - 优化提示词设计
   - 考虑缓存机制

### 调试技巧

1. 启用详细日志：
```yaml
logging:
  level:
    com.alibaba.cloud.ai.service.MockTool: DEBUG
```

2. 使用测试数据验证：
```json
{
  "toolName": "test_tool",
  "args": "test_args",
  "description": "测试工具",
  "mockInstructions": "生成测试响应"
}
```

3. 监控工具调用：
```java
// 在代码中添加监控
log.info("MockTool 被调用: {}", toolName);
```

## 扩展功能

### 1. 自定义 MockTool

可以创建自定义的 MockTool 实现：

```java
@Component
public class CustomMockTool implements ToolCallback {
    
    @Override
    public String getName() {
        return "custom_mock_tool";
    }
    
    @Override
    public Object call(Map<String, Object> arguments) {
        // 自定义模拟逻辑
        return "Custom mock response";
    }
}
```

### 2. 工具链集成

MockTool 可以与工具链集成：

```java
@Configuration
public class ToolConfiguration {
    
    @Bean
    public ToolFactory toolFactory(ChatClient chatClient) {
        ToolFactory factory = new ToolFactory(chatClient);
        factory.registerTool(new CustomMockTool());
        return factory;
    }
}
```

## 总结

MockTool 为 Spring AI Alibaba Graph 平台提供了强大的工具降级能力，确保工作流在各种情况下都能正常运行。通过合理配置和使用，可以大大提升系统的稳定性和用户体验。 