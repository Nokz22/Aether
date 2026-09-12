package com.aether.ai.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ChatMessageResponse(
        UUID id,
        ChatRole role,
        String content,
        List<String> toolsUsed,
        Instant createdAt) {

    static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.id(), message.role(), message.content(),
                message.toolsUsed(), message.createdAt());
    }
}
