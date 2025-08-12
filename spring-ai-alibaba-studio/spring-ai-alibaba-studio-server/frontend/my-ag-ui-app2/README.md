# AG-UI Chat Demo

一个基于 AG-UI 协议的聊天演示应用，使用 Java 后端和 Next.js 前端。

## 功能特性

- 💬 **AG-UI 聊天**: 支持标准的 AG-UI 协议聊天
- 🎬 **事件演示**: 展示所有 AG-UI 事件类型
- 🔄 **实时流式响应**: 使用 Server-Sent Events (SSE)
- 📊 **事件日志**: 实时显示 AG-UI 协议事件
- 🎨 **现代化 UI**: 使用 Tailwind CSS 构建的响应式界面

## 技术栈

### 前端
- **Next.js 15** - React 框架
- **React 19** - 用户界面库
- **TypeScript** - 类型安全
- **Tailwind CSS** - 样式框架
- **UUID** - 唯一标识符生成

### 后端
- **Java Spring Boot** - 后端框架
- **AG-UI SDK** - AG-UI 协议实现
- **Server-Sent Events** - 实时事件流

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

应用将在 [http://localhost:3000](http://localhost:3000) 启动。

### 3. 构建生产版本

```bash
npm run build
npm start
```

## 使用说明

### AG-UI 聊天

1. 在聊天标签页中输入消息
2. 点击发送或按回车键
3. 观察实时流式响应
4. 查看右侧的事件日志

### 事件演示

1. 切换到演示标签页
2. 选择要演示的事件类型
3. 点击"Start Demo"开始演示
4. 观察各种 AG-UI 事件的实时发射

## AG-UI 协议支持

### 生命周期事件
- `RUN_STARTED` - 代理运行开始
- `RUN_FINISHED` - 代理运行完成
- `RUN_ERROR` - 运行错误
- `STEP_STARTED` - 步骤开始
- `STEP_FINISHED` - 步骤完成

### 文本消息事件
- `TEXT_MESSAGE_START` - 文本消息开始
- `TEXT_MESSAGE_CONTENT` - 文本内容块
- `TEXT_MESSAGE_END` - 文本消息结束

### 思考事件
- `THINKING_START` - AI 开始思考
- `THINKING_END` - AI 完成思考

### 工具调用事件
- `TOOL_CALL_START` - 工具调用开始
- `TOOL_CALL_ARGS` - 工具调用参数
- `TOOL_CALL_END` - 工具调用结束

### 状态管理事件
- `STATE_SNAPSHOT` - 状态快照
- `STATE_DELTA` - 状态变化
- `MESSAGES_SNAPSHOT` - 消息快照

### 特殊事件
- `RAW` - 原始事件数据
- `CUSTOM` - 自定义事件

## API 端点

### 聊天端点
- **POST** `/api/agui/chat` - 简化聊天请求
- **POST** `/api/agui/run` - 标准 AG-UI 代理运行

### 演示端点
- **POST** `/api/agui/demo` - 事件类型演示

### 信息端点
- **GET** `/api/agui/health` - 健康检查
- **GET** `/api/agui/protocol` - 协议信息

## 项目结构

```
src/
├── app/
│   ├── components/
│   │   ├── AguiChat.tsx      # 聊天组件
│   │   ├── AguiDemo.tsx      # 演示组件
│   │   └── Navigation.tsx    # 导航组件
│   ├── globals.css           # 全局样式
│   ├── layout.tsx            # 布局组件
│   └── page.tsx              # 主页面
├── package.json              # 项目配置
└── README.md                 # 项目说明
```

## 配置说明

### 后端配置

确保 Java 后端服务运行在正确的端口上，并配置了 AG-UI 控制器。

### 前端配置

前端默认连接到本地后端。如需修改后端地址，请更新相关 API 调用。

## 开发说明

### 添加新的事件类型

1. 在 `AguiChat.tsx` 的 `handleAguiEvent` 函数中添加新的事件处理逻辑
2. 在 `AguiDemo.tsx` 中添加相应的事件图标和颜色
3. 更新事件类型定义

### 自定义样式

使用 Tailwind CSS 类名来自定义组件样式。所有样式都在组件文件中定义。

### 错误处理

应用包含完整的错误处理机制：
- 网络错误处理
- 事件解析错误处理
- 用户输入验证

## 故障排除

### 常见问题

1. **连接失败**
   - 检查后端服务是否运行
   - 确认端口配置正确

2. **事件不显示**
   - 检查浏览器控制台错误
   - 确认 EventSource 连接正常

3. **样式问题**
   - 确认 Tailwind CSS 正确加载
   - 检查 CSS 类名拼写

### 调试模式

启用浏览器开发者工具查看：
- 网络请求
- 控制台日志
- 事件流数据

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 参考资源

- [AG-UI 协议文档](https://docs.ag-ui.com/)
- [Java AG-UI SDK](https://github.com/ag-ui/ag-ui-java)
- [Next.js 文档](https://nextjs.org/docs)
- [Tailwind CSS 文档](https://tailwindcss.com/docs)