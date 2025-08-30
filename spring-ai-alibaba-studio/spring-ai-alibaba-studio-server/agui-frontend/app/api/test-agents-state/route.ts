import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    console.log("Testing backend /agents/state endpoint");
    
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    const testUrl = `${backendUrl}/copilotkit/agents/state`;
    
    const testPayload = {
      threadId: "test-thread-123",
      name: "ai_researcher",
      properties: {}
    };
    
    console.log("Making request to:", testUrl);
    console.log("Payload:", testPayload);
    
    const response = await fetch(testUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(testPayload),
    });
    
    console.log("Response status:", response.status);
    console.log("Response headers:", Object.fromEntries(response.headers.entries()));
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error("Error response:", errorText);
      return NextResponse.json({
        success: false,
        error: `HTTP ${response.status}`,
        details: errorText,
        url: testUrl,
      });
    }
    
    const data = await response.json();
    console.log("Response data:", data);
    
    // Validate the response format matches CopilotKit's LoadAgentStateResponse
    const validation = {
      hasThreadId: typeof data.threadId === 'string' && data.threadId !== null,
      hasThreadExists: typeof data.threadExists === 'boolean',
      hasState: typeof data.state === 'string',
      hasMessages: typeof data.messages === 'string',
      threadIdValue: data.threadId,
      threadExistsValue: data.threadExists,
      stateValue: data.state,
      messagesValue: data.messages,
    };
    
    return NextResponse.json({
      success: true,
      url: testUrl,
      response: data,
      validation,
      timestamp: new Date().toISOString(),
    });
    
  } catch (error: any) {
    console.error("Test failed:", error);
    return NextResponse.json({
      success: false,
      error: error.message,
      stack: error.stack,
    });
  }
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    console.log("Testing with custom payload:", body);
    
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    const testUrl = `${backendUrl}/copilotkit/agents/state`;
    
    const response = await fetch(testUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
    
    console.log("Response status:", response.status);
    
    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json({
        success: false,
        error: `HTTP ${response.status}`,
        details: errorText,
        url: testUrl,
      });
    }
    
    const data = await response.json();
    
    return NextResponse.json({
      success: true,
      url: testUrl,
      response: data,
      timestamp: new Date().toISOString(),
    });
    
  } catch (error: any) {
    console.error("Test failed:", error);
    return NextResponse.json({
      success: false,
      error: error.message,
    });
  }
}