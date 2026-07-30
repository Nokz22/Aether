package com.aether.notes.internal;

import java.time.Instant;
import java.util.UUID;

record NoteResponse(UUID id, String title, String content, Instant createdAt, Instant updatedAt) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.id(), note.title(), note.content(),
                note.createdAt(), note.updatedAt());
    }
}
