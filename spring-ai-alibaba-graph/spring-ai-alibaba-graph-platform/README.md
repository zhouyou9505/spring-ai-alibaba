# Spring AI Alibaba Graph Platform

## 概述

Spring AI Alibaba Graph Platform 是一个低代码AI工作流平台，允许用户通过配置 `workflowSchema` 来定义和运行AI工作流，而无需预先编写代码。平台支持将工作流配置转换为基于 `NodeAction` 的Java代码，实现从低代码到代码的平滑过渡。

## 核心特性

- 🚀 **低代码工作流定义** - 通过JSON配置定义工作流
- 🔄 **动态工作流执行** - 无需重启即可注册和运行新工作流
- 🧩 **多种节点类型** - 支持LLM、工具、自定义、条件等节点类型
- 🔗 **条件分支支持** - 支持基于条件的动态路由
- 📝 **代码自动生成** - 将工作流配置转换为Java代码
- 🌐 **REST API接口** - 提供完整的HTTP API
- ⚡ **基于NodeAction** - 完全基于Spring AI Alibaba的NodeAction架构
- 🤖 **智能Copilot** - 通过自然语言描述智能调整工作流
- 🛠️ **MockTool支持** - 智能工具降级，确保工作流稳定性

## 快速开始

### 1. 启动平台

```bash
# 设置API密钥
export DASHSCOPE_API_KEY=your-api-key-here

# 启动应用
mvn spring-boot:run
```

### 2. 注册工作流

```bash
curl -X POST http://localhost:8080/api/workflow/register \
  -H "Content-Type: application/json" \
  -d @src/main/resources/examples/sample-workflow.json
```

### 3. 运行工作流

```bash
curl -X POST http://localhost:8080/api/workflow/sample-workflow-001/run \
  -H "Content-Type: application/json" \
  -d '{"input": "这是一个测试文本，需要进行分析和处理。"}'
```

### 4. 生成代码

```bash
curl -X POST http://localhost:8080/api/workflow/sample-workflow-001/generate-code
```

### 5. 创建 MockTool 演示工作流

```bash
curl -X POST http://localhost:8080/api/workflow/create-mock-tools-workflow
```

## MockTool 功能

### 概述

MockTool 是平台提供的一个智能模拟工具，用于处理工具不存在或不可用的情况。它能够根据工具名称、参数和描述生成真实的模拟响应，确保工作流能够正常运行。

### 特性

- **智能模拟**: 根据工具描述生成真实的模拟响应
- **优雅降级**: 当实际工具不可用时自动使用 MockTool
- **灵活配置**: 支持自定义工具名称、参数和描述

### 使用方式

#### 自动使用（推荐）

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

#### 显式配置

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

### 示例工作流

平台提供了 MockTool 演示工作流，可以通过以下方式创建：

```bash
# 创建 MockTool 演示工作流
curl -X POST http://localhost:8080/api/workflow/create-mock-tools-workflow

# 运行演示工作流
curl -X POST http://localhost:8080/api/workflow/mock-tools-workflow-demo/run \
  -H "Content-Type: application/json" \
  -d '{"user_topic": "人工智能的发展趋势"}'
```

详细的使用说明请参考：[MockTool 使用指南](src/main/resources/docs/mock-tool-guide.md)

## 智能Copilot功能

### 概述

CopilotController 是一个智能工作流调整助手，支持通过自然语言描述来创建、编辑和改进多Agent工作流。它使用LLM来理解用户需求并生成相应的WorkflowSchema配置。

### 支持的操作

通过自然语言描述支持以下操作：
1. **创建多Agent系统** - 根据描述创建完整的多Agent工作流
2. **创建新Agent** - 向现有工作流添加新的Agent
3. **编辑现有Agent** - 修改现有Agent的配置和行为
4. **改进Agent指令** - 优化Agent的指令和提示词
5. **管理工具** - 添加、编辑或删除工具
6. **管理提示词** - 添加、编辑或删除提示词
7. **优化工作流性能** - 添加并行处理、重试机制等
8. **添加条件逻辑和循环** - 支持复杂的流程控制
9. **配置并行执行** - 提高工作流执行效率
10. **其他工作流调整需求** - 任何其他工作流相关的调整

### 使用示例

#### 创建多Agent系统

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "userRequest": "创建一个智能客服系统，包含自动接待、问题分类、技术支持、情感分析和满意度评估功能"
  }'
```

#### 调整现有工作流

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "existing-workflow-id",
    "userRequest": "在现有工作流中添加一个数据分析Agent，用于分析客户交互数据并生成报告"
  }'
```

#### 复杂工作流调整

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "customer-service-workflow",
    "userRequest": "在现有的客服工作流中添加一个情感分析Agent，用于分析客户情绪并提供相应的服务策略。同时改进接待Agent，让它能够根据客户情绪调整回复语气。另外添加一个自动升级机制，当客户情绪为负面时自动转接人工客服。"
  }'
```

#### 性能优化

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "tech-support-workflow",
    "userRequest": "优化技术支持工作流，添加并行处理能力，让问题分类Agent和知识库Agent可以同时工作，提高响应速度。同时添加重试机制和超时处理。"
  }'
```

详细的使用示例请参考：[Copilot API 使用示例](src/main/resources/docs/copilot-api-examples.md)

## 工作流配置结构

### WorkflowSchema

```json
{
  "workflowId": "unique-workflow-id",
  "name": "工作流名称",
  "description": "工作流描述",
  "nodes": [...],
  "edges": [...],
  "globalConfig": {...}
}
```

