import React, { createContext, useContext, useReducer, ReactNode } from 'react';

export interface Message {
  id: string;
  type: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  toolCalls?: any[];
  error?: string;
  attachments?: File[];
}

export interface ChatThread {
  id: string;
  title: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

interface ChatState {
  threads: ChatThread[];
  currentThreadId: string | null;
  isLoading: boolean;
  isStreaming: boolean;
  error: string | null;
}

type ChatAction =
  | { type: 'SET_THREADS'; payload: ChatThread[] }
  | { type: 'ADD_THREAD'; payload: ChatThread }
  | { type: 'SET_CURRENT_THREAD'; payload: string }
  | { type: 'ADD_MESSAGE'; payload: { threadId: string; message: Message } }
  | { type: 'UPDATE_MESSAGE'; payload: { threadId: string; messageId: string; updates: Partial<Message> } }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_STREAMING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'DELETE_THREAD'; payload: string }
  | { type: 'INITIALIZE_THREAD_MESSAGES'; payload: { threadId: string; messages: Message[] } };

const initialState: ChatState = {
  threads: [],
  currentThreadId: null,
  isLoading: false,
  isStreaming: false,
  error: null,
};

const chatReducer = (state: ChatState, action: ChatAction): ChatState => {
  switch (action.type) {
    case 'SET_THREADS':
      return { ...state, threads: action.payload };

    case 'ADD_THREAD':
      return {
        ...state,
        threads: [action.payload, ...state.threads],
        currentThreadId: action.payload.id,
      };

    case 'SET_CURRENT_THREAD':
      return { ...state, currentThreadId: action.payload };

    case 'ADD_MESSAGE':
      return {
        ...state,
        threads: state.threads.map(thread =>
          thread.id === action.payload.threadId
            ? {
                ...thread,
                messages: [...thread.messages, action.payload.message],
                updatedAt: new Date(),
              }
            : thread
        ),
      };

    case 'UPDATE_MESSAGE':
      return {
        ...state,
        threads: state.threads.map(thread =>
          thread.id === action.payload.threadId
            ? {
                ...thread,
                messages: thread.messages.map(msg =>
                  msg.id === action.payload.messageId
                    ? { ...msg, ...action.payload.updates }
                    : msg
                ),
                updatedAt: new Date(),
              }
            : thread
        ),
      };

    case 'INITIALIZE_THREAD_MESSAGES':
      return {
        ...state,
        threads: state.threads.map(thread =>
          thread.id === action.payload.threadId
            ? {
                ...thread,
                messages: action.payload.messages,
                updatedAt: new Date(),
              }
            : thread
        ),
      };

    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };

    case 'SET_STREAMING':
      return { ...state, isStreaming: action.payload };

    case 'SET_ERROR':
      return { ...state, error: action.payload };

    case 'DELETE_THREAD':
      const filteredThreads = state.threads.filter(t => t.id !== action.payload);
      return {
        ...state,
        threads: filteredThreads,
        currentThreadId: state.currentThreadId === action.payload
          ? (filteredThreads.length > 0 ? filteredThreads[0].id : null)
          : state.currentThreadId,
      };

    default:
      return state;
  }
};

interface ChatContextValue {
  state: ChatState;
  dispatch: React.Dispatch<ChatAction>;
  currentThread: ChatThread | null;
  createNewThread: () => void;
  sendMessage: (content: string, attachments?: File[]) => Promise<void>;
  deleteThread: (threadId: string) => void;
  switchThread: (threadId: string) => Promise<void>;
}

const ChatContext = createContext<ChatContextValue | undefined>(undefined);

