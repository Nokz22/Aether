package com.aether.projects.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ProjectResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<TaskResponse> tasks) {}
