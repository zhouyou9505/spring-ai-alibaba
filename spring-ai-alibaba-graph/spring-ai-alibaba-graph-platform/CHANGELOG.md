# 更新日志

## [1.0.0.4-SNAPSHOT] - 2024-01-01

### 🆕 新增功能

#### MCP 工具管理系统
- **McpToolManager**: 新增 MCP 工具管理器，支持 Agent 级工具配置
- **Agent 级 MCP 工具**: 每个 Agent 可以配置独立的 MCP 工具集
- **MCP 服务器支持**: 支持连接到不同的 MCP 服务器
- **库工具支持**: 支持库类型的工具
- **动态工具注册**: 支持运行时动态注册 MCP 工具

#### 工具配置优化
- **WorkflowSchema.AgentConfig.mcpTools**: 新增 MCP 工具配置字段
- **工具参数验证**: 支持完整的参数定义和验证
- **模拟工具配置**: 通过 `mockTool` 和 `mockInstructions` 配置模拟行为

### 🔄 架构变更

#### 工具管理重构
- **移除 WorkflowSchema.tools**: 不再在全局级别定义工具
- **Agent 级工具配置**: 工具现在直接配置在 Agent 下面
- **McpToolManager 替换 ToolFactory**: 使用更专业的 MCP 工具管理器

#### NodeActionFactory 更新
- **MCP 工具集成**: 初始化 ReactAgent 等时使用 MCP 工具
- **工具自动发现**: 自动从 Agent 配置中获取 MCP 工具
- **智能降级**: 当 MCP 工具不可用时自动使用 MockTool

### 📝 文档更新

#### 新增文档
- **MCP 工具使用指南**: `docs/mcp-tools-guide.md`
- **MCP 工具示例**: `examples/workflow-with-mcp-tools.json`

#### 更新文档
- **README.md**: 更新工具管理部分，介绍 MCP 工具
- **API 文档**: 更新工具相关的 API 接口

### 🛠️ 技术改进

#### 工具管理优化
- **MCP 协议支持**: 完整的 MCP 协议实现
- **工具发现机制**: 自动发现和加载可用的 MCP 工具
- **配置驱动**: 通过 JSON 配置驱动工具行为

#### 错误处理增强
- **工具降级机制**: 优雅的工具降级处理
- **参数验证**: 完整的参数验证和错误提示
- **日志记录**: 详细的工具调用日志

### 🔧 配置变更

#### WorkflowSchema 结构更新
```json
{
  "agents": [
    {
      "agentId": "agent_id",
      "name": "Agent名称",
      "type": "react",
      "mcpTools": [
        {
          "name": "tool_name",
          "description": "工具描述",
          "mcpServerName": "server_name",
          "mcpServerURL": "https://api.server.com",
          "isLibrary": false,
          "mockTool": true,
          "mockInstructions": "模拟指令",
          "parameters": {
            "type": "object",
            "properties": {
              "param1": {
                "type": "string",
                "description": "参数描述"
              }
            },
            "required": ["param1"]
          }
        }
      ]
    }
  ]
}
```

### 🚀 性能优化

#### 工具加载优化
- **延迟加载**: 工具按需加载，减少内存占用
- **缓存机制**: 工具配置缓存，提高访问速度
- **并发安全**: 线程安全的工具管理

### 🐛 问题修复

#### 工具管理问题
- **修复工具注册问题**: 解决工具重复注册的问题
- **修复参数解析问题**: 解决工具参数解析错误
- **修复降级机制问题**: 修复 MockTool 降级逻辑

### 📦 依赖更新

#### 新增依赖
- **MCP 协议支持**: 添加 MCP 协议相关依赖
- **工具验证**: 添加参数验证相关依赖

### 🔄 向后兼容性

#### 兼容性说明
- **MockTool 保持兼容**: MockTool 功能保持不变
- **API 接口兼容**: 主要 API 接口保持兼容
- **配置迁移**: 提供配置迁移指南

### 📋 迁移指南

#### 从旧版本迁移
1. **更新工具配置**: 将全局工具配置迁移到 Agent 级别
2. **添加 MCP 配置**: 为每个工具添加 MCP 相关配置
3. **更新工具调用**: 更新工具调用代码以使用新的 MCP 管理器

