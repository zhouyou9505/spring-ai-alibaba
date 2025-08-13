import React, { Suspense, useState } from 'react';
import { Spin, Card, Button, Space, Typography } from 'antd';
import { PlayCircleOutlined, BugOutlined } from '@ant-design/icons';
import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { ChatProvider } from './contexts/ChatContext';
import { ConfigProvider } from './contexts/ConfigContext';
import { DebugProvider } from './contexts/DebugContext';
import ChatInterface from './components/ChatInterface';
import AguiPlayground from './AguiPlayground';
import styles from './index.module.less';

const DebugPage: React.FC = () => {
  const [currentView, setCurrentView] = useState<'chat' | 'agui'>('chat');

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
        },
        {
          title: $i18n.get({
            id: 'main.pages.Debug.index.title',
            dm: 'Agent Chat UI',
          }),
        },
      ]}
    >
      <div className={styles.debugContainer}>
        {/* 视图选择器 */}
        <Card style={{ marginBottom: '16px' }}>
          <Space>
            <Button
              type={currentView === 'chat' ? 'primary' : 'default'}
              icon={<BugOutlined />}
              onClick={() => setCurrentView('chat')}
            >
              Agent Chat UI
            </Button>
            <Button
              type={currentView === 'agui' ? 'primary' : 'default'}
              icon={<PlayCircleOutlined />}
              onClick={() => setCurrentView('agui')}
            >
              AG-UI Playground
            </Button>
          </Space>
        </Card>

        {/* 内容区域 */}
        {currentView === 'chat' ? (
          <ConfigProvider>
            <DebugProvider>
              <ChatProvider>
                <Suspense
                  fallback={
                    <div className={styles.loadingContainer}>
                      <Spin size="large" tip="加载聊天界面..." />
                    </div>
                  }
                >
                  <ChatInterface />
                </Suspense>
              </ChatProvider>
            </DebugProvider>
          </ConfigProvider>
        ) : (
          <AguiPlayground />
        )}
      </div>
    </InnerLayout>
  );
};

export default DebugPage;
