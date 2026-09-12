package com.aether.notes;

import java.time.Instant;
import java.util.UUID;

/** A note in list form: enough to pick one without loading its body. */
public record NoteView(UUID id, String title, String preview, Instant updatedAt) {}
