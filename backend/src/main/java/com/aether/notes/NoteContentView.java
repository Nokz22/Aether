package com.aether.notes;

import java.time.Instant;
import java.util.UUID;

/** A note with its full body. */
public record NoteContentView(UUID id, String title, String content, Instant updatedAt) {}
