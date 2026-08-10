package com.aether.projects.internal;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
class Task {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
        // JPA only
    }

    Task(UUID projectId, String title) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.title = title;
        this.status = TaskStatus.TODO;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Partial update: a null field is left unchanged. */
    void update(String title, TaskStatus status) {
        if (title != null) {
            this.title = title;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    UUID projectId() {
        return projectId;
    }

    String title() {
        return title;
    }

    TaskStatus status() {
        return status;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
