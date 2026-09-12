package com.aether.ai.internal;

import java.util.List;

/**
 * Port to the model provider. Kept as an interface so the orchestration can be
 * tested with a scripted double — no API key and no network in tests or CI.
 */
interface AnthropicClient {

    AiTurn send(String systemPrompt, List<AiMessage> messages, List<ToolSpec> tools);
}
