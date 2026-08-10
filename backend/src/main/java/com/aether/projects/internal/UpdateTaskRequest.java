package com.aether.projects.internal;

import jakarta.validation.constraints.Size;

/** Partial update: a null field is left unchanged. */
record UpdateTaskRequest(@Size(max = 300) String title, TaskStatus status) {}