#### 配置示例
```json
// 旧配置
{
  "tools": [
    {
      "name": "web_search",
      "description": "搜索工具"
    }
  ]
}

// 新配置
{
  "agents": [
    {
      "agentId": "agent_id",
      "mcpTools": [
        {
          "name": "web_search",
          "description": "搜索工具",
          "mcpServerName": "search_server",
          "mockTool": true
        }
      ]
    }
  ]
}
```

---

## [1.0.0.3-SNAPSHOT] - 2024-01-01

### 🆕 新增功能

#### MockTool 和 ToolFactory
- **MockTool**: 新增模拟工具，当实际工具不可用时提供模拟响应
- **ToolFactory**: 新增工具工厂，统一管理工具实例
- **智能降级**: 当工具不存在时自动使用 MockTool
- **工具注册**: 支持动态注册和管理工具

#### 工具管理优化
- **ToolCallback 实现**: MockTool 完全实现 ToolCallback 接口
- **工具定义**: 完整的工具定义和参数验证
- **模拟指令**: 支持自定义模拟指令

### 🔄 架构变更

#### NodeActionFactory 重构
- **移除旧 Agent 类型**: 移除对 `llm`, `tool`, `custom` 等旧类型的支持
- **新增 Agent 类型**: 支持 `react`, `react_with_human`, `reflect` 三种新类型
- **工具集成**: 集成 MockTool 和 ToolFactory

#### 工作流配置更新
- **Agent 配置**: 更新 Agent 配置结构，支持新类型
- **工具配置**: 添加工具相关配置选项
- **示例更新**: 更新所有示例配置

### 📝 文档更新

#### 新增文档
- **MockTool 使用指南**: `docs/mock-tool-guide.md`
- **新 Agent 类型指南**: `docs/new-agents-guide.md`
- **工具示例**: `examples/workflow-with-mock-tools.json`

#### 更新文档
- **README.md**: 更新节点类型说明，添加 MockTool 功能
- **API 文档**: 更新 Agent 类型和工具相关 API

### 🛠️ 技术改进

#### 工具管理优化
- **MockTool 智能模拟**: 根据工具描述生成真实的模拟响应
- **工具参数解析**: 支持 JSON 格式的工具参数解析
- **错误处理**: 完善的错误处理和日志记录

#### API 兼容性改进
- **向后兼容**: 保持主要 API 接口的兼容性
- **配置迁移**: 提供从旧配置到新配置的迁移路径
- **示例更新**: 更新所有示例以使用新的 Agent 类型

### 🔧 配置变更

#### 支持的 Agent 类型
```json
// 新的 Agent 类型
"type": "react"           // ReactAgent
"type": "react_with_human" // ReactAgentWithHuman  
"type": "reflect"         // ReflectAgent
```

#### MockTool 配置
```json
{
  "tools": [
    {
      "name": "mock_tool",
      "description": "模拟工具",
      "mockTool": true,
      "mockInstructions": "生成模拟响应"
    }
  ]
}
```

### 🚀 性能优化

#### 工具调用优化
- **MockTool 缓存**: 缓存模拟响应，提高性能
- **工具发现**: 优化工具发现和加载机制
- **内存管理**: 改进工具实例的内存管理

### 🐛 问题修复

#### 编译错误修复
- **修复 asNodeAction 错误**: 正确处理 ReactAgent 的 asNodeAction 方法
- **修复类型转换错误**: 修复 NodeActionWithConfig 到 NodeAction 的转换
- **修复工具调用错误**: 修复工具调用时的参数传递问题

### 📦 依赖更新

#### 新增依赖
- **Spring AI Tool**: 添加 Spring AI Tool 相关依赖
- **JSON Schema**: 添加 JSON Schema 验证依赖

### 🔄 向后兼容性

#### 兼容性说明
- **旧 Agent 类型**: 旧类型已被标记为 @Deprecated
- **配置兼容**: 提供配置迁移工具
- **API 兼容**: 主要 API 保持兼容

---

## [1.0.0.2-SNAPSHOT] - 2024-01-01

### 🆕 新增功能

#### 多Agent协作工作流
- **WorkflowSchema**: 新增工作流配置结构，支持多Agent协作
- **AgentConfig**: 支持复杂的Agent配置，包括输入输出映射
- **EdgeConfig**: 支持条件分支和循环的边配置
- **FlowRunner**: 动态构建和运行工作流

#### 复杂流程控制
- **条件分支**: 支持基于状态的条件分支
- **循环控制**: 支持循环流程控制
- **并行执行**: 支持并行Agent执行
- **状态管理**: 完整的工作流状态管理

