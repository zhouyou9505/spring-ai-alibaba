# 新 Agent 类型使用指南

本文档详细介绍了 Spring AI Alibaba Graph 平台支持的三种 Agent 类型：ReactAgent、ReactAgentWithHuman 和 ReflectAgent。

## 概述

Spring AI Alibaba Graph 平台现在支持三种核心的 Agent 类型，每种类型都有其特定的用途和优势：

1. **ReactAgent** - 用于复杂的推理任务和工具调用
2. **ReactAgentWithHuman** - 支持人机交互的推理任务
3. **ReflectAgent** - 用于反思和改进内容质量

## 1. ReactAgent

ReactAgent 是最基础的 Agent 类型，适用于需要进行多步推理和工具调用的任务。

### 特点
- 支持多步推理
- 内置工具调用能力
- 自动迭代直到完成任务
- 可配置最大迭代次数

### 配置示例

```json
{
  "agentId": "research_agent",
  "name": "研究Agent",
  "type": "react",
  "description": "进行深度研究和分析",
  "instructions": "你是一个专业的研究员，擅长收集和分析信息。",
  "model": "qwen-turbo",
  "config": {
    "prompt": "请深入研究以下主题：{topic}",
    "maxIterations": 10,
    "inputKey": "topic",
    "outputKey": "research_result",
    "tools": [
      {
        "name": "web_search",
        "description": "搜索网络信息"
      },
      {
        "name": "data_analysis",
        "description": "分析数据"
      }
    ]
  }
}
```

### 使用场景
- 信息收集和研究
- 数据分析
- 问题解决
- 知识推理

## 2. ReactAgentWithHuman

ReactAgentWithHuman 在 ReactAgent 的基础上增加了人机交互能力，适用于需要人类参与和指导的任务。

### 特点
- 支持人机交互
- 可以在推理过程中暂停等待人类输入
- 支持条件性中断
- 保持对话上下文

### 配置示例

```json
{
  "agentId": "collaborative_agent",
  "name": "协作Agent",
  "type": "react_with_human",
  "description": "与人类协作完成任务",
  "instructions": "你是一个协作助手，能够与人类一起解决问题。",
  "model": "qwen-turbo",
  "config": {
    "prompt": "请与用户协作解决以下问题：{problem}",
    "maxIterations": 8,
    "inputKey": "problem",
    "outputKey": "solution",
    "tools": [
      {
        "name": "ask_human",
        "description": "向人类询问信息"
      },
      {
        "name": "confirm_with_human",
        "description": "与人类确认结果"
      }
    ]
  }
}
```

### 使用场景
- 创意协作
- 决策支持
- 内容创作
- 问题诊断

## 3. ReflectAgent

ReflectAgent 专门用于反思和改进内容质量，通过迭代反思来提升输出质量。

### 特点
- 支持内容反思和改进
- 可配置反思策略
- 自动迭代优化
- 质量评估机制

### 配置示例

```json
{
  "agentId": "quality_agent",
  "name": "质量Agent",
  "type": "reflect",
  "description": "反思和改进内容质量",
  "instructions": "你是一个内容质量专家，负责反思和改进内容。",
  "model": "qwen-turbo",
  "config": {
    "maxIterations": 5,
    "inputKey": "content",
    "outputKey": "improved_content",
    "graphAction": {
      "type": "custom",
      "logic": "process_content"
    },
    "reflectionAction": {
      "type": "custom",
      "logic": "evaluate_quality"
    }
  }
}
```

### 使用场景
- 内容质量改进
- 代码审查
- 文档优化
- 创意作品完善

## 配置参数说明

### 通用参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | String | 是 | Agent 的唯一标识符 |
| name | String | 是 | Agent 的显示名称 |
| type | String | 是 | Agent 类型（react/react_with_human/reflect） |
| description | String | 是 | Agent 的功能描述 |
| instructions | String | 是 | Agent 的具体指令 |
| model | String | 是 | 使用的 AI 模型 |
| config | Object | 是 | Agent 的具体配置 |

### ReactAgent 特定参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| prompt | String | - | 提示词模板 |
| maxIterations | Integer | 10 | 最大迭代次数 |
| tools | Array | - | 可用工具列表 |
| resolver | Object | - | 工具解析器 |

### ReactAgentWithHuman 特定参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| prompt | String | - | 提示词模板 |
| maxIterations | Integer | 10 | 最大迭代次数 |
| tools | Array | - | 可用工具列表 |
| resolver | Object | - | 工具解析器 |

### ReflectAgent 特定参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| maxIterations | Integer | 5 | 最大迭代次数 |
| graphAction | Object | - | 图形处理动作 |
| reflectionAction | Object | - | 反思动作 |

## 最佳实践

### 1. 选择合适的 Agent 类型

- **ReactAgent**: 适用于自动化推理任务
- **ReactAgentWithHuman**: 适用于需要人类参与的协作任务
- **ReflectAgent**: 适用于内容质量改进任务

### 2. 配置优化

- 合理设置 `maxIterations` 避免无限循环
- 为 ReactAgent 和 ReactAgentWithHuman 配置合适的工具
- 为 ReflectAgent 设计有效的反思策略

### 3. 错误处理

- 配置适当的重试机制
- 设置合理的超时时间
- 实现错误恢复逻辑

### 4. 性能优化

- 使用合适的模型
- 优化提示词设计
- 合理配置迭代次数

## 示例工作流

### 博客撰写工作流

```json
{
  "workflowId": "blog-writing-workflow",
  "name": "博客撰写工作流",
  "agents": [
    {
      "agentId": "topic_research",
      "type": "react",
      "name": "主题研究",
      "config": {
        "prompt": "研究主题：{topic}",
        "maxIterations": 5
      }
    },
    {
      "agentId": "content_generation",
      "type": "react_with_human",
      "name": "内容生成",
      "config": {
        "prompt": "生成博客内容：{research}",
        "maxIterations": 8
      }
    },
    {
      "agentId": "content_improvement",
      "type": "reflect",
      "name": "内容改进",
      "config": {
        "maxIterations": 3
      }
    }
  ]
}
```

## 故障排除

### 常见问题

1. **Agent 创建失败**
   - 检查配置参数是否正确
   - 确认模型名称有效
   - 验证工具配置

2. **迭代次数过多**
   - 调整 `maxIterations` 参数
   - 优化提示词设计
   - 检查工具配置

3. **性能问题**
   - 选择合适的模型
   - 优化配置参数
   - 考虑并行处理

### 调试技巧

1. 启用详细日志
2. 使用测试数据验证
3. 逐步调试复杂配置
4. 监控资源使用情况

## 总结

Spring AI Alibaba Graph 平台的三种 Agent 类型为不同的应用场景提供了专门化的解决方案。通过合理选择和配置这些 Agent，可以构建高效、智能的工作流系统。 