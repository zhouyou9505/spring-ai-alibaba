import { NextRequest } from "next/server";
import { NextRequest } from "next/server";
import {
  CopilotRuntime,
  OpenAIAdapter,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";

// Even when using remoteEndpoints, we need a serviceAdapter for the function signature
// This adapter won't be used for actual LLM processing (handled by remote endpoint)
// but is required for the endpoint configuration
const serviceAdapter = new OpenAIAdapter({
  // Dummy configuration - won't be used since we have remoteEndpoints
  model: "gpt-3.5-turbo",
});

// When using remoteEndpoints, the Spring Boot backend handles the LLM processing
const runtime = new CopilotRuntime({
  remoteEndpoints: [
    {
      url: process.env.BACKEND_URL ? `${process.env.BACKEND_URL}/copilotkit` : "http://localhost:8080/copilotkit",
    },
  ],
});

export const POST = async (req: NextRequest) => {
  try {
    console.log("CopilotKit route: Processing request", {
      url: req.url,
      method: req.method,
      headers: Object.fromEntries(req.headers.entries())
    });
    
    // Test backend connectivity first
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
      const testResponse = await fetch(`${backendUrl}/copilotkit/health`, {
        method: "GET",
        signal: controller.signal,
      });
      
      clearTimeout(timeoutId);
      console.log("Backend health check:", testResponse.status);
    } catch (healthError) {
      console.warn("Backend health check failed:", healthError);
      // Continue anyway as health endpoint might not exist
    }
    
    const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
      runtime,
      serviceAdapter, // Required even when using remoteEndpoints
      endpoint: "/api/copilotkit",
    });

    console.log("CopilotKit route: Request handler created, processing...");
    const response = await handleRequest(req);
    
    console.log("CopilotKit route: Request processed successfully", {
      status: response.status,
      headers: Object.fromEntries(response.headers.entries())
    });
    return response;
    
  } catch (error) {
    console.error("CopilotKit route error:", error);
    
    // Log additional error details for debugging
    if (error instanceof Error) {
      console.error("Error name:", error.name);
      console.error("Error message:", error.message);
      console.error("Error stack:", error.stack);
    }
    
    // Return a proper error response
    return new Response(
      JSON.stringify({
        error: "Internal server error",
        message: error instanceof Error ? error.message : "Unknown error",
        timestamp: new Date().toISOString(),
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
  }
};

// Add GET endpoint for health checks and info
export const GET = async (req: NextRequest) => {
  try {
    const url = new URL(req.url);
    
    // Handle health check
    if (url.pathname.endsWith('/health')) {
      const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
      return new Response(
        JSON.stringify({
          status: "healthy",
          service: "CopilotKit Frontend Proxy",
          backend: `${backendUrl}/copilotkit`,
          timestamp: new Date().toISOString(),
        }),
        {
          status: 200,
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
    }
    
    // For other GET requests, proxy to backend
    const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
      runtime,
      serviceAdapter, // Required even when using remoteEndpoints
      endpoint: "/api/copilotkit",
    });
    
    return handleRequest(req);
    
  } catch (error) {
    console.error("CopilotKit GET route error:", error);
    
    return new Response(
      JSON.stringify({
        error: "GET request failed",
        message: error instanceof Error ? error.message : "Unknown error",
        timestamp: new Date().toISOString(),
      }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
        },
      }
    );
  }
};
