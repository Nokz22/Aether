package com.aether.projects.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
class Project {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // JPA only
    }

    Project(UUID userId, String name, String description) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Partial update: a null field is left unchanged. */
    void update(String name, String description) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String description() {
        return description;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
