package com.aether.projects.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateTaskRequest(@NotBlank @Size(max = 300) String title) {}
