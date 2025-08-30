import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    console.log("Testing backend connection...");
    
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    
    // Test health endpoint
    const healthResponse = await fetch(`${backendUrl}/copilotkit/health`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (healthResponse.ok) {
      const healthData = await healthResponse.json();
      console.log("Health check successful:", healthData);
      
      // Test info endpoint
      const infoResponse = await fetch(`${backendUrl}/copilotkit/info`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (infoResponse.ok) {
        const infoData = await infoResponse.json();
        console.log("Info endpoint successful");
        
        // Test agents/execute endpoint with a simple payload
        const testPayload = {
          threadId: "test-thread-123",
          name: "ai_researcher",
          nodeName: "chat_node",
          state: {},
          messages: [
            {
              id: "test-msg-1",
              role: "user",
              content: "Hello"
            }
          ],
          actions: [],
          tools: []
        };
        
        const executeResponse = await fetch(`${backendUrl}/copilotkit/agents/execute`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
          },
          body: JSON.stringify(testPayload),
        });
        
        console.log("Execute endpoint response status:", executeResponse.status);
        
        if (executeResponse.ok) {
          return NextResponse.json({
            status: "success",
            backend: "connected",
            health: healthData,
            info: "available",
            agents: "available",
            execute: "working",
            timestamp: new Date().toISOString(),
          });
        } else {
          const errorText = await executeResponse.text();
          return NextResponse.json({
            status: "partial_success",
            backend: "connected",
            health: healthData,
            info: "available", 
            execute: "failed",
            executeError: `HTTP ${executeResponse.status}: ${errorText}`,
            timestamp: new Date().toISOString(),
          });
        }
      } else {
        return NextResponse.json({
          status: "partial_success",
          backend: "connected",
          health: healthData,
          info: "failed",
          infoError: `HTTP ${infoResponse.status}`,
          timestamp: new Date().toISOString(),
        });
      }
    } else {
      return NextResponse.json({
        status: "error",
        backend: "connection_failed",
        error: `Health check failed: HTTP ${healthResponse.status}`,
        timestamp: new Date().toISOString(),
      });
    }
    
  } catch (error) {
    console.error("Backend connection test failed:", error);
    
    return NextResponse.json({
      status: "error",
      backend: "unreachable",
      error: error instanceof Error ? error.message : "Unknown error",
      timestamp: new Date().toISOString(),
    });
  }
}