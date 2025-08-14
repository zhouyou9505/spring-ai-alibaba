import React, { useState, useEffect } from 'react';
import { Collapse, Tag, Typography } from 'antd';
import { UserOutlined, RobotOutlined, ToolOutlined, ExclamationCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { Message } from '../contexts/ChatContext';
import { useConfigContext } from '../contexts/ConfigContext';
import styles from '../index.module.less';

const { Panel } = Collapse;
const { Text } = Typography;

// 动态状态指示器组件
const StatusIndicator: React.FC<{ status: string }> = ({ status }) => {
  switch (status) {
    case 'running':
      return <LoadingOutlined style={{ color: '#1890ff', marginRight: 4 }} />;
    case 'completed':
      return <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 4 }} />;
    case 'failed':
      return <CloseCircleOutlined style={{ color: '#ff4d4f', marginRight: 4 }} />;
    default:
      return <LoadingOutlined style={{ color: '#1890ff', marginRight: 4 }} />;
  }
};

interface MessageListProps {
  messages: Message[];
}

const MessageList: React.FC<MessageListProps> = ({ messages }) => {
  const { config } = useConfigContext();

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderAttachments = (attachments?: File[]) => {
    if (!attachments || attachments.length === 0) return null;

    return (
      <div className={styles.fileAttachment}>
        {attachments.map((file, index) => (
          <Tag key={index} icon={<ToolOutlined />}>
            {file.name}
          </Tag>
        ))}
      </div>
    );
  };

  const renderToolCalls = (toolCalls?: any[]) => {
    if (!config.showToolCalls || !toolCalls || toolCalls.length === 0) return null;

    return (
      <div className={styles.messageToolCalls}>
        <Collapse size="small" ghost>
          <Panel header="🔧 工具调用详情" key="1">
            {toolCalls.map((call, index) => (
              <div key={index} style={{ marginBottom: 12, padding: '8px', border: '1px solid #f0f0f0', borderRadius: '4px' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
                  <StatusIndicator status={call.status || 'running'} />
                  <Text strong style={{ fontSize: 13 }}>
                    {call.name}
                  </Text>
                  <Tag 
                    style={{ marginLeft: 'auto', fontSize: '11px' }}
                    color={call.status === 'completed' ? 'success' : call.status === 'failed' ? 'error' : 'processing'}
                  >
                    {call.status === 'running' ? '运行中...' : call.status === 'completed' ? '已完成' : call.status === 'failed' ? '失败' : '未知'}
                  </Tag>
                </div>
                
                {call.arguments && Object.keys(call.arguments).length > 0 && (
                  <div style={{ marginBottom: 4 }}>
                    <Text type="secondary" style={{ fontSize: 11 }}>参数:</Text>
                    <pre style={{ margin: '4px 0', fontSize: 10, backgroundColor: '#f5f5f5', padding: '4px', borderRadius: '2px' }}>
                      {JSON.stringify(call.arguments, null, 2)}
                    </pre>
                  </div>
                )}
                
                {call.result && (
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>结果:</Text>
                    <pre style={{ margin: '4px 0', fontSize: 10, backgroundColor: '#f0f8ff', padding: '4px', borderRadius: '2px' }}>
                      {call.result}
                    </pre>
                  </div>
                )}
              </div>
            ))}
          </Panel>
        </Collapse>
      </div>
    );
  };

  const renderError = (error?: string) => {
    if (!error) return null;

    return (
      <div className={styles.messageError}>
        <ExclamationCircleOutlined style={{ marginRight: 4 }} />
        {error}
      </div>
    );
  };

  return (
    <div>
      {messages.map((message) => (
        <div
          key={message.id}
          className={`${styles.message} ${message.type === 'user' ? styles.user : ''}`}
        >
          <div className={styles.messageAvatar}>
            {message.type === 'user' ? (
              <UserOutlined />
            ) : (
              <RobotOutlined />
            )}
          </div>

          <div className={styles.messageContent}>
            <div className={styles.messageBubble}>
              <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {message.content}
              </div>
              {renderAttachments(message.attachments)}
            </div>

            <div className={styles.messageTime}>
              {formatTime(message.timestamp)}
            </div>

            {renderToolCalls(message.toolCalls)}
            {renderError(message.error)}
          </div>
        </div>
      ))}
    </div>
  );
};

export default MessageList;
