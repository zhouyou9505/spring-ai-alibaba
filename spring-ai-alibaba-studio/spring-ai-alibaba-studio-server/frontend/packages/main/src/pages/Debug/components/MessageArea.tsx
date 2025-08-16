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
  const { currentThread } = useChatContext();
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
  }, [currentThread?.messages, config.autoScroll]);

  const handleClearChat = () => {
    if (currentThread && window.confirm('确定要清空当前对话吗？')) {
      // Clear messages in current thread
      // This would need to be implemented in the context
    }
  };

  // AG-UI 事件处理函数 - 仅用于调试显示，不影响聊天
  const startAguiStream = () => {
    if (isStreaming) return;
    
    setConnected(false);
    setError(null);
    setAguiEvents([]);
    setIsStreaming(true);
    
    // Create RunAgentInput structure for debug stream
    const runAgentInput = {
      threadId: `debug_thread_${Date.now()}`,
      runId: `debug_run_${Date.now()}`,
      state: null,
      messages: [
        {
          id: `debug_msg_${Date.now()}`,
          role: "user",
          content: customQuestion || "测试问题",
          name: null
        }
      ],
      tools: [
        {
          name: "debug_tool",
          description: "Debug tool for testing",
          parameters: {
            type: "object",
            properties: {
              query: { type: "string" },
              filter: { type: "string" },
              limit: { type: "number" }
            }
          }
        }
      ],
      context: [
        {
          description: "debug_session",
          value: "debug"
        }
      ],
      forwardedProps: null
    };
    
    console.log('Starting AG-UI debug stream with RunAgentInput:', runAgentInput);
    
    // Use fetch with ReadableStream for POST request (AG-UI standard)
    const startStream = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/agui/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
          },
          body: JSON.stringify(runAgentInput)
        });
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        if (!response.body) {
          throw new Error('Response body is null');
        }
        
        setConnected(true);
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        
        try {
          while (true) {
            const { done, value } = await reader.read();
            
            if (done) {
              console.log('Debug stream completed');
              break;
            }
            
            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');
            
                         for (const line of lines) {
               if (line.startsWith('data: ')) {
                 try {
                   const data = JSON.parse(line.slice(6)) as AguiEvent;
                   console.log('Received AG-UI debug event:', data);
                   
                   // AG-UI Event classes have direct fields
                   setAguiEvents(prev => [...prev, data]);
                 } catch (error) {
                   console.error('Failed to parse AG-UI debug event data:', error);
                 }
               }
             }
          }
        } finally {
          reader.releaseLock();
          setConnected(false);
          setIsStreaming(false);
        }
        
      } catch (error) {
        console.error('AG-UI debug stream error:', error);
        setError('连接错误');
        setConnected(false);
        setIsStreaming(false);
      }
    };
    
    startStream();
  };

  const stopAguiStream = () => {
    // For POST request with ReadableStream, we can't easily stop the stream
    // The stream will complete naturally when the backend finishes
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
      
      // Test with POST request and RunAgentInput structure (AG-UI standard)
      const testInput = {
        threadId: "test_thread",
        runId: "test_run",
        state: null,
        messages: [
          {
            id: "test_msg",
            role: "user",
            content: "test",
            name: null
          }
        ],
        tools: [],
        context: [],
        forwardedProps: null
      };
      
      const response = await fetch('http://localhost:8080/api/agui/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(testInput)
      });
      
      if (response.ok) {
        console.log('Backend connection test successful');
        setError('✅ 后端连接正常');
        setTimeout(() => setError(null), 2000);
      } else {
        console.error('Backend connection test failed:', response.status);
        setError(`❌ 后端连接测试失败: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Backend connection test error:', error);
      setError('❌ 后端连接测试失败: 网络错误');
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

  // No cleanup needed for ReadableStream

  return (
    <>
      <div className={styles.chatHeader}>
        <h3 className={styles.chatTitle}>
          {currentThread ? currentThread.title : '选择或创建一个对话'}
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
            disabled={!currentThread || currentThread.messages.length === 0}
          />
        </div>
      </div>

      <div className={styles.messageContainer}>
        {currentThread ? (
          <>
            <MessageList messages={currentThread.messages} />
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

      {currentThread && (
        <div className={styles.inputArea}>
          <MessageInput />
        </div>
      )}
    </>
  );
};

export default MessageArea;
