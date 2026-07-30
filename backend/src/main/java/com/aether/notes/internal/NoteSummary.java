package com.aether.notes.internal;

import java.time.Instant;
import java.util.UUID;

/** Row in the notes list: enough to render it without loading full content. */
record NoteSummary(UUID id, String title, String preview, Instant updatedAt) {}
