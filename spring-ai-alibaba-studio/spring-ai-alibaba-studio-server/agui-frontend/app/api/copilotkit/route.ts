import {
  CopilotRuntime,
  copilotRuntimeNextJSAppRouterEndpoint,
} from '@copilotkit/runtime';
import { NextRequest } from 'next/server';
import { Langgraph4jAdapter } from '../lib/langgraph4j';


const serviceAdapter = new Langgraph4jAdapter();

const runtime = new CopilotRuntime({});

export const POST = async (req: NextRequest) => {

  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter,
    endpoint: "/api/copilotkit",
  });

  return handleRequest(req);
};