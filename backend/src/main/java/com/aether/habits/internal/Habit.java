package com.aether.habits.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "habits")
class Habit {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Habit() {
        // JPA only
    }

    Habit(UUID userId, String name) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.createdAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    String name() {
        return name;
    }

    void rename(String name) {
        this.name = name;
    }
}
