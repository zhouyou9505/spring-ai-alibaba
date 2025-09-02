import { NextRequest } from "next/server";
import {
  CopilotRuntime,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";
import { Langgraph4jAdapter } from "../lib/langgraph4j";

// ServiceAdapter Architecture Configuration
// Use Langgraph4jAdapter for direct backend communication
const serviceAdapter = new Langgraph4jAdapter();

// Runtime configured for ServiceAdapter mode
const runtime = new CopilotRuntime({});

// ServiceAdapter endpoint - direct communication with backend
export const POST = async (req: NextRequest) => {
  // Debug logging
  if (process.env.NEXT_PUBLIC_DEBUG === 'true') {
    console.log('[DEBUG] ServiceAdapter Request:', {
      method: req.method,
      url: req.url,
      headers: Object.fromEntries(req.headers.entries()),
      timestamp: new Date().toISOString()
    });
  }

  try {
    const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
      runtime,
      serviceAdapter, // Direct ServiceAdapter communication
      endpoint: "/api/copilotkit",
    });

    const response = await handleRequest(req);
    
    if (process.env.NEXT_PUBLIC_DEBUG === 'true') {
      console.log('[DEBUG] ServiceAdapter Response:', {
        status: response.status,
        headers: Object.fromEntries(response.headers.entries()),
        timestamp: new Date().toISOString()
      });
    }
    
    return response;
  } catch (error) {
    console.error('[ERROR] ServiceAdapter Error:', error);
    return new Response(
      JSON.stringify({
        error: 'Internal Server Error',
        message: error instanceof Error ? error.message : 'Unknown error',
        timestamp: new Date().toISOString()
      }),
      {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
};
