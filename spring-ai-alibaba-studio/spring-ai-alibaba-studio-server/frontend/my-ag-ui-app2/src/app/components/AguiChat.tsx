'use client';

import React, { useState, useRef, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { getApiUrl, AGUI_CONFIG, LOG_CONFIG } from '../config/agui';

// AG-UI 事件类型定义
interface AguiEvent {
  type: string;
  timestamp: number;
  rawEvent?: Record<string, unknown>;
  messageId?: string;
  delta?: string;
  runId?: string;
  [key: string]: unknown;
}

// 聊天消息类型
interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  isStreaming?: boolean;
}

// AG-UI 运行代理请求类型
interface RunAgentInput {
  threadId: string;
  runId: string;
  state: Record<string, unknown> | null;
  messages: Array<{
    id: string;
    content: string;
    name: string | null;
    role: string;
  }>;
  tools: Record<string, unknown> | null;
  context: Record<string, unknown> | null;
  forwardedProps: Record<string, unknown> | null;
}

const AguiChat: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [currentRunId, setCurrentRunId] = useState<string | null>(null);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [eventLog, setEventLog] = useState<AguiEvent[]>([]);
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected'>('disconnected');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 生成新的会话ID
  const generateNewSession = () => {
    const newThreadId = uuidv4();
    const newRunId = uuidv4();
    setCurrentThreadId(newThreadId);
    setCurrentRunId(newRunId);
    setMessages([]);
    setEventLog([]);
    setConnectionStatus('disconnected');
    return { threadId: newThreadId, runId: newRunId };
  };

  // 处理AG-UI事件
  const handleAguiEvent = (event: AguiEvent) => {
    if (LOG_CONFIG.EVENTS.SHOW_ALL) {
      console.log(`📋 AG-UI Event: ${event.type}`, event);
    }
    
    setEventLog(prev => [...prev, event]);
    
    switch (event.type) {
      case 'RUN_STARTED':
        console.log('🟢 Agent run started:', event.runId);
        setConnectionStatus('connected');
        break;
        
      case 'STEP_STARTED':
        console.log('🔄 Step started:', (event.rawEvent as Record<string, unknown>)?.stepId);
        break;
        
      case 'TEXT_MESSAGE_START':
        console.log('💬 Message started:', event.messageId);
        // 添加新的助手消息
        setMessages(prev => [...prev, {
          id: event.messageId || uuidv4(),
          role: 'assistant',
          content: '',
          timestamp: event.timestamp * 1000,
          isStreaming: true
        }]);
        break;
        
      case 'TEXT_MESSAGE_CONTENT':
        if (LOG_CONFIG.EVENTS.SHOW_ALL) {
          console.log('📝 Content chunk:', event.delta);
        }
        // 更新流式消息内容
        setMessages(prev => prev.map(msg => 
          msg.id === event.messageId 
            ? { ...msg, content: msg.content + (event.delta || '') }
            : msg
        ));
        break;
        
      case 'TEXT_MESSAGE_END':
        console.log('✅ Message ended:', event.messageId);
        // 完成流式消息
        setMessages(prev => prev.map(msg => 
          msg.id === event.messageId 
            ? { ...msg, isStreaming: false }
            : msg
        ));
        break;
        
      case 'STEP_FINISHED':
        console.log('✅ Step finished:', (event.rawEvent as Record<string, unknown>)?.stepId);
        break;
        
      case 'RUN_FINISHED':
        console.log('🎉 Agent run finished');
        setIsLoading(false);
        setConnectionStatus('disconnected');
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
        break;
        
      case 'RUN_ERROR':
        console.error('❌ Run error:', (event.rawEvent as Record<string, unknown>)?.error);
        setIsLoading(false);
        setConnectionStatus('disconnected');
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
        // 添加错误消息
        setMessages(prev => [...prev, {
          id: uuidv4(),
          role: 'assistant',
          content: `❌ Error: ${(event.rawEvent as Record<string, unknown>)?.error || 'Unknown error'}`,
          timestamp: Date.now()
        }]);
        break;
        
      case 'THINKING_START':
        console.log('🤔 AI started thinking');
        break;
        
      case 'THINKING_END':
        console.log('💡 AI finished thinking');
        break;
        
      case 'TOOL_CALL_START':
        console.log('🛠️ Tool call started:', (event.rawEvent as Record<string, unknown>)?.toolName);
        break;
        
      case 'TOOL_CALL_END':
        console.log('✅ Tool call finished:', (event.rawEvent as Record<string, unknown>)?.result);
        break;
        
      default:
        if (LOG_CONFIG.EVENTS.SHOW_ALL) {
          console.log('📋 Unknown event type:', event.type, event);
        }
        break;
    }
  };

  // 发送聊天消息
  const sendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMessage: ChatMessage = {
      id: uuidv4(),
      role: 'user',
      content: inputValue.trim(),
      timestamp: Date.now()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setIsLoading(true);
    setConnectionStatus('connecting');

    // 如果没有会话ID，生成新的
    if (!currentThreadId || !currentRunId) {
      generateNewSession();
    }

    try {
      // 使用标准AG-UI端点
      const runAgentInput: RunAgentInput = {
        threadId: currentThreadId!,
        runId: currentRunId!,
        state: null,
        messages: [{
          id: userMessage.id,
          content: userMessage.content,
          name: null,
          role: 'user'
        }],
        tools: null,
        context: null,
        forwardedProps: null
      };

      // 创建EventSource连接
      const eventSource = new EventSource(getApiUrl(AGUI_CONFIG.ENDPOINTS.RUN));
      eventSourceRef.current = eventSource;

      eventSource.addEventListener('event', (e) => {
        try {
          const event = JSON.parse(e.data);
          handleAguiEvent(event);
        } catch (error) {
          console.error('Failed to parse event:', error);
        }
      });

      eventSource.onerror = (error) => {
        console.error('EventSource error:', error);
        setIsLoading(false);
        setConnectionStatus('disconnected');
        eventSource.close();
      };

      // 发送POST请求启动代理
      const response = await fetch(getApiUrl(AGUI_CONFIG.ENDPOINTS.RUN), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(runAgentInput)
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

    } catch (error) {
      console.error('Failed to send message:', error);
      setIsLoading(false);
      setConnectionStatus('disconnected');
      setMessages(prev => [...prev, {
        id: uuidv4(),
        role: 'assistant',
        content: '❌ Failed to send message. Please try again.',
        timestamp: Date.now()
      }]);
    }
  };

  // 处理回车键
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 清除会话
  const clearSession = () => {
    generateNewSession();
  };

  // 获取连接状态图标
  const getConnectionIcon = () => {
    switch (connectionStatus) {
      case 'connected':
        return '🟢';
      case 'connecting':
        return '🟡';
      case 'disconnected':
        return '🔴';
      default:
        return '⚪';
    }
  };

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* 头部 */}
      <div className="bg-white border-b border-gray-200 p-4">
        <div className="flex justify-between items-center">
          <h1 className="text-xl font-bold text-gray-900">AG-UI Chat Demo</h1>
          <div className="flex gap-2 items-center">
            <div className="flex items-center gap-2 text-sm">
              <span>Status: {getConnectionIcon()} {connectionStatus}</span>
              <span className="text-gray-400">|</span>
              <span>Backend: {AGUI_CONFIG.API_BASE_URL}</span>
            </div>
            <button
              onClick={clearSession}
              className="px-3 py-1 text-sm bg-gray-500 text-white rounded hover:bg-gray-600"
            >
              New Session
            </button>
            <div className="text-xs text-gray-500">
              {currentThreadId && (
                <div>Thread: {currentThreadId.slice(0, 8)}...</div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* 聊天区域 */}
        <div className="flex-1 flex flex-col">
          {/* 消息列表 */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.length === 0 && (
              <div className="text-center text-gray-500 mt-8">
                <p>Start a new conversation!</p>
                <p className="text-sm mt-2">This demo uses the AG-UI protocol with Java backend</p>
                <p className="text-xs mt-1 text-gray-400">Backend: {AGUI_CONFIG.API_BASE_URL}</p>
              </div>
            )}
            
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                    message.role === 'user'
                      ? 'bg-blue-500 text-white'
                      : 'bg-white text-gray-900 border border-gray-200'
                  }`}
                >
                  <div className="text-sm">
                    {message.content}
                    {message.isStreaming && (
                      <span className="inline-block w-2 h-4 bg-gray-400 ml-1 animate-pulse" />
                    )}
                  </div>
                  <div className={`text-xs mt-1 ${
                    message.role === 'user' ? 'text-blue-100' : 'text-gray-500'
                  }`}>
                    {new Date(message.timestamp).toLocaleTimeString()}
                  </div>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>

          {/* 输入区域 */}
          <div className="border-t border-gray-200 p-4 bg-white">
            <div className="flex gap-2">
              <input
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type your message..."
                disabled={isLoading || connectionStatus !== 'connected'}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              />
              <button
                onClick={sendMessage}
                disabled={isLoading || !inputValue.trim() || connectionStatus !== 'connected'}
                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Sending...' : 'Send'}
              </button>
            </div>
          </div>
        </div>

        {/* 事件日志区域 */}
        <div className="w-80 bg-white border-l border-gray-200 overflow-y-auto">
          <div className="p-4 border-b border-gray-200">
            <h3 className="font-semibold text-gray-900">Event Log</h3>
            <p className="text-xs text-gray-500">AG-UI Protocol Events</p>
            <p className="text-xs text-gray-400 mt-1">Total: {eventLog.length}</p>
          </div>
          <div className="p-2 space-y-1">
            {eventLog.map((event, index) => (
              <div
                key={index}
                className="text-xs p-2 bg-gray-50 rounded border-l-2 border-blue-500"
              >
                <div className="font-mono text-blue-600">{event.type}</div>
                <div className="text-gray-600 mt-1">
                  {new Date(event.timestamp * 1000).toLocaleTimeString()}
                </div>
                {event.rawEvent && LOG_CONFIG.EVENTS.SHOW_RAW_DATA && (
                  <div className="text-gray-500 mt-1">
                    {JSON.stringify(event.rawEvent, null, 2)}
                  </div>
                )}
              </div>
            ))}
            {eventLog.length === 0 && (
              <div className="text-center text-gray-500 text-sm p-4">
                No events yet
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AguiChat; 