package com.aether.notes.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
class NoteController {

    private final NoteService noteService;
    private final AuthApi authApi;

    NoteController(NoteService noteService, AuthApi authApi) {
        this.noteService = noteService;
        this.authApi = authApi;
    }

    @GetMapping
    List<NoteSummary> list() {
        return noteService.list(authApi.currentUserId());
    }

    @GetMapping("/{id}")
    NoteResponse get(@PathVariable UUID id) {
        return noteService.get(authApi.currentUserId(), id);
    }

    @PostMapping
    ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request) {
        NoteResponse note = noteService.create(
                authApi.currentUserId(), request.title(), request.content());
        return ResponseEntity.created(URI.create("/api/notes/" + note.id())).body(note);
    }

    @PatchMapping("/{id}")
    NoteResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
        return noteService.update(
                authApi.currentUserId(), id, request.title(), request.content());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        noteService.delete(authApi.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
