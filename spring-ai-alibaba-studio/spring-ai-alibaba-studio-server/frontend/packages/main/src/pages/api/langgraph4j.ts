// 从环境变量获取后端地址，开发环境默认 localhost:8080
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

export default async function handler(req: any, res: any) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    console.log('LangGraph4j API 请求:', req.body);
    console.log('后端地址:', BACKEND_URL);
    
    // 直接转发请求到后端服务
    const backendResponse = await fetch(`${BACKEND_URL}/api/v1/agui/copilotkit`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(req.body),
    });

    if (!backendResponse.ok) {
      throw new Error(`Backend error: ${backendResponse.status}`);
    }

    // 设置 SSE 响应头
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type',
    });

    // 获取后端响应流并转发
    const reader = backendResponse.body?.getReader();
    if (!reader) {
      throw new Error('No response body');
    }

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        // 转发后端的数据
        res.write(value);
      }
    } finally {
      reader.releaseLock();
    }

    res.end();

  } catch (error: any) {
    console.error('LangGraph4j API error:', error);
    
    // 如果后端不可用，返回错误响应
    res.status(500).json({ 
      error: 'Backend service unavailable',
      details: error.message 
    });
  }
}
