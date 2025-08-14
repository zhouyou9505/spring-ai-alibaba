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

  // AG-UI 事件处理函数
  const startAguiStream = () => {
    setAguiEvents([]);
    setIsStreaming(true);
    
    if (esRef.current) {
      esRef.current.close();
    }

    const url = `http://localhost:8080/api/agui/stream?question=${encodeURIComponent(customQuestion)}&filter=${filter}&limit=${limit}`;
    console.log('Starting AG-UI stream with URL:', url);
    
    const es = new EventSource(url);
    esRef.current = es;
    setConnected(true);

         // Listen to SSE events directly (AG-UI standard)
     es.onmessage = (ev: MessageEvent) => {
       try {
         console.log('Received AG-UI event:', ev.data);
         const data = JSON.parse(ev.data) as AguiEvent;
         setAguiEvents(prev => [...prev, data]);
       } catch (error) {
         console.error('Failed to parse event data:', error);
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
      const response = await fetch('http://localhost:8080/api/agui/stream?question=test&filter=ALL&limit=1', {
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

      {/* AG-UI 控制面板 */}
      <div style={{ 
        padding: '16px', 
        backgroundColor: '#f8f9fa', 
        borderBottom: '1px solid #e9ecef',
        marginBottom: '16px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '12px' }}>
          <span style={{ fontWeight: 'bold', color: '#495057' }}>AG-UI 事件流:</span>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#6c757d' }}>问题:</span>
            <Input 
              value={customQuestion}
              onChange={(e) => setCustomQuestion(e.target.value)}
              placeholder="输入问题..."
              style={{ width: 200 }}
              disabled={isStreaming}
              size="small"
            />
          </div>
          
          <Button
            type="primary"
            icon={isStreaming ? <ReloadOutlined /> : <PlayCircleOutlined />}
            onClick={isStreaming ? stopAguiStream : startAguiStream}
            loading={isStreaming}
            size="small"
          >
            {isStreaming ? '停止' : '开始'}
          </Button>
          <Button
            icon={<ClearOutlined />}
            onClick={clearAguiEvents}
            disabled={isStreaming}
            size="small"
          >
            清空
          </Button>
          
          <Button
            onClick={testBackendConnection}
            disabled={isStreaming}
            size="small"
            title="测试后端连接"
          >
            测试连接
          </Button>
        </div>
        
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#6c757d' }}>过滤:</span>
            <Select 
              value={filter} 
              onChange={setFilter}
              style={{ width: 100 }}
              disabled={isStreaming}
              size="small"
            >
              <Option value="ALL">全部</Option>
              <Option value="LIFECYCLE">生命周期</Option>
              <Option value="MESSAGE">消息</Option>
              <Option value="TOOL">工具</Option>
              <Option value="STATE">状态</Option>
            </Select>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#6c757d' }}>限制:</span>
            <Input 
              type="number" 
              value={limit} 
              onChange={(e) => setLimit(Number(e.target.value))}
              style={{ width: 60 }}
              disabled={isStreaming}
              size="small"
            />
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#6c757d' }}>原始模式:</span>
            <Switch 
              checked={rawMode} 
              onChange={setRawMode}
              disabled={isStreaming}
              size="small"
            />
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#6c757d' }}>状态:</span>
            <Tag color={connected ? 'success' : 'error'}>
              {connected ? '🟢 已连接' : '🔴 未连接'}
            </Tag>
          </div>
          
          {error && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '12px', color: '#dc3545' }}>错误:</span>
              <span style={{ fontSize: '11px', color: '#dc3545' }}>{error}</span>
            </div>
          )}
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

        {/* AG-UI 事件显示区域 */}
        {aguiEvents.length > 0 && (
          <div style={{ 
            marginTop: '20px', 
            padding: '16px', 
            backgroundColor: '#f8f9fa', 
            borderRadius: '8px',
            border: '1px solid #e9ecef'
          }}>
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center', 
              marginBottom: '12px' 
            }}>
              <h4 style={{ margin: 0, color: '#495057' }}>AG-UI 事件流</h4>
              <span style={{ fontSize: '12px', color: '#6c757d' }}>
                总计: {aguiEvents.length} | 过滤后: {visibleEvents.length}
              </span>
            </div>
            
            <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
              {visibleEvents.map((event, index) => {
                const timestamp = event.timestamp ? new Date(event.timestamp).toLocaleTimeString() : 'N/A';
                
                if (rawMode) {
                  return (
                    <pre key={index} style={{ 
                      backgroundColor: '#fff', 
                      padding: '8px', 
                      borderRadius: '4px', 
                      border: '1px solid #dee2e6',
                      fontSize: '11px',
                      marginBottom: '8px',
                      overflowX: 'auto'
                    }}>
                      {JSON.stringify(event, null, 2)}
                    </pre>
                  );
                }

                // 渲染事件内容
                let eventContent = '';
                let eventColor = '#6c757d';
                
                switch (event.type) {
                  case "RUN_STARTED":
                    eventContent = `🟢 运行开始`;
                    eventColor = '#28a745';
                    break;
                  case "STEP_STARTED":
                    eventContent = `➡️ 步骤开始: ${event.stepName || 'unknown'}`;
                    eventColor = '#007bff';
                    break;
                  case "TEXT_MESSAGE_START":
                    eventContent = `💬 消息开始 (${event.role || 'unknown'})`;
                    eventColor = '#17a2b8';
                    break;
                                     case "TEXT_MESSAGE_CHUNK":
                     eventContent = `📝 内容块: ${event.delta || ''}`;
                     eventColor = '#6f42c1';
                     break;
                  case "TEXT_MESSAGE_END":
                    eventContent = `✅ 消息结束`;
                    eventColor = '#6c757d';
                    break;
                  case "TOOL_CALL_START":
                    eventContent = `🛠️ 工具调用: ${event.toolCallName || 'unknown'}`;
                    eventColor = '#fd7e14';
                    break;
                  case "TOOL_CALL_ARGS":
                    eventContent = `🔧 参数: ${event.delta || ''}`;
                    eventColor = '#fd7e14';
                    break;
                  case "TOOL_CALL_END":
                    eventContent = `✅ 工具调用结束`;
                    eventColor = '#6c757d';
                    break;
                  case "TOOL_CALL_RESULT":
                    eventContent = `📦 结果: ${String(event.content || '')}`;
                    eventColor = '#fd7e14';
                    break;
                  case "STATE_SNAPSHOT":
                    eventContent = `📊 状态快照`;
                    eventColor = '#6f42c1';
                    break;
                  case "STATE_DELTA":
                    eventContent = `📈 状态更新`;
                    eventColor = '#6f42c1';
                    break;
                  case "MESSAGES_SNAPSHOT":
                    eventContent = `💬 消息快照`;
                    eventColor = '#6f42c1';
                    break;
                  case "STEP_FINISHED":
                    eventContent = `✅ 步骤完成: ${event.stepName || 'unknown'}`;
                    eventColor = '#007bff';
                    break;
                  case "RUN_FINISHED":
                    eventContent = `🎉 运行完成`;
                    eventColor = '#28a745';
                    break;
                  default:
                    eventContent = `• ${event.type}`;
                    eventColor = '#6c757d';
                }

                return (
                  <div key={index} style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '8px', 
                    padding: '8px',
                    backgroundColor: '#fff',
                    borderRadius: '4px',
                    border: '1px solid #dee2e6',
                    marginBottom: '8px'
                  }}>
                    <div style={{ 
                      width: '8px', 
                      height: '8px', 
                      borderRadius: '50%', 
                      backgroundColor: eventColor 
                    }} />
                    <span style={{ 
                      fontSize: '12px', 
                      color: eventColor, 
                      fontWeight: '500',
                      minWidth: '200px'
                    }}>
                      {eventContent}
                    </span>
                    <span style={{ 
                      fontSize: '10px', 
                      color: '#6c757d',
                      marginLeft: 'auto'
                    }}>
                      {timestamp}
                    </span>
                  </div>
                );
              })}
            </div>
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