export const ChatProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  const currentThread = state.threads.find(t => t.id === state.currentThreadId) || null;

  const createNewThread = () => {
    const newThread: ChatThread = {
      id: `thread_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      title: `对话 ${state.threads.length + 1}`,
      messages: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    dispatch({ type: 'ADD_THREAD', payload: newThread });
  };

  const sendMessage = async (content: string, attachments?: File[]) => {
    if (!currentThread) {
      createNewThread();
      return;
    }

    const userMessage: Message = {
      id: `user_msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      type: 'user',
      content,
      timestamp: new Date(),
      attachments,
    };

    console.log('Adding user message:', userMessage.id, 'with content:', userMessage.content);
    dispatch({
      type: 'ADD_MESSAGE',
      payload: { threadId: currentThread.id, message: userMessage }
    });

    dispatch({ type: 'SET_STREAMING', payload: true });

    try {
      // Call real AG-UI backend API
      await simulateAPICall(content, currentThread.id, dispatch);
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: '发送消息失败' });
    } finally {
      dispatch({ type: 'SET_STREAMING', payload: false });
    }
  };

  const deleteThread = (threadId: string) => {
    dispatch({ type: 'DELETE_THREAD', payload: threadId });
  };

  // 添加初始化thread消息的函数
  const initializeThreadMessages = async (threadId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      
      // 调用新的init接口
      const response = await fetch(`http://localhost:8080/api/agui/init/${threadId}`);
      const messagesSnapshot = await response.json();
      
      // 转换AG-UI消息格式到前端格式
      const convertedMessages: Message[] = messagesSnapshot.messages.map((msg: any) => ({
        id: msg.id || `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        type: msg.role === 'user' ? 'user' : 'assistant',
        content: msg.content || '',
        timestamp: new Date(),
        toolCalls: msg.tool_calls || [],
      }));
      
      // 更新thread的消息
      dispatch({ 
        type: 'INITIALIZE_THREAD_MESSAGES', 
        payload: { threadId, messages: convertedMessages } 
      });
      
    } catch (error) {
      console.error('Failed to initialize thread messages:', error);
      dispatch({ type: 'SET_ERROR', payload: '初始化对话记录失败' });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // 添加切换thread的函数
  const switchThread = async (threadId: string) => {
    // 先设置当前thread
    dispatch({ type: 'SET_CURRENT_THREAD', payload: threadId });
    
    // 然后初始化该thread的消息
    await initializeThreadMessages(threadId);
  };

  const value: ChatContextValue = {
    state,
    dispatch,
    currentThread,
    createNewThread,
    sendMessage,
    deleteThread,
    switchThread,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

// Real API call to AG-UI backend
const simulateAPICall = async (
  content: string,
  threadId: string,
  dispatch: React.Dispatch<ChatAction>
) => {
  // Create initial assistant message
  const assistantMessage: Message = {
    id: `assistant_msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    type: 'assistant',
    content: '正在处理您的问题...',
    timestamp: new Date(),
  };

  dispatch({
    type: 'ADD_MESSAGE',
    payload: { threadId, message: assistantMessage }
  });

  // Helper function to safely update assistant message
  const safeUpdateAssistantMessage = (updates: Partial<Message>) => {
    
    if (assistantMessage && assistantMessage.type === 'assistant') {
      dispatch({
        type: 'UPDATE_MESSAGE',
        payload: { 
          threadId, 
          messageId: assistantMessage.id, 
          updates
        }
      });
      console.log('UPDATE_MESSAGE dispatch completed');
    } else {
      console.error('Cannot update assistant message - invalid message:', assistantMessage);
    }
  };

  try {
    // Connect to AG-UI backend for chat messages (running on port 8080)
    // Use POST request with RunAgentInput structure (AG-UI standard)
    const url = `http://localhost:8080/api/agui/stream`;
    // Create RunAgentInput structure following AG-UI standard
    const runAgentInput = {
      threadId: threadId,
      runId: `run_${Date.now()}`,
      state: null,
      messages: [
        {
          id: `user_msg_${Date.now()}`,
          role: "user",
          content: content,
          name: null
        }
      ],
      tools: [
        {
          name: "search_knowledge",
          description: "Search for relevant information",
          parameters: {
            type: "object",
            properties: {
              query: { type: "string" },
              max_results: { type: "number" }
            }
          }
        }
      ],
      context: [
        {
          description: "chat_thread",
          value: threadId
        }
      ],
      forwardedProps: null
    };
    
    // Use fetch with ReadableStream for POST request (AG-UI standard)
    const response = await fetch(url, {
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
    
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullResponse = '';
    let toolCalls: any[] = [];
    let buffer = ''; // Buffer for incomplete lines
    
    const processAGUIEvent = (data: any) => {
      console.log('Processing AG-UI event:', data);
      console.log('Current fullResponse before processing:', fullResponse);
      
      // Process all events for this chat session
      // AG-UI Event classes have direct fields, not rawEvent
      switch (data.type) {
        case "RUN_STARTED":
          console.log('RUN_STARTED: Agent run started');
          break;
          
        case "STEP_STARTED":
          console.log('STEP_STARTED: Processing step started');
          break;
          
        case "TEXT_MESSAGE_START":
          // Reset response for new message
          fullResponse = '';
          console.log('TEXT_MESSAGE_START: Starting new text message, reset fullResponse to empty');
          break;
          
        case "TEXT_MESSAGE_CONTENT":
          // Extract delta directly from event and accumulate content
          const contentChunk = data.delta || '';
          console.log('TEXT_MESSAGE_CONTENT: Processing chunk:', contentChunk, 'Current fullResponse:', fullResponse);
          
          if (contentChunk.trim()) {
            fullResponse += contentChunk;
            console.log('TEXT_MESSAGE_CONTENT: Updated fullResponse to:', fullResponse);
            
            // Update assistant message with accumulated content in real-time
            console.log('TEXT_MESSAGE_CONTENT: About to call safeUpdateAssistantMessage with content:', fullResponse);
            safeUpdateAssistantMessage({ content: fullResponse });
            console.log('TEXT_MESSAGE_CONTENT: Called safeUpdateAssistantMessage with content:', fullResponse);
          }
          break;
          
        case "TEXT_MESSAGE_END":
          console.log('TEXT_MESSAGE_END: Message completed, final content:', fullResponse);
          break;
          
        case "TOOL_CALL_START":
          // Extract tool call info directly from event
          const newToolCall = {
            id: data.tool_call_id, // Store the tool_call_id for matching
            name: data.tool_call_name || 'unknown_tool',
            arguments: {},
            result: null,
            status: 'running'
          };
          toolCalls.push(newToolCall);
          console.log('TOOL_CALL_START: Added tool call:', newToolCall);
          
          // Update tool calls array
          safeUpdateAssistantMessage({ 
            toolCalls: [...toolCalls]
          });
          break;
          
        case "TOOL_CALL_ARGS":
          // Update tool call arguments directly from event
          if (toolCalls.length > 0) {
            // Find tool call by tool_call_id for better matching
            const toolCall = toolCalls.find(tc => tc.id === data.tool_call_id);
            if (toolCall) {
              // The delta contains tool parameters in a readable format
              // Example: "{type=object, properties={query={type=string}, max_results={type=number}}}"
              toolCall.arguments = data.delta || '{}';
              console.log('TOOL_CALL_ARGS: Updated arguments for tool:', toolCall.name, 'args:', toolCall.arguments);
              
              // Update tool calls array
              safeUpdateAssistantMessage({ 
                toolCalls: [...toolCalls]
              });
            }
          }
          break;
          
        case "TOOL_CALL_END":
          // Tool call is complete, update status
          if (toolCalls.length > 0) {
            const toolCall = toolCalls.find(tc => tc.id === data.tool_call_id);
            if (toolCall) {
              toolCall.status = 'completed';
              console.log('TOOL_CALL_END: Tool call completed:', toolCall.name);
              
              // Update tool calls array
              safeUpdateAssistantMessage({ 
                toolCalls: [...toolCalls]
              });
            }
          }
          break;
          
        case "TOOL_CALL_RESULT":
          // Tool call result according to AG-UI standard
          if (toolCalls.length > 0) {
            const toolCall = toolCalls.find(tc => tc.id === data.tool_call_id);
            if (toolCall) {
              toolCall.result = data.content || '';
              toolCall.status = 'completed';
              console.log('TOOL_CALL_RESULT: Updated tool call with result:', toolCall);
              
              // Update tool calls array
              safeUpdateAssistantMessage({ 
                toolCalls: [...toolCalls]
              });
            }
          }
          break;
          
        case "STATE_SNAPSHOT":
          console.log('STATE_SNAPSHOT: Received state snapshot:', data.snapshot);
          break;
          
        case "MESSAGES_SNAPSHOT":
          // 移除这个case的处理，因为现在由init接口处理
          console.log('MESSAGES_SNAPSHOT: Ignored - handled by init endpoint');
          break;
          
        case "STEP_FINISHED":
          break;
          
        case "RUN_FINISHED":
          // Finalize message with accumulated content and tool calls
          safeUpdateAssistantMessage({ 
            content: fullResponse || '处理完成',
            toolCalls: toolCalls.length > 0 ? toolCalls : undefined
          });
          break;
          
        default:
          break;
      }
    };

    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          break;
        }
        
        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk; // Add new chunk to buffer
        
        // Process complete lines from buffer
        const lines = buffer.split('\n');
        
        // Keep the last incomplete line in buffer
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          
          if (line.trim() === '') {
            continue;
          }
          
          // Handle data:{...} format
          if (line.includes('data:')) {
            const dataIndex = line.indexOf('data:');
            const afterData = line.substring(dataIndex + 5).trim(); // +5 for "data:"
            
            if (afterData) {
              try {
                const data = JSON.parse(afterData);
                console.log('=== PARSED EVENT ===');
                console.log('Event type:', data.type);
                console.log('Event data:', data);
                console.log('Current fullResponse before processing:', fullResponse);
                
                processAGUIEvent(data);
                
                console.log('Current fullResponse after processing:', fullResponse);
                console.log('=== END EVENT ===');
              } catch (error) {
                console.error('Failed to parse AG-UI event:', error, 'from line:', line);
              }
            } else {
              console.log('Line contains data: but no JSON content:', line);
            }
          } else {
            console.log('Line does not contain data: prefix:', line);
          }
        }
      }
    } finally {
      reader.releaseLock();
    }

  } catch (error) {
    console.error('AG-UI API call failed:', error);
    safeUpdateAssistantMessage({ 
      content: '抱歉，无法连接到后端服务，请检查服务状态。',
      error: '连接失败'
    });
  }
};

export const useChatContext = () => {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
};
