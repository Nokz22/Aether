package com.aether.ai.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(nullable = false)
    private String content;

    /** Tool names only — never their inputs or results. */
    @Column(name = "tools_used", nullable = false)
    private String toolsUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessage() {
        // JPA only
    }

    ChatMessage(UUID userId, ChatRole role, String content, List<String> toolsUsed) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.toolsUsed = String.join(",", toolsUsed);
        this.createdAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    ChatRole role() {
        return role;
    }

    String content() {
        return content;
    }

    List<String> toolsUsed() {
        return toolsUsed.isBlank() ? List.of() : Arrays.asList(toolsUsed.split(","));
    }

    Instant createdAt() {
        return createdAt;
    }
}
