package com.aether.notes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotesApiAdapterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @Mock
    private NoteService notes;

    @Test
    void recentNotesHonoursTheLimit() {
        when(notes.list(USER)).thenReturn(List.of(
                new NoteSummary(UUID.randomUUID(), "A", "a", NOW),
                new NoteSummary(UUID.randomUUID(), "B", "b", NOW),
                new NoteSummary(UUID.randomUUID(), "C", "c", NOW)));

        assertThat(new NotesApiAdapter(notes).recentNotes(USER, 2)).hasSize(2);
    }

    @Test
    void createdNoteGetsThePreviewFromItsFirstRealLine() {
        UUID id = UUID.randomUUID();
        when(notes.create(USER, "T", "\n  \nprimeira linha\nsegunda"))
                .thenReturn(new NoteResponse(id, "T", "\n  \nprimeira linha\nsegunda", NOW, NOW));

        assertThat(new NotesApiAdapter(notes).createNote(USER, "T", "\n  \nprimeira linha\nsegunda")
                .preview()).isEqualTo("primeira linha");
    }

    @Test
    void noteReturnsTheFullBody() {
        UUID id = UUID.randomUUID();
        when(notes.get(USER, id)).thenReturn(new NoteResponse(id, "T", "corpo inteiro", NOW, NOW));

        assertThat(new NotesApiAdapter(notes).note(USER, id).content()).isEqualTo("corpo inteiro");
    }
}
