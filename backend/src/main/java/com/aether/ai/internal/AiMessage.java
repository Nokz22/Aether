package com.aether.ai.internal;

import java.util.List;

/** A turn in the conversation. {@code role} is either "user" or "assistant". */
record AiMessage(String role, List<AiContent> content) {

    static AiMessage user(String text) {
        return new AiMessage("user", List.of(new AiContent.Text(text)));
    }

    static AiMessage assistant(List<AiContent> content) {
        return new AiMessage("assistant", content);
    }

    static AiMessage toolResults(List<AiContent> results) {
        // Tool results are sent back as a user turn, per the Messages API.
        return new AiMessage("user", results);
    }
}
