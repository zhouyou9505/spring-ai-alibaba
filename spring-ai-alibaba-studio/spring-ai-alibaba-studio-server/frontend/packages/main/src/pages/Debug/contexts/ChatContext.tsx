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

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

interface ChatState {
  sessions: ChatSession[];
  currentSessionId: string | null;
  isLoading: boolean;
  isStreaming: boolean;
  error: string | null;
}

type ChatAction =
  | { type: 'SET_SESSIONS'; payload: ChatSession[] }
  | { type: 'ADD_SESSION'; payload: ChatSession }
  | { type: 'SET_CURRENT_SESSION'; payload: string }
  | { type: 'ADD_MESSAGE'; payload: { sessionId: string; message: Message } }
  | { type: 'UPDATE_MESSAGE'; payload: { sessionId: string; messageId: string; updates: Partial<Message> } }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_STREAMING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'DELETE_SESSION'; payload: string };

const initialState: ChatState = {
  sessions: [],
  currentSessionId: null,
  isLoading: false,
  isStreaming: false,
  error: null,
};

const chatReducer = (state: ChatState, action: ChatAction): ChatState => {
  switch (action.type) {
    case 'SET_SESSIONS':
      return { ...state, sessions: action.payload };

    case 'ADD_SESSION':
      return {
        ...state,
        sessions: [action.payload, ...state.sessions],
        currentSessionId: action.payload.id,
      };

    case 'SET_CURRENT_SESSION':
      return { ...state, currentSessionId: action.payload };

    case 'ADD_MESSAGE':
      return {
        ...state,
        sessions: state.sessions.map(session =>
          session.id === action.payload.sessionId
            ? {
                ...session,
                messages: [...session.messages, action.payload.message],
                updatedAt: new Date(),
              }
            : session
        ),
      };

    case 'UPDATE_MESSAGE':
      return {
        ...state,
        sessions: state.sessions.map(session =>
          session.id === action.payload.sessionId
            ? {
                ...session,
                messages: session.messages.map(msg =>
                  msg.id === action.payload.messageId
                    ? { ...msg, ...action.payload.updates }
                    : msg
                ),
                updatedAt: new Date(),
              }
            : session
        ),
      };

    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };

    case 'SET_STREAMING':
      return { ...state, isStreaming: action.payload };

    case 'SET_ERROR':
      return { ...state, error: action.payload };

    case 'DELETE_SESSION':
      const filteredSessions = state.sessions.filter(s => s.id !== action.payload);
      return {
        ...state,
        sessions: filteredSessions,
        currentSessionId: state.currentSessionId === action.payload
          ? (filteredSessions.length > 0 ? filteredSessions[0].id : null)
          : state.currentSessionId,
      };

    default:
      return state;
  }
};

interface ChatContextValue {
  state: ChatState;
  dispatch: React.Dispatch<ChatAction>;
  currentSession: ChatSession | null;
  createNewSession: () => void;
  sendMessage: (content: string, attachments?: File[]) => Promise<void>;
  deleteSession: (sessionId: string) => void;
}

const ChatContext = createContext<ChatContextValue | undefined>(undefined);

export const ChatProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [state, dispatch] = useReducer(chatReducer, initialState);

  const currentSession = state.sessions.find(s => s.id === state.currentSessionId) || null;

  const createNewSession = () => {
    const newSession: ChatSession = {
      id: `session_${Date.now()}`,
      title: `对话 ${state.sessions.length + 1}`,
      messages: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    dispatch({ type: 'ADD_SESSION', payload: newSession });
  };

  const sendMessage = async (content: string, attachments?: File[]) => {
    if (!currentSession) {
      createNewSession();
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
      payload: { sessionId: currentSession.id, message: userMessage }
    });

    dispatch({ type: 'SET_STREAMING', payload: true });

    try {
      // Call real AG-UI backend API
      await simulateAPICall(content, currentSession.id, dispatch);
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: '发送消息失败' });
    } finally {
      dispatch({ type: 'SET_STREAMING', payload: false });
    }
  };

  const deleteSession = (sessionId: string) => {
    dispatch({ type: 'DELETE_SESSION', payload: sessionId });
  };

  const value: ChatContextValue = {
    state,
    dispatch,
    currentSession,
    createNewSession,
    sendMessage,
    deleteSession,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

// Real API call to AG-UI backend
const simulateAPICall = async (
  content: string,
  sessionId: string,
  dispatch: React.Dispatch<ChatAction>
) => {
  // Create initial assistant message
  const assistantMessage: Message = {
    id: `assistant_msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    type: 'assistant',
    content: '正在处理您的问题...',
    timestamp: new Date(),
  };

  console.log('Creating assistant message with ID:', assistantMessage.id);
  console.log('User question:', content);

  console.log('Adding assistant message:', assistantMessage.id, 'with content:', assistantMessage.content);
  dispatch({
    type: 'ADD_MESSAGE',
    payload: { sessionId, message: assistantMessage }
  });

  // Helper function to safely update assistant message
  const safeUpdateAssistantMessage = (updates: Partial<Message>) => {
    if (assistantMessage && assistantMessage.type === 'assistant') {
      console.log('Safely updating assistant message:', assistantMessage.id, 'with updates:', updates);
      dispatch({
        type: 'UPDATE_MESSAGE',
        payload: { 
          sessionId, 
          messageId: assistantMessage.id, 
          updates
        }
      });
    } else {
      console.error('Cannot update assistant message - invalid message:', assistantMessage);
    }
  };

  try {
    // Connect to AG-UI backend for chat messages (running on port 8080)
    // Use different parameters to avoid conflict with debug stream
    const url = `http://localhost:8080/api/agui/stream?question=${encodeURIComponent(content)}&filter=ALL&limit=120&mode=chat`;
    console.log('Connecting to AG-UI backend for chat:', url);
    
    const es = new EventSource(url);
    let fullResponse = '';
    let toolCalls: any[] = [];
    
    // Listen to SSE events directly (AG-UI standard)
    es.onmessage = (ev: MessageEvent) => {
      try {
        const data = JSON.parse(ev.data);
        console.log('Received AG-UI event for chat:', data);
        
        // Process all events for this chat session
        // Note: Backend returns a fixed messageId, so we process all events
        
        switch (data.type) {
          case "TEXT_MESSAGE_START":
            // Reset response for new message
            fullResponse = '';
            console.log('TEXT_MESSAGE_START: Resetting response for assistant message:', assistantMessage.id);
            break;
            
          case "TEXT_MESSAGE_CHUNK":
            // Accumulate content from AI response (not user input)
            const chunk = data.delta || '';
            fullResponse += chunk;
            console.log('TEXT_MESSAGE_CHUNK: Received chunk:', chunk, 'Full response so far:', fullResponse);
            
            // Safely update assistant message with accumulated content
            safeUpdateAssistantMessage({ content: fullResponse });
            break;
            
          case "TOOL_CALL_START":
            // Add tool call
            toolCalls.push({
              name: data.toolCallName || 'unknown_tool',
              arguments: {},
              result: null
            });
            break;
            
          case "TOOL_CALL_ARGS":
            // Update tool call arguments
            if (toolCalls.length > 0) {
              const lastTool = toolCalls[toolCalls.length - 1];
              lastTool.arguments = { ...lastTool.arguments, ...data.delta };
            }
            break;
            
          case "TOOL_CALL_RESULT":
            // Update tool call result
            if (toolCalls.length > 0) {
              const lastTool = toolCalls[toolCalls.length - 1];
              lastTool.result = data.content;
            }
            break;
            
          case "RUN_FINISHED":
            // Finalize message with tool calls
            console.log('Finalizing assistant message with tool calls:', toolCalls);
            safeUpdateAssistantMessage({ 
              content: fullResponse || '处理完成',
              toolCalls: toolCalls.length > 0 ? toolCalls : undefined
            });
            es.close();
            break;
        }
      } catch (error) {
        console.error('Failed to parse AG-UI event:', error);
      }
    };

    es.onerror = (error) => {
      console.error('AG-UI EventSource error:', error);
      safeUpdateAssistantMessage({ 
        content: '抱歉，处理过程中出现错误，请稍后重试。',
        error: '连接错误'
      });
      es.close();
    };

    // Set timeout to prevent hanging
    setTimeout(() => {
      if (es.readyState !== EventSource.CLOSED) {
        es.close();
        if (fullResponse === '') {
          safeUpdateAssistantMessage({ 
            content: '处理超时，请稍后重试。',
            error: '超时错误'
          });
        }
      }
    }, 30000); // 30 seconds timeout

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