### 节点类型

#### 1. React Agent (ReactAgent)
```json
{
  "agentId": "react_agent",
  "name": "React Agent",
  "type": "react",
  "description": "ReactAgent 用于处理复杂的推理任务",
  "instructions": "你是一个专业的AI助手，能够进行多步推理和工具调用",
  "model": "qwen-turbo",
  "config": {
    "prompt": "请分析以下问题：{input}",
    "maxIterations": 10,
    "inputKey": "input",
    "outputKey": "output",
    "tools": [...],
    "resolver": "..."
  }
}
```

#### 2. React Agent With Human (ReactAgentWithHuman)
```json
{
  "agentId": "react_human_agent",
  "name": "React Agent With Human",
  "type": "react_with_human",
  "description": "ReactAgentWithHuman 支持人机交互的推理任务",
  "instructions": "你是一个专业的AI助手，能够与人类进行交互式推理",
  "model": "qwen-turbo",
  "config": {
    "prompt": "请与用户交互解决以下问题：{input}",
    "maxIterations": 8,
    "inputKey": "input",
    "outputKey": "output",
    "tools": [...],
    "resolver": "..."
  }
}
```

#### 3. Reflect Agent (ReflectAgent)
```json
{
  "agentId": "reflect_agent",
  "name": "Reflect Agent",
  "type": "reflect",
  "description": "ReflectAgent 用于反思和改进内容质量",
  "instructions": "你是一个内容质量专家，负责反思和改进内容",
  "model": "qwen-turbo",
  "config": {
    "maxIterations": 5,
    "inputKey": "input",
    "outputKey": "output",
    "graphAction": "...",
    "reflectionAction": "..."
  }
}
```

### 边配置

```json
{
  "edgeId": "edge_1",
  "fromNodeId": "node_a",
  "toNodeId": "node_b",
  "condition": "result == 'success'",
  "config": {}
}
```

## API接口

### 工作流管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/workflow/register` | 注册工作流 |
| POST | `/api/workflow/{id}/run` | 运行工作流 |
| POST | `/api/workflow/{id}/generate-code` | 生成代码 |
| GET | `/api/workflow/list` | 获取工作流列表 |
| GET | `/api/workflow/{id}/status` | 检查工作流状态 |
| DELETE | `/api/workflow/{id}` | 移除工作流 |
| GET | `/api/workflow/info` | 获取平台信息 |

### 示例请求

#### 注册工作流
```bash
curl -X POST http://localhost:8080/api/workflow/register \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "my-workflow",
    "name": "我的工作流",
    "description": "示例工作流",
    "nodes": [...],
    "edges": [...]
  }'
```

#### 运行工作流
```bash
curl -X POST http://localhost:8080/api/workflow/my-workflow/run \
  -H "Content-Type: application/json" \
  -d '{"input": "测试数据"}'
```

## 代码生成

平台可以将工作流配置转换为基于 `NodeAction` 的Java代码，生成的代码包括：

- 完整的Spring Boot控制器
- 基于NodeAction的节点实现
- 图形化工作流构建代码
- REST API接口

生成的代码可以直接集成到现有的Spring Boot项目中。

## 架构设计

### 核心组件

1. **WorkflowSchema** - 工作流配置数据结构
2. **FlowRunner** - 工作流运行引擎
3. **NodeActionFactory** - 节点工厂，根据配置创建NodeAction
4. **CodeGenerator** - 代码生成器
5. **WorkflowService** - 工作流管理服务
6. **WorkflowController** - REST API控制器

### 执行流程

1. 用户提交 `WorkflowSchema` 配置
2. `FlowRunner` 解析配置并构建 `StateGraph`
3. `NodeActionFactory` 根据节点类型创建相应的 `NodeAction`
4. 工作流在运行时动态执行各个节点
5. 支持将工作流转换为Java代码

## 扩展性

### 添加新的节点类型

1. 在 `NodeActionFactory` 中添加新的节点类型处理
2. 在 `CodeGenerator` 中添加相应的代码生成逻辑
3. 更新API文档和示例

### 自定义逻辑

通过 `custom` 节点类型，可以执行自定义的业务逻辑：

```json
{
  "nodeType": "custom",
  "config": {
    "customLogic": "your-custom-logic"
  }
}
```

## 配置说明

### application.yml

```yaml
server:
  port: 8080

spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-turbo
          temperature: 0.7

workflow:
  platform:
    name: "Spring AI Alibaba Graph Platform"
    version: "1.0.0.3-SNAPSHOT"
```

## 最佳实践

1. **工作流设计**
   - 保持节点职责单一
   - 合理使用条件分支
   - 注意数据流的清晰性

2. **性能优化**
   - 避免过长的线性工作流
   - 合理使用并行节点
   - 监控工作流执行时间

3. **错误处理**
   - 在关键节点添加错误处理逻辑
   - 使用条件节点处理异常情况
   - 记录详细的执行日志

## 故障排除

### 常见问题

1. **工作流注册失败**
   - 检查JSON格式是否正确
   - 确认所有节点ID唯一
   - 验证边的连接关系

2. **节点执行失败**
   - 检查输入数据格式
   - 确认API密钥配置正确
   - 查看详细错误日志

3. **代码生成失败**
   - 确认工作流已正确注册
   - 检查节点配置完整性
   - 验证文件路径权限

## 贡献指南

欢迎提交Issue和Pull Request来改进这个平台。

## 许可证

Apache License 2.0 