# CopilotController API 使用示例

## 概述

CopilotController 是一个智能工作流调整助手，支持通过自然语言描述来创建、编辑和改进多Agent工作流。它使用LLM来理解用户需求并生成相应的WorkflowSchema配置。

## 核心功能

### 智能工作流调整

```http
POST /api/copilot/adjust
Content-Type: application/json
```

**功能说明：**
通过自然语言描述来智能调整工作流，支持以下操作：
1. 创建多Agent系统
2. 创建新Agent
3. 编辑现有Agent
4. 改进Agent指令
5. 添加/编辑/删除工具
6. 添加/编辑/删除提示词
7. 优化工作流性能
8. 添加条件逻辑和循环
9. 配置并行执行
10. 其他工作流调整需求

**请求参数：**
- `userRequest` (必需): 自然语言描述的用户需求
- `workflowId` (可选): 现有工作流ID，如果不提供则创建新工作流

**响应格式：**
```json
{
  "success": true/false,
  "message": "操作结果消息",
  "workflowId": "工作流ID",
  "workflowName": "工作流名称",
  "agentCount": 4,
  "edgeCount": 5,
  "schema": {
    // 完整的WorkflowSchema配置
  }
}
```

## 使用示例

### 1. 创建新的多Agent系统

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "userRequest": "创建一个智能客服系统，包含接待Agent、问题分类Agent、技术支持Agent和满意度调查Agent"
  }'
```

**响应示例：**
```json
{
  "success": true,
  "message": "工作流调整成功",
  "workflowId": "smart-customer-service-system",
  "workflowName": "智能客服系统",
  "agentCount": 4,
  "edgeCount": 5,
  "schema": {
    "workflowId": "smart-customer-service-system",
    "name": "智能客服系统",
    "description": "智能客服多Agent协作系统",
    "agents": [...],
    "edges": [...]
  }
}
```

### 2. 调整现有工作流

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "existing-workflow-id",
    "userRequest": "在现有工作流中添加一个数据分析Agent，用于分析客户交互数据并生成报告"
  }'
```

### 3. 复杂工作流调整

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "customer-service-workflow",
    "userRequest": "在现有的客服工作流中添加一个情感分析Agent，用于分析客户情绪并提供相应的服务策略。同时改进接待Agent，让它能够根据客户情绪调整回复语气。另外添加一个自动升级机制，当客户情绪为负面时自动转接人工客服。"
  }'
```

### 4. 性能优化

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "tech-support-workflow",
    "userRequest": "优化技术支持工作流，添加并行处理能力，让问题分类Agent和知识库Agent可以同时工作，提高响应速度。同时添加重试机制和超时处理。"
  }'
```

### 5. 添加工具和提示词

```bash
curl -X POST http://localhost:8080/api/copilot/adjust \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "existing-workflow",
    "userRequest": "添加一个知识库搜索工具，用于快速检索技术文档和解决方案。同时添加一个友好的问候提示词，用于接待客户时使用。"
  }'
```

## 自然语言请求示例

### 创建复杂工作流

```json
{
  "userRequest": "创建一个电商订单处理系统，包含以下Agent：1. 订单接收Agent - 接收和验证订单 2. 库存检查Agent - 检查商品库存 3. 支付处理Agent - 处理支付 4. 物流安排Agent - 安排发货 5. 通知Agent - 发送状态通知。要求支持条件分支和循环处理。"
}
```

### 修改现有工作流

```json
{
  "userRequest": "在现有的客服工作流中添加一个情感分析Agent，用于分析客户情绪并提供相应的服务策略。同时改进接待Agent，让它能够根据客户情绪调整回复语气。",
  "workflowId": "existing-customer-service-workflow"
}
```

### 优化工作流性能

```json
{
  "userRequest": "优化技术支持工作流，添加并行处理能力，让问题分类Agent和知识库Agent可以同时工作，提高响应速度。同时添加重试机制和超时处理。",
  "workflowId": "tech-support-workflow"
}
```

### 添加条件逻辑

```json
{
  "userRequest": "在订单处理工作流中添加条件判断，当订单金额大于1000元时，需要额外的审核流程。同时添加循环机制，允许客户修改订单信息。",
  "workflowId": "order-processing-workflow"
}
```

## 错误处理

### 常见错误响应

**参数缺失：**
```json
{
  "success": false,
  "message": "用户请求不能为空"
}
```

**工作流不存在：**
```json
{
  "success": false,
  "message": "工作流不存在: workflow-123"
}
```

**LLM生成失败：**
```json
{
  "success": false,
  "message": "工作流调整失败: 无法解析LLM生成的工作流配置",
  "error": "RuntimeException"
}
```

## 最佳实践

1. **清晰的描述**：提供详细、清晰的自然语言描述，包含具体的功能需求和约束条件。

2. **渐进式调整**：对于复杂的工作流，建议分步骤进行调整，而不是一次性描述所有需求。

3. **验证结果**：在应用生成的配置前，检查生成的WorkflowSchema是否符合预期。

4. **错误处理**：实现适当的错误处理机制，处理LLM生成失败或配置无效的情况。

5. **版本控制**：在修改现有工作流时，考虑保留历史版本或创建备份。

## 集成示例

### 前端集成

```javascript
// 调整工作流
async function adjustWorkflow(workflowId, userRequest) {
  const response = await fetch('/api/copilot/adjust', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      workflowId: workflowId,
      userRequest: userRequest
    })
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('工作流调整成功');
    return result.schema;
  } else {
    throw new Error(result.message);
  }
}

// 创建新工作流
async function createWorkflow(userRequest) {
  const response = await fetch('/api/copilot/adjust', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      userRequest: userRequest
    })
  });
  
  const result = await response.json();
  if (result.success) {
    console.log('工作流创建成功:', result.workflowId);
    return result.schema;
  } else {
    throw new Error(result.message);
  }
}
```

### 后端集成

```java
@Autowired
private CopilotController copilotController;

public void adjustCustomerServiceWorkflow() {
    Map<String, Object> request = new HashMap<>();
    request.put("workflowId", "customer-service-workflow");
    request.put("userRequest", "添加情感分析Agent和改进接待Agent的指令");
    
    Map<String, Object> result = copilotController.adjustWorkflow(request);
    
    if ((Boolean) result.get("success")) {
        WorkflowSchema schema = (WorkflowSchema) result.get("schema");
        // 处理生成的配置
        workflowService.updateWorkflow((String) result.get("workflowId"), schema);
    }
}
```

## 支持的Agent类型

- **llm**: 语言模型Agent
- **tool**: 工具调用Agent
- **custom**: 自定义逻辑Agent
- **condition**: 条件决策Agent
- **simple**: 简单处理Agent
- **input**: 输入处理Agent
- **output**: 输出处理Agent

## 支持的边类型

- **SEQUENTIAL**: 顺序执行
- **CONDITIONAL**: 条件分支
- **LOOP**: 循环执行
- **PARALLEL**: 并行执行 