import React from 'react';
import { Button, Select, Slider, Switch, Tooltip } from 'antd';
import { PlusOutlined, SettingOutlined, DeleteOutlined } from '@ant-design/icons';
import { useChatContext } from '../contexts/ChatContext';
import { useConfigContext } from '../contexts/ConfigContext';
import styles from '../index.module.less';

const { Option } = Select;

const Sidebar: React.FC = () => {
  const { state, createNewThread, deleteThread, switchThread } = useChatContext();
  const { config, updateModelConfig, toggleToolCalls, toggleDebugInfo } = useConfigContext();

  const handleThreadClick = async (threadId: string) => {
    await switchThread(threadId);
  };

  const formatTime = (date: Date) => {
    return date.toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className={styles.sidebar}>
      <div className={styles.sidebarHeader}>
        <div className={styles.sidebarTitle}>Agent Chat UI</div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={createNewThread}
          size="small"
        >
          新对话
        </Button>
      </div>

      <div className={styles.configSection}>
        <div className={styles.configTitle}>模型配置</div>

        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: '#666' }}>模型:</label>
          <Select
            value={config.modelConfig.model}
            onChange={(value) => updateModelConfig({ model: value })}
            size="small"
            style={{ width: '100%', marginTop: 4 }}
          >
            <Option value="qwen-plus">Qwen Plus</Option>
            <Option value="qwen-turbo">Qwen Turbo</Option>
            <Option value="qwen-max">Qwen Max</Option>
          </Select>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: '#666' }}>
            Temperature: {config.modelConfig.temperature}
          </label>
          <Slider
            min={0}
            max={2}
            step={0.1}
            value={config.modelConfig.temperature}
            onChange={(value) => updateModelConfig({ temperature: value })}
            size="small"
          />
        </div>

        <div style={{ marginBottom: 12 }}>
          <label style={{ fontSize: 12, color: '#666' }}>
            Max Tokens: {config.modelConfig.maxTokens}
          </label>
          <Slider
            min={512}
            max={4096}
            step={256}
            value={config.modelConfig.maxTokens}
            onChange={(value) => updateModelConfig({ maxTokens: value })}
            size="small"
          />
        </div>

        <div style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 12, color: '#666' }}>显示工具调用</span>
            <Switch
              size="small"
              checked={config.showToolCalls}
              onChange={toggleToolCalls}
            />
          </div>
        </div>

        <div style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 12, color: '#666' }}>调试信息</span>
            <Switch
              size="small"
              checked={config.showDebugInfo}
              onChange={toggleDebugInfo}
            />
          </div>
        </div>
      </div>

      <div className={styles.threadList}>
        {state.threads.map((thread) => (
          <div
            key={thread.id}
            className={`${styles.threadItem} ${
              state.currentThreadId === thread.id ? styles.active : ''
            }`}
            onClick={() => handleThreadClick(thread.id)}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className={styles.threadTitle}>{thread.title}</div>
                <div className={styles.threadTime}>
                  {formatTime(thread.updatedAt)} · {thread.messages.length} 条消息
                </div>
              </div>
              <Tooltip title="删除对话">
                <Button
                  type="text"
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteThread(thread.id);
                  }}
                  style={{ opacity: 0.6 }}
                />
              </Tooltip>
            </div>
          </div>
        ))}

        {state.threads.length === 0 && (
          <div style={{ padding: 16, textAlign: 'center', color: '#999', fontSize: 14 }}>
            暂无对话记录
            <br />
            点击"新对话"开始
          </div>
        )}
      </div>
    </div>
  );
};

export default Sidebar;
