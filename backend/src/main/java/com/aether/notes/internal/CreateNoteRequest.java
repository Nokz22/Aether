package com.aether.notes.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateNoteRequest(
        @Size(max = 200) String title,
        @NotBlank @Size(max = 50_000) String content) {}
