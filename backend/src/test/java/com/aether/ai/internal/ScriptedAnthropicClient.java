package com.aether.ai.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A model that answers from a script. Lets the whole tool-use loop be tested
 * without an API key and without touching the network — which is what CI runs.
 */
class ScriptedAnthropicClient implements AnthropicClient {

    private final Deque<AiTurn> turns = new ArrayDeque<>();
    private final List<Call> calls = new ArrayList<>();

    record Call(String systemPrompt, List<AiMessage> messages, List<ToolSpec> tools) {}

    ScriptedAnthropicClient script(AiTurn... scripted) {
        turns.clear();
        calls.clear();
        turns.addAll(List.of(scripted));
        return this;
    }

    List<Call> calls() {
        return calls;
    }

    Call lastCall() {
        return calls.getLast();
    }

    @Override
    public AiTurn send(String systemPrompt, List<AiMessage> messages, List<ToolSpec> tools) {
        calls.add(new Call(systemPrompt, List.copyOf(messages), List.copyOf(tools)));
        AiTurn next = turns.poll();
        if (next == null) {
            throw new AssertionError("the model was called more times than the script allows");
        }
        return next;
    }

    static AiTurn text(String text) {
        return new AiTurn(List.of(new AiContent.Text(text)), "end_turn");
    }

    static AiTurn toolUse(String id, String name, Map<String, Object> input) {
        return new AiTurn(List.of(new AiContent.ToolUse(id, name, input)), "tool_use");
    }
}
