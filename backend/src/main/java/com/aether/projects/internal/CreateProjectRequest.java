package com.aether.projects.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateProjectRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description) {}
