package com.aether.projects.internal;

import jakarta.validation.constraints.Size;

/** Partial update: a null field is left unchanged. */
record UpdateProjectRequest(
        @Size(max = 200) String name,
        @Size(max = 2000) String description) {}
