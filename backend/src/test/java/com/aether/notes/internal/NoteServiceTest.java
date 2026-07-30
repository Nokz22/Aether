package com.aether.notes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock
    private NoteRepository notes;

    private NoteService service() {
        return new NoteService(notes);
    }

    @Test
    void createTrimsTitleAndReturnsTheStoredNote() {
        NoteResponse response = service().create(USER, "  Groceries  ", "milk\nbread");

        assertThat(response.title()).isEqualTo("Groceries");
        assertThat(response.content()).isEqualTo("milk\nbread");
        verify(notes).save(any(Note.class));
    }

    @Test
    void listBuildsPreviewFromTheFirstNonBlankLine() {
        Note note = new Note(USER, "", "\n   \nFirst real line\nsecond");
        when(notes.findAllByUserIdOrderByUpdatedAtDesc(USER)).thenReturn(List.of(note));

        NoteSummary summary = service().list(USER).getFirst();

        assertThat(summary.title()).isEmpty();
        assertThat(summary.preview()).isEqualTo("First real line");
    }

    @Test
    void previewIsCappedInLength() {
        String longLine = "x".repeat(300);
        Note note = new Note(USER, "", longLine);
        when(notes.findAllByUserIdOrderByUpdatedAtDesc(USER)).thenReturn(List.of(note));

        assertThat(service().list(USER).getFirst().preview()).hasSize(140);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Note note = new Note(USER, "Old title", "Old body");
        when(notes.findByIdAndUserId(note.id(), USER)).thenReturn(Optional.of(note));

        NoteResponse response = service().update(USER, note.id(), null, "New body");

        assertThat(response.title()).isEqualTo("Old title");
        assertThat(response.content()).isEqualTo("New body");
    }

    @Test
    void updateTouchesUpdatedAt() {
        Note note = new Note(USER, "T", "B");
        when(notes.findByIdAndUserId(note.id(), USER)).thenReturn(Optional.of(note));

        NoteResponse response = service().update(USER, note.id(), "T2", null);

        assertThat(response.updatedAt()).isAfterOrEqualTo(response.createdAt());
        assertThat(response.title()).isEqualTo("T2");
    }

    @Test
    void readingSomeoneElsesNoteIsNotFound() {
        UUID foreignNote = UUID.randomUUID();
        when(notes.findByIdAndUserId(foreignNote, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(USER, foreignNote))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("note_not_found"));
    }

    @Test
    void deletingSomeoneElsesNoteIsNotFoundAndDeletesNothing() {
        UUID foreignNote = UUID.randomUUID();
        when(notes.findByIdAndUserId(foreignNote, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(USER, foreignNote))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("note_not_found"));
        verify(notes, never()).delete(any());
    }

    @Test
    void createStoresAnEmptyTitleWhenNoneGiven() {
        service().create(USER, null, "just a body");

        ArgumentCaptor<Note> saved = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(saved.capture());
        assertThat(saved.getValue().title()).isEmpty();
    }
}
