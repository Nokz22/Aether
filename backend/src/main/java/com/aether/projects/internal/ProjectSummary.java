package com.aether.projects.internal;

import java.time.Instant;
import java.util.UUID;

record ProjectSummary(UUID id, String name, TaskCounts taskCounts, Instant updatedAt) {}
