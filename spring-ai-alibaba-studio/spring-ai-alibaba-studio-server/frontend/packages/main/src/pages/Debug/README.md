# Debug 页面 - CopilotKit 集成

这个目录包含了从 Next.js 项目复制的 CopilotKit 组件，已经适配到 UmiJS 框架。

## 文件说明

- `langgraph4j.ts` - LangGraph4j 后端适配器
- `chat.tsx` - 简单聊天组件
- `chatApproval.tsx` - 带审批功能的聊天组件
- `CopilotKitProvider.tsx` - CopilotKit 运行时提供者
- `index.tsx` - 主页面入口

## 使用方法

1. 确保后端 LangGraph4j 服务运行在 `http://localhost:8080`
2. 访问 `/debug` 路径即可看到聊天界面
3. 聊天组件会自动连接到后端服务

## 依赖

- `@copilotkit/react-core`
- `@copilotkit/react-ui` 
- `@copilotkit/runtime`

## 注意事项

- 这些组件使用 CopilotKit 的标准协议
- 支持流式响应和工具调用
- 需要确保后端服务正常运行
