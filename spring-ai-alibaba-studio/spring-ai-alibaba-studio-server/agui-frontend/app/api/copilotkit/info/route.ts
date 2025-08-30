import { NextRequest } from "next/server";

/**
 * CopilotKit Info endpoint
 * This endpoint is called by CopilotKit frontend to get information about available agents and actions
 * It proxies the request to the Spring Boot backend
 * Supports both GET and POST methods
 */
export const GET = async (req: NextRequest) => {
  return handleInfoRequest(req, "GET");
};

export const POST = async (req: NextRequest) => {
  return handleInfoRequest(req, "POST");
};

async function handleInfoRequest(req: NextRequest, method: "GET" | "POST") {
  try {
    console.log(`CopilotKit info: Proxying ${method} request to backend`);
    
    // Forward the request to the Spring Boot backend
    const backendUrl = process.env.BACKEND_URL || "http://localhost:8080";
    const backendInfoUrl = `${backendUrl}/copilotkit/info`;
    
    // Get request body for POST requests
    let requestBody = null;
    if (method === "POST") {
      try {
        requestBody = await req.text();
        console.log("Request body:", requestBody);
      } catch (error) {
        console.warn("Failed to read request body:", error);
      }
    }
    
    const response = await fetch(backendInfoUrl, {
      method,
      headers: {
        "Content-Type": "application/json",
        // Forward any relevant headers
        ...Object.fromEntries(
          Array.from(req.headers.entries()).filter(([key]) => 
            key.toLowerCase().startsWith("authorization") || 
            key.toLowerCase().startsWith("x-")
          )
        ),
      },
      ...(method === "POST" && requestBody ? { body: requestBody } : {}),
    });

    if (!response.ok) {
      throw new Error(`Backend responded with status ${response.status}`);
    }

    const data = await response.json();
    console.log("CopilotKit info: Backend response received", {
      data,
      hasActions: Array.isArray(data.actions),
      hasAgents: Array.isArray(data.agents),
      actionsLength: data.actions ? data.actions.length : 'undefined',
      agentsLength: data.agents ? data.agents.length : 'undefined'
    });

    // Validate and fix response format to ensure CopilotKit compatibility
    const validatedData = {
      ...data,
      agents: Array.isArray(data.agents) ? data.agents : [],
      actions: Array.isArray(data.actions) ? data.actions : [],
    };

    console.log("CopilotKit info: Validated response", {
      originalData: data,
      validatedData,
      validation: {
        agentsIsArray: Array.isArray(validatedData.agents),
        actionsIsArray: Array.isArray(validatedData.actions),
        agentsCount: validatedData.agents.length,
        actionsCount: validatedData.actions.length
      }
    });

    return new Response(JSON.stringify(validatedData), {
      status: 200,
      headers: {
        "Content-Type": "application/json",
        // Add CORS headers
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });

  } catch (error) {
    console.error(`CopilotKit info ${method} endpoint error:`, error);
    
    // Return error but ensure the structure matches what CopilotKit expects
    const errorResponse = {
      error: "Failed to get CopilotKit info from backend",
      message: error instanceof Error ? error.message : "Unknown error",
      agents: [], // Ensure agents array is present even in error
      actions: [], // Ensure actions array is present even in error
      status: "error",
      timestamp: new Date().toISOString(),
    };

    return new Response(JSON.stringify(errorResponse), {
      status: 500,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  }
}

// Handle OPTIONS request for CORS
export const OPTIONS = async () => {
  return new Response(null, {
    status: 200,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    },
  });
};