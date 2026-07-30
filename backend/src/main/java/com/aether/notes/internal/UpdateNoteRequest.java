package com.aether.notes.internal;

import jakarta.validation.constraints.Size;

/** Partial update: a null field is left unchanged. */
record UpdateNoteRequest(
        @Size(max = 200) String title,
        @Size(max = 50_000) String content) {}
