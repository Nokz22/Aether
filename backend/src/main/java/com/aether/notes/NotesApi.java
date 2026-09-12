package com.aether.notes;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the notes module (ADR-002). Deliberately no delete: the AI
 * layer must not be able to remove a user's notes.
 */
public interface NotesApi {

    /** The user's notes, most recently edited first, capped at {@code limit}. */
    List<NoteView> recentNotes(UUID userId, int limit);

    NoteContentView note(UUID userId, UUID noteId);

    NoteView createNote(UUID userId, String title, String content);
}
