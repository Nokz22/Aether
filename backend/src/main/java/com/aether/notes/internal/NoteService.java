package com.aether.notes.internal;

import com.aether.shared.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class NoteService {

    private static final int PREVIEW_MAX = 140;

    private final NoteRepository notes;

    NoteService(NoteRepository notes) {
        this.notes = notes;
    }

    @Transactional(readOnly = true)
    List<NoteSummary> list(UUID userId) {
        return notes.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(note -> new NoteSummary(note.id(), note.title(),
                        preview(note.content()), note.updatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    NoteResponse get(UUID userId, UUID noteId) {
        return NoteResponse.from(owned(userId, noteId));
    }

    @Transactional
    NoteResponse create(UUID userId, String title, String content) {
        Note note = new Note(userId, normalize(title), content);
        notes.save(note);
        return NoteResponse.from(note);
    }

    @Transactional
    NoteResponse update(UUID userId, UUID noteId, String title, String content) {
        Note note = owned(userId, noteId);
        note.update(title == null ? null : normalize(title), content);
        return NoteResponse.from(note);
    }

    @Transactional
    void delete(UUID userId, UUID noteId) {
        notes.delete(owned(userId, noteId));
    }

    // Ownership failures surface as 404, never revealing another user's note exists.
    private Note owned(UUID userId, UUID noteId) {
        return notes.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "note_not_found",
                        "Note not found."));
    }

    private static String normalize(String title) {
        return title == null ? "" : title.trim();
    }

    // First non-blank line, trimmed and capped — a label for titleless notes.
    private static String preview(String content) {
        return content.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .map(line -> line.length() > PREVIEW_MAX ? line.substring(0, PREVIEW_MAX) : line)
                .orElse("");
    }
}
