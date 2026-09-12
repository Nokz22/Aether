package com.aether.ai.internal;

/** The result of running one tool, fed back to the model. */
record ToolOutcome(String content, boolean isError) {

    static ToolOutcome ok(String content) {
        return new ToolOutcome(content, false);
    }

    static ToolOutcome failed(String reason) {
        return new ToolOutcome(reason, true);
    }
}
