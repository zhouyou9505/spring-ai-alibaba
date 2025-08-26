import React from 'react';
import { Card, Typography, Divider, Alert } from 'antd';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { CopilotKit } from '@copilotkit/react-core';
import { CopilotChat } from '@copilotkit/react-ui';
import '@copilotkit/react-ui/styles.css';
import styles from './index.module.less';

const { Title, Text, Paragraph } = Typography;

const CopilotKitDebugPage: React.FC = () => {
  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.Debug.index.debug',
            dm: '调试工具',
          }),
        },
        {
          title: 'CopilotKit 调试',
        },
      ]}
    >
      <div className={styles.copilotKitContainer}>
        <Title level={2}>CopilotKit 调试页面</Title>
        <Text type="secondary">
          使用 CopilotKit 的 CopilotChat 组件来测试与后端 AguiStreamController 的集成
        </Text>

        <Divider />

        {/* 说明信息 */}
        <Card title="使用说明" className={styles.infoCard}>
          <Paragraph>
            这个页面使用 CopilotKit 的 <Text code>CopilotChat</Text> 组件来提供聊天界面。
            当用户发送消息时，组件会调用后端的 <Text code>/api/v1/agui/copilotkit</Text> 接口。
          </Paragraph>
          
          <Alert
            message="重要提示"
            description="确保后端 AguiStreamController 服务正在运行，并且接口路径正确配置。"
            type="info"
            showIcon
            style={{ marginTop: 16 }}
          />
        </Card>

        <Divider />

        {/* CopilotKit 提供者和聊天组件 */}
        <Card title="AI 助手聊天" className={styles.chatCard}>
          <CopilotKit 
            runtimeUrl="/api/v1/agui/copilotkit"
            showDevConsole={true}
          >
            <CopilotChat
              instructions="你是一个智能助手，可以帮助用户解答问题。请用中文回答，并保持友好和专业的语气。"
              labels={{
                title: "CopilotKit 调试助手",
                initial: "你好！👋 我是 CopilotKit 调试助手，有什么可以帮助你的吗？",
                placeholder: "输入你的问题...",
                stopGenerating: "停止",
                regenerateResponse: "重新生成",
              }}
              className={styles.copilotChat}
            />
          </CopilotKit>
        </Card>

        <Divider />

        {/* 技术细节 */}
        <Card title="技术实现细节" className={styles.techCard}>
          <Paragraph>
            <Text strong>前端组件：</Text> 使用 <Text code>@copilotkit/react-ui</Text> 包中的 <Text code>CopilotChat</Text> 组件
          </Paragraph>
          
          <Paragraph>
            <Text strong>后端接口：</Text> 调用 <Text code>/api/v1/agui/copilotkit</Text> 端点，该端点由 <Text code>AguiStreamController</Text> 处理
          </Paragraph>
          
          <Paragraph>
            <Text strong>通信方式：</Text> 使用 Server-Sent Events (SSE) 实现实时流式响应
          </Paragraph>
          
          <Paragraph>
            <Text strong>事件处理：</Text> 通过 <Text code>CallbackManager</Text> 和 <Text code>EventHandler</Text> 处理各种事件类型
          </Paragraph>
          
          <Paragraph>
            <Text strong>CopilotKit 配置：</Text> 使用 <Text code>runtimeUrl="/api/v1/agui/copilotkit"</Text> 指向后端接口
          </Paragraph>
        </Card>
      </div>
    </InnerLayout>
  );
};

export default CopilotKitDebugPage;
