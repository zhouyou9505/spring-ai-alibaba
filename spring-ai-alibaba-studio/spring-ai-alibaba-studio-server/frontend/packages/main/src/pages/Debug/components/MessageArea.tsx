import React, { useEffect, useRef, useState, useMemo } from 'react';
import { Button, Select, Input, Switch, Tag, Space } from 'antd';
import { SettingOutlined, ClearOutlined, PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { useChatContext } from '../contexts/ChatContext';
import { useConfigContext } from '../contexts/ConfigContext';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import styles from '../index.module.less';

const { Option } = Select;

// AG-UI 事件类型定义
interface AguiEvent {
  type: string;
  timestamp?: number;
  threadId?: string;
  runId?: string;
  stepName?: string;
  messageId?: string;
  role?: string;
  delta?: string;
  toolCallId?: string;
  toolCallName?: string;
  content?: any;
  [key: string]: any;
}

const MessageArea: React.FC = () => {
  const { currentSession } = useChatContext();
  const { config, toggleDebugInfo } = useConfigContext();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // AG-UI 事件状态
  const [aguiEvents, setAguiEvents] = useState<AguiEvent[]>([]);
  const [rawMode, setRawMode] = useState(false);
  const [filter, setFilter] = useState<"ALL" | "MESSAGE" | "TOOL" | "STATE" | "LIFECYCLE">("ALL");
  const [limit, setLimit] = useState(120);
  const [connected, setConnected] = useState(false);
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [customQuestion, setCustomQuestion] = useState("AG-UI Demo");
  const esRef = useRef<EventSource | null>(null);

  const scrollToBottom = () => {
    if (config.autoScroll && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [currentSession?.messages, config.autoScroll]);

  const handleClearChat = () => {
    if (currentSession && window.confirm('确定要清空当前对话吗？')) {
      // Clear messages in current session
      // This would need to be implemented in the context
    }
  };

  // AG-UI 事件处理函数 - 仅用于调试显示，不影响聊天
  const startAguiStream = () => {
    setAguiEvents([]);
    setIsStreaming(true);

    if (esRef.current) {
      esRef.current.close();
    }

    // 使用独立的AG-UI调试流，不影响聊天消息
    const url = `http://localhost:8080/api/agui/stream?question=${encodeURIComponent(customQuestion)}&filter=${filter}&limit=${limit}&mode=debug`;
    console.log('Starting AG-UI debug stream with URL:', url);

    const es = new EventSource(url);
    esRef.current = es;
    setConnected(true);

    // Listen to SSE events directly (AG-UI standard) - 仅用于调试显示
    es.onmessage = (ev: MessageEvent) => {
      try {
        console.log('Received AG-UI debug event:', ev.data);
        const data = JSON.parse(ev.data) as AguiEvent;
        setAguiEvents(prev => [...prev, data]);
      } catch (error) {
        console.error('Failed to parse AG-UI debug event data:', error);
      }
    };

    es.onopen = () => {
      console.log('EventSource connection opened');
      setConnected(true);
    };

    es.onerror = (error) => {
      console.error('EventSource error:', error);
      setError('连接错误，请检查后端服务是否运行');
      setConnected(false);
      setIsStreaming(false);
      es.close();
    };

    es.addEventListener('error', (error) => {
      console.error('EventSource error event:', error);
      setError('连接错误，请检查后端服务是否运行');
      setConnected(false);
      setIsStreaming(false);
      es.close();
    });
  };

  const stopAguiStream = () => {
    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }
    setConnected(false);
    setIsStreaming(false);
  };

  const clearAguiEvents = () => {
    setAguiEvents([]);
    setError(null);
    stopAguiStream();
  };

  // 测试后端连接
  const testBackendConnection = async () => {
    try {
      setError(null);
      const response = await fetch('http://localhost:8080/api/agui/stream?question=test&filter=ALL&limit=1&mode=debug', {
        method: 'GET',
        headers: {
          'Accept': 'text/event-stream',
        },
      });

      if (response.ok) {
        console.log('Backend connection test successful');
        setError(null);
      } else {
        console.error('Backend connection test failed:', response.status);
        setError(`后端连接测试失败: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Backend connection test error:', error);
      setError('后端连接测试失败: 网络错误');
    }
  };

  // 过滤可见事件
  const visibleEvents = useMemo(() => {
    if (filter === "ALL") return aguiEvents;
    return aguiEvents.filter(e => {
      if (filter === "MESSAGE") return e.type.startsWith("TEXT_MESSAGE");
      if (filter === "TOOL") return e.type.startsWith("TOOL_CALL");
      if (filter === "STATE") return e.type.startsWith("STATE") || e.type === "MESSAGES_SNAPSHOT";
      if (filter === "LIFECYCLE") return e.type === "RUN_STARTED" || e.type === "RUN_FINISHED" || e.type === "RUN_ERROR" || e.type.startsWith("STEP_");
      return true;
    });
  }, [aguiEvents, filter]);

  // 清理 EventSource
  useEffect(() => {
    return () => {
      if (esRef.current) {
        esRef.current.close();
      }
    };
  }, []);

  return (
    <>
      <div className={styles.chatHeader}>
        <h3 className={styles.chatTitle}>
          {currentSession ? currentSession.title : '选择或创建一个对话'}
        </h3>
        <div className={styles.headerActions}>
          <Button
            type="text"
            icon={<SettingOutlined />}
            onClick={toggleDebugInfo}
            size="small"
            title="调试面板"
          />
          <Button
            type="text"
            icon={<ClearOutlined />}
            onClick={handleClearChat}
            size="small"
            title="清空对话"
            disabled={!currentSession || currentSession.messages.length === 0}
          />
        </div>
      </div>

      <div className={styles.messageContainer}>
        {currentSession ? (
          <>
            <MessageList messages={currentSession.messages} />
            <div ref={messagesEndRef} />
          </>
        ) : (
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            height: '100%',
            flexDirection: 'column',
            color: '#999'
          }}>
            <div style={{ fontSize: 16, marginBottom: 8 }}>🤖</div>
            <div>欢迎使用 Agent Chat UI</div>
            <div style={{ fontSize: 12, marginTop: 4 }}>请创建或选择一个对话开始聊天</div>
          </div>
        )}
      </div>

      {currentSession && (
        <div className={styles.inputArea}>
          <MessageInput />
        </div>
      )}
    </>
  );
};

export default MessageArea;
