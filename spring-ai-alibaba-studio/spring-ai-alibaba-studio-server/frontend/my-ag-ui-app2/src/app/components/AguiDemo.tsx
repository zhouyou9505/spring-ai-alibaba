'use client';

import React, { useState, useRef } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { getApiUrl, AGUI_CONFIG, LOG_CONFIG } from '../config/agui';

// AG-UI 事件类型定义
interface AguiEvent {
  type: string;
  timestamp: number;
  rawEvent?: Record<string, unknown>;
  threadId?: string;
  runId?: string;
  [key: string]: unknown;
}

// 演示类型
type DemoType = 'full' | 'lifecycle' | 'text' | 'tool' | 'state' | 'thinking' | 'custom';

const AguiDemo: React.FC = () => {
  const [isRunning, setIsRunning] = useState(false);
  const [eventLog, setEventLog] = useState<AguiEvent[]>([]);
  const [selectedDemo, setSelectedDemo] = useState<DemoType>('full');
  const [currentRunId, setCurrentRunId] = useState<string | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected'>('disconnected');
  const eventSourceRef = useRef<EventSource | null>(null);

  // 开始演示
  const startDemo = async () => {
    if (isRunning) return;

    const runId = uuidv4();
    const threadId = uuidv4();
    setCurrentRunId(runId);
    setIsRunning(true);
    setConnectionStatus('connecting');
    setEventLog([]);

    try {
      // 创建EventSource连接
      const eventSource = new EventSource(getApiUrl(AGUI_CONFIG.ENDPOINTS.DEMO));
      eventSourceRef.current = eventSource;

      eventSource.addEventListener('event', (e) => {
        try {
          const event = JSON.parse(e.data);
          if (LOG_CONFIG.EVENTS.SHOW_ALL) {
            console.log(`📋 Demo Event: ${event.type}`, event);
          }
          setEventLog(prev => [...prev, event]);
          
          // 处理特定事件类型
          switch (event.type) {
            case 'RUN_STARTED':
              setConnectionStatus('connected');
              break;
            case 'RUN_FINISHED':
            case 'RUN_ERROR':
              setIsRunning(false);
              setConnectionStatus('disconnected');
              if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
              }
              break;
          }
        } catch (error) {
          console.error('Failed to parse event:', error);
        }
      });

      eventSource.onerror = (error) => {
        console.error('EventSource error:', error);
        setIsRunning(false);
        setConnectionStatus('disconnected');
        eventSource.close();
      };

      // 发送演示请求
      const response = await fetch(getApiUrl(AGUI_CONFIG.ENDPOINTS.DEMO), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          runId,
          threadId,
          demoType: selectedDemo
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

    } catch (error) {
      console.error('Failed to start demo:', error);
      setIsRunning(false);
      setConnectionStatus('disconnected');
    }
  };

  // 停止演示
  const stopDemo = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    setIsRunning(false);
    setConnectionStatus('disconnected');
  };

  // 清除事件日志
  const clearLog = () => {
    setEventLog([]);
  };

  // 获取事件类型图标
  const getEventIcon = (eventType: string) => {
    switch (eventType) {
      case 'RUN_STARTED':
        return '🟢';
      case 'RUN_FINISHED':
        return '🎉';
      case 'RUN_ERROR':
        return '❌';
      case 'STEP_STARTED':
        return '🔄';
      case 'STEP_FINISHED':
        return '✅';
      case 'TEXT_MESSAGE_START':
        return '💬';
      case 'TEXT_MESSAGE_CONTENT':
        return '📝';
      case 'TEXT_MESSAGE_END':
        return '✅';
      case 'THINKING_START':
        return '🤔';
      case 'THINKING_END':
        return '💡';
      case 'TOOL_CALL_START':
        return '🛠️';
      case 'TOOL_CALL_END':
        return '✅';
      case 'STATE_SNAPSHOT':
        return '📊';
      case 'STATE_DELTA':
        return '📈';
      case 'MESSAGES_SNAPSHOT':
        return '💾';
      case 'RAW':
        return '📋';
      case 'CUSTOM':
        return '🎯';
      default:
        return '📋';
    }
  };

  // 获取事件类型颜色
  const getEventColor = (eventType: string) => {
    if (eventType.includes('START')) return 'border-blue-500 bg-blue-50';
    if (eventType.includes('END') || eventType.includes('FINISHED')) return 'border-green-500 bg-green-50';
    if (eventType.includes('ERROR')) return 'border-red-500 bg-red-50';
    if (eventType.includes('THINKING')) return 'border-purple-500 bg-purple-50';
    if (eventType.includes('TOOL')) return 'border-orange-500 bg-orange-50';
    if (eventType.includes('STATE')) return 'border-indigo-500 bg-indigo-50';
    return 'border-gray-500 bg-gray-50';
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
          <h1 className="text-xl font-bold text-gray-900">AG-UI Event Demo</h1>
          <div className="flex gap-2 items-center">
            <div className="flex items-center gap-2 text-sm">
              <span>Status: {getConnectionIcon()} {connectionStatus}</span>
              <span className="text-gray-400">|</span>
              <span>Backend: {AGUI_CONFIG.API_BASE_URL}</span>
            </div>
            <select
              value={selectedDemo}
              onChange={(e) => setSelectedDemo(e.target.value as DemoType)}
              disabled={isRunning}
              className="px-3 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="full">Full Demo</option>
              <option value="lifecycle">Lifecycle Events</option>
              <option value="text">Text Message Events</option>
              <option value="tool">Tool Call Events</option>
              <option value="state">State Management Events</option>
              <option value="thinking">Thinking Events</option>
              <option value="custom">Custom Events</option>
            </select>
            <button
              onClick={startDemo}
              disabled={isRunning}
              className="px-4 py-1 text-sm bg-blue-500 text-white rounded hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {isRunning ? 'Running...' : 'Start Demo'}
            </button>
            <button
              onClick={stopDemo}
              disabled={!isRunning}
              className="px-4 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              Stop
            </button>
            <button
              onClick={clearLog}
              className="px-3 py-1 text-sm bg-gray-500 text-white rounded hover:bg-gray-600"
            >
              Clear Log
            </button>
          </div>
        </div>
        {currentRunId && (
          <div className="text-xs text-gray-500 mt-2">
            Run ID: {currentRunId}
          </div>
        )}
      </div>

      {/* 事件日志区域 */}
      <div className="flex-1 overflow-y-auto p-4">
        <div className="max-w-4xl mx-auto">
          <div className="mb-4">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">
              Event Log - {selectedDemo.charAt(0).toUpperCase() + selectedDemo.slice(1)} Demo
            </h2>
            <p className="text-sm text-gray-600">
              This demo showcases AG-UI protocol events in real-time. Watch how different event types are emitted during the demo.
            </p>
            <p className="text-xs text-gray-400 mt-1">Backend: {AGUI_CONFIG.API_BASE_URL}</p>
          </div>

          {eventLog.length === 0 && (
            <div className="text-center text-gray-500 py-12">
              <div className="text-4xl mb-4">🎬</div>
              <p className="text-lg">No events yet</p>
              <p className="text-sm mt-2">Click &quot;Start Demo&quot; to begin the AG-UI event demonstration</p>
            </div>
          )}

          <div className="space-y-3">
            {eventLog.map((event, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border-l-4 ${getEventColor(event.type)} shadow-sm`}
              >
                <div className="flex items-start gap-3">
                  <div className="text-2xl">{getEventIcon(event.type)}</div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="font-mono text-sm font-semibold text-gray-900">
                        {event.type}
                      </span>
                      <span className="text-xs text-gray-500">
                        {new Date(event.timestamp * 1000).toLocaleTimeString()}
                      </span>
                    </div>
                    
                    {event.rawEvent && LOG_CONFIG.EVENTS.SHOW_RAW_DATA && (
                      <div className="bg-white p-3 rounded border text-xs">
                        <pre className="whitespace-pre-wrap text-gray-700">
                          {JSON.stringify(event.rawEvent, null, 2)}
                        </pre>
                      </div>
                    )}
                    
                    {event.threadId && (
                      <div className="text-xs text-gray-500 mt-2">
                        Thread: {event.threadId.slice(0, 8)}...
                      </div>
                    )}
                    
                    {event.runId && (
                      <div className="text-xs text-gray-500">
                        Run: {event.runId.slice(0, 8)}...
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 状态栏 */}
      <div className="bg-white border-t border-gray-200 p-3">
        <div className="flex justify-between items-center text-sm text-gray-600">
          <div>
            Status: {getConnectionIcon()} {connectionStatus}
          </div>
          <div>
            Events: {eventLog.length}
          </div>
          <div>
            Demo Type: {selectedDemo.charAt(0).toUpperCase() + selectedDemo.slice(1)}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AguiDemo; 