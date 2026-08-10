package com.aether.projects.internal;

import java.time.Instant;
import java.util.UUID;

record TaskResponse(UUID id, String title, TaskStatus status, Instant createdAt, Instant updatedAt) {

    static TaskResponse from(Task task) {
        return new TaskResponse(task.id(), task.title(), task.status(),
                task.createdAt(), task.updatedAt());
    }
}