### 🔄 架构变更

#### 工作流引擎重构
- **StateGraph**: 基于状态图的工作流引擎
- **CompiledGraph**: 编译后的可执行工作流
- **OverAllState**: 全局状态管理
- **NodeAction**: 统一的节点动作接口

#### Agent 类型更新
- **ReactAgent**: 支持复杂推理和工具调用
- **ReactAgentWithHuman**: 支持人机协作
- **ReflectAgent**: 支持反思和改进

### 📝 文档更新

#### 新增文档
- **工作流配置指南**: `docs/workflow-configuration.md`
- **Agent 类型指南**: `docs/agent-types.md`
- **流程控制指南**: `docs/flow-control.md`

#### 示例更新
- **复杂工作流示例**: `examples/complex-workflow.json`
- **条件分支示例**: `examples/conditional-workflow.json`
- **循环流程示例**: `examples/loop-workflow.json`

### 🛠️ 技术改进

#### 工作流引擎优化
- **动态编译**: 支持工作流的动态编译
- **状态管理**: 改进的状态管理和持久化
- **错误处理**: 完善的错误处理和恢复机制

#### API 设计优化
- **RESTful API**: 完整的 RESTful API 设计
- **JSON Schema**: 完整的 JSON Schema 验证
- **版本控制**: 支持工作流版本控制

### 🔧 配置变更

#### 工作流配置结构
```json
{
  "workflowId": "workflow_id",
  "name": "工作流名称",
  "description": "工作流描述",
  "version": "1.0.0",
  "agents": [...],
  "edges": [...],
  "globalConfig": {...},
  "metadata": {...}
}
```

### 🚀 性能优化

#### 工作流执行优化
- **并行执行**: 支持Agent的并行执行
- **状态缓存**: 改进的状态缓存机制
- **内存管理**: 优化的工作流内存管理

### 🐛 问题修复

#### 工作流执行问题
- **修复状态同步问题**: 解决多Agent间的状态同步
- **修复条件评估问题**: 修复条件分支的评估逻辑
- **修复循环控制问题**: 修复循环流程的控制逻辑

---

## [1.0.0.1-SNAPSHOT] - 2024-01-01

### 🆕 新增功能

#### 基础工作流平台
- **NodeActionFactory**: 动态创建 NodeAction 实例
- **WorkflowController**: RESTful API 控制器
- **FlowRunner**: 工作流运行器
- **基础 Agent 支持**: 支持简单的 LLM 和工具节点

#### 核心组件
- **WorkflowSchema**: 工作流配置结构
- **AgentConfig**: Agent 配置结构
- **EdgeConfig**: 边配置结构
- **基础工具支持**: 简单的工具调用支持

### �� 架构设计

#### 模块化设计
- **spring-ai-alibaba-graph-platform**: 主要平台模块
- **spring-ai-alibaba-graph-core**: 核心组件模块
- **spring-ai-alibaba-graph-examples**: 示例模块

#### 基础架构
- **Spring Boot**: 基于 Spring Boot 的微服务架构
- **Spring AI**: 集成 Spring AI 框架
- **Graph Engine**: 基于图的工作流引擎

### 📝 初始文档

#### 基础文档
- **README.md**: 项目介绍和使用指南
- **API 文档**: 基础 API 文档
- **配置指南**: 基础配置指南

#### 示例文档
- **基础示例**: 基础工作流示例
- **配置示例**: 基础配置示例

### 🛠️ 技术实现

#### 核心功能
- **工作流注册**: 支持工作流的动态注册
- **工作流执行**: 支持工作流的动态执行
- **状态管理**: 基础的状态管理功能

#### API 设计
- **RESTful API**: 基础的 RESTful API 设计
- **JSON 配置**: 基于 JSON 的配置系统
- **错误处理**: 基础的错误处理机制

### 🔧 基础配置

#### 系统配置
```yaml
spring:
  ai:
    alibaba:
      graph:
        enabled: true
        max-retries: 3
        timeout: 30000
```

### 🚀 性能基础

#### 基础优化
- **内存管理**: 基础的内存管理
- **并发控制**: 基础的并发控制
- **错误恢复**: 基础的错误恢复机制

### 🐛 基础问题修复

#### 系统稳定性
- **启动问题**: 修复系统启动问题
- **配置问题**: 修复基础配置问题
- **API 问题**: 修复基础 API 问题 