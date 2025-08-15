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
    console.log('safeUpdateAssistantMessage called with updates:', updates);
    console.log('Current assistantMessage:', assistantMessage);
    
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
      console.log('UPDATE_MESSAGE dispatch completed');
    } else {
      console.error('Cannot update assistant message - invalid message:', assistantMessage);
    }
  };

  try {
    // Connect to AG-UI backend for chat messages (running on port 8080)
    // Use POST request with RunAgentInput structure (AG-UI standard)
    const url = `http://localhost:8080/api/agui/stream`;
    console.log('Connecting to AG-UI backend for chat:', url);
    
    // Create RunAgentInput structure following AG-UI standard
    const runAgentInput = {
      threadId: sessionId,
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
          description: "chat_session",
          value: sessionId
        }
      ],
      forwardedProps: null
    };
    
    console.log('Sending RunAgentInput:', runAgentInput);
    
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
    
    console.log('Starting SSE stream processing with initial fullResponse:', fullResponse);
    
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
            const lastTool = toolCalls[toolCalls.length - 1];
            // Parse delta as JSON for arguments
            try {
              lastTool.arguments = JSON.parse(data.delta || '{}');
            } catch (e) {
              lastTool.arguments = {};
            }
            console.log('TOOL_CALL_ARGS: Updated arguments for tool:', lastTool.name, 'args:', lastTool.arguments);
            
            // Update tool calls array
            safeUpdateAssistantMessage({ 
              toolCalls: [...toolCalls]
            });
          }
          break;
          
        case "TOOL_CALL_RESULT":
          // Update tool call result directly from event
          if (toolCalls.length > 0) {
            const lastTool = toolCalls[toolCalls.length - 1];
            lastTool.result = data.content;
            lastTool.status = 'completed';
            console.log('TOOL_CALL_RESULT: Updated result for tool:', lastTool.name, 'result:', lastTool.result);
            
            // Update tool calls array
            safeUpdateAssistantMessage({ 
              toolCalls: [...toolCalls]
            });
          }
          break;
          
        case "STATE_SNAPSHOT":
          console.log('STATE_SNAPSHOT: Received state snapshot:', data.snapshot);
          break;
          
        case "MESSAGES_SNAPSHOT":
          console.log('MESSAGES_SNAPSHOT: Received messages snapshot with', data.messages?.length || 0, 'messages');
          // Update the final message content from snapshot if we have it
          if (data.messages && data.messages.length > 0) {
            const lastMessage = data.messages[data.messages.length - 1];
            if (lastMessage.role === 'assistant') {
              safeUpdateAssistantMessage({ 
                content: lastMessage.content,
                toolCalls: lastMessage.tool_calls || []
              });
            }
          }
          break;
          
        case "STEP_FINISHED":
          console.log('STEP_FINISHED: Processing step completed');
          break;
          
        case "RUN_FINISHED":
          console.log('RUN_FINISHED: Agent run completed with result:', data.result);
          
          // Finalize message with accumulated content and tool calls
          safeUpdateAssistantMessage({ 
            content: fullResponse || '处理完成',
            toolCalls: toolCalls.length > 0 ? toolCalls : undefined
          });
          break;
          
        default:
          console.log('Unhandled event type:', data.type, data);
          break;
      }
    };

    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          console.log('Stream completed');
          break;
        }
        
        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk; // Add new chunk to buffer
        
        // Process complete lines from buffer
        const lines = buffer.split('\n');
        
        // Keep the last incomplete line in buffer
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          console.log('Processing line:', line);
          
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
