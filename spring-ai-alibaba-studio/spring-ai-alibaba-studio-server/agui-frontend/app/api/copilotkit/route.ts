import { NextRequest } from "next/server";
import {
  CopilotRuntime,
  OpenAIAdapter,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";
import OpenAI from "openai";

// Agent Lock Mode Configuration
// ServiceAdapter only used for peripherals (suggestions, etc.) since we have a dedicated agent
const openai = new OpenAI();
const serviceAdapter = new OpenAIAdapter({ openai } as any);

// Runtime configured for Agent Lock Mode with our Spring Boot backend
const runtime = new CopilotRuntime({
  remoteEndpoints: [
    {
      url: process.env.BACKEND_URL ? `${process.env.BACKEND_URL}/copilotkit` : "http://localhost:8080/copilotkit",
    },
  ],
});

// Agent Lock Mode endpoint - all requests will be handled by the ai_researcher agent
export const POST = async (req: NextRequest) => {
  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter, // Only for peripheral operations like suggestions
    endpoint: "/api/copilotkit",
  });

  return handleRequest(req);
};
