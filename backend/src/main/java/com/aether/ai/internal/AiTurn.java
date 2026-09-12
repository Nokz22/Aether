package com.aether.ai.internal;

import java.util.List;

/** What the model replied: its content blocks and why it stopped. */
record AiTurn(List<AiContent> content, String stopReason) {

    boolean wantsTools() {
        return content.stream().anyMatch(block -> block instanceof AiContent.ToolUse);
    }

    List<AiContent.ToolUse> toolUses() {
        return content.stream()
                .filter(AiContent.ToolUse.class::isInstance)
                .map(AiContent.ToolUse.class::cast)
                .toList();
    }

    String text() {
        return content.stream()
                .filter(AiContent.Text.class::isInstance)
                .map(block -> ((AiContent.Text) block).text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
