package com.aether.notes.internal;

import com.aether.notes.NoteContentView;
import com.aether.notes.NoteView;
import com.aether.notes.NotesApi;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class NotesApiAdapter implements NotesApi {

    private final NoteService notes;

    NotesApiAdapter(NoteService notes) {
        this.notes = notes;
    }

    @Override
    public List<NoteView> recentNotes(UUID userId, int limit) {
        return notes.list(userId).stream()
                .limit(Math.max(limit, 0))
                .map(note -> new NoteView(note.id(), note.title(), note.preview(), note.updatedAt()))
                .toList();
    }

    @Override
    public NoteContentView note(UUID userId, UUID noteId) {
        NoteResponse note = notes.get(userId, noteId);
        return new NoteContentView(note.id(), note.title(), note.content(), note.updatedAt());
    }

    @Override
    public NoteView createNote(UUID userId, String title, String content) {
        NoteResponse created = notes.create(userId, title, content);
        return new NoteView(created.id(), created.title(), NotePreview.of(created.content()),
                created.updatedAt());
    }
}
