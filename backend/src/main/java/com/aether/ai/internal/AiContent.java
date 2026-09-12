package com.aether.ai.internal;

import java.util.Map;

/**
 * One block of a conversation turn. Mirrors the shape of the Anthropic
 * Messages API without leaking its wire format into the rest of the module.
 */
sealed interface AiContent {

    record Text(String text) implements AiContent {}

    /** The model asking for a tool to be run. */
    record ToolUse(String id, String name, Map<String, Object> input) implements AiContent {}

    /** Our answer to a {@link ToolUse}, fed back on the next turn. */
    record ToolResult(String toolUseId, String content, boolean isError) implements AiContent {}
}
