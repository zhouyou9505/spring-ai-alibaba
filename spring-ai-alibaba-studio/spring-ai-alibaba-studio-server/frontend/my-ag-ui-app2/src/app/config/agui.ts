// AG-UI 配置文件
export const AGUI_CONFIG = {
  // 后端API基础URL
  API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  
  // AG-UI API端点
  ENDPOINTS: {
    RUN: '/api/agui/run',
    CHAT: '/api/agui/chat',
    DEMO: '/api/agui/demo',
    HEALTH: '/api/agui/health',
    PROTOCOL: '/api/agui/protocol'
  },
  
  // 事件类型配置
  EVENT_TYPES: {
    LIFECYCLE: ['RUN_STARTED', 'RUN_FINISHED', 'RUN_ERROR', 'STEP_STARTED', 'STEP_FINISHED'],
    TEXT_MESSAGE: ['TEXT_MESSAGE_START', 'TEXT_MESSAGE_CONTENT', 'TEXT_MESSAGE_END'],
    THINKING: ['THINKING_START', 'THINKING_END'],
    TOOL_CALL: ['TOOL_CALL_START', 'TOOL_CALL_ARGS', 'TOOL_CALL_END'],
    STATE_MANAGEMENT: ['STATE_SNAPSHOT', 'STATE_DELTA', 'MESSAGES_SNAPSHOT'],
    SPECIAL: ['RAW', 'CUSTOM']
  },
  
  // 默认配置
  DEFAULTS: {
    TIMEOUT: 30000, // 30秒超时
    RETRY_ATTEMPTS: 3,
    CHUNK_DELAY: 100, // 流式响应块延迟
    MAX_MESSAGE_LENGTH: 1000
  },
  
  // 演示配置
  DEMO: {
    DELAYS: {
      LIFECYCLE: 500,
      TEXT: 200,
      TOOL: 300,
      THINKING: 400,
      STATE: 250
    }
  }
};

// 获取完整的API URL
export const getApiUrl = (endpoint: string): string => {
  return `${AGUI_CONFIG.API_BASE_URL}${endpoint}`;
};

// 验证配置
export const validateConfig = (): boolean => {
  return AGUI_CONFIG.API_BASE_URL.length > 0;
};

// 环境检查
export const isDevelopment = (): boolean => {
  return process.env.NODE_ENV === 'development';
};

// 日志配置
export const LOG_CONFIG = {
  ENABLED: isDevelopment(),
  LEVEL: isDevelopment() ? 'debug' : 'info',
  EVENTS: {
    SHOW_ALL: isDevelopment(),
    SHOW_TIMESTAMPS: true,
    SHOW_RAW_DATA: isDevelopment()
  }
}; 