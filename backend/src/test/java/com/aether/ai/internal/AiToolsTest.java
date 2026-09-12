package com.aether.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aether.calendar.CalendarApi;
import com.aether.calendar.EventView;
import com.aether.finances.EntryType;
import com.aether.finances.EntryView;
import com.aether.finances.FinancesApi;
import com.aether.habits.HabitView;
import com.aether.habits.HabitsApi;
import com.aether.notes.NotesApi;
import com.aether.notes.NoteView;
import com.aether.projects.ProjectsApi;
import com.aether.projects.TaskState;
import com.aether.projects.TaskView;
import com.aether.shared.ApiException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AiToolsTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 12);
    private static final ChatContext CONTEXT = new ChatContext(TODAY, ZoneOffset.ofHours(1));

    @Mock
    private HabitsApi habits;

    @Mock
    private NotesApi notes;

    @Mock
    private ProjectsApi projects;

    @Mock
    private CalendarApi calendar;

    @Mock
    private FinancesApi finances;

    private AiTools tools() {
        return new AiTools(habits, notes, projects, calendar, finances,
                JsonMapper.builder().addModule(new JavaTimeModule()).build());
    }

    @Test
    void everyToolHasANameAndAnInputSchemaAndNoneDeletes() {
        List<ToolSpec> specs = tools().specs();

        assertThat(specs).allSatisfy(spec -> {
            assertThat(spec.name()).isNotBlank();
            assertThat(spec.description()).isNotBlank();
            assertThat(spec.inputSchema()).containsEntry("type", "object");
        });
        assertThat(specs).extracting(ToolSpec::name).doesNotHaveDuplicates();
        // The assistant can create and update, never destroy.
        assertThat(specs).extracting(ToolSpec::name)
                .noneMatch(name -> name.contains("delete") || name.contains("remove"));
    }

    @Test
    void readsHabitsForTheCallersOwnToday() {
        when(habits.todayHabits(USER, TODAY))
                .thenReturn(List.of(new HabitView(UUID.randomUUID(), "Ler", false, 3)));

        ToolOutcome outcome = tools().run(USER, "list_today_habits", Map.of(), CONTEXT);

        assertThat(outcome.isError()).isFalse();
        assertThat(outcome.content()).contains("\"name\":\"Ler\"").contains("\"currentStreak\":3");
    }

    @Test
    void checkInDefaultsToTodayAndNeverPassesAFutureDay() {
        UUID habitId = UUID.randomUUID();

        ToolOutcome outcome = tools().run(USER, "check_in_habit",
                Map.of("habit_id", habitId.toString()), CONTEXT);

        verify(habits).checkIn(USER, habitId, TODAY, TODAY);
        assertThat(outcome.content()).contains("2026-09-12");
    }

    @Test
    void createsAnEventFromInstants() {
        Instant start = Instant.parse("2026-09-13T13:00:00Z");
        Instant end = Instant.parse("2026-09-13T14:00:00Z");
        when(calendar.createEvent(USER, "Dentista", "", start, end))
                .thenReturn(new EventView(UUID.randomUUID(), "Dentista", "", start, end));

        ToolOutcome outcome = tools().run(USER, "create_event", Map.of(
                "title", "Dentista",
                "starts_at", start.toString(),
                "ends_at", end.toString()), CONTEXT);

        assertThat(outcome.isError()).isFalse();
        assertThat(outcome.content()).contains("Dentista");
    }

    @Test
    void recordsMoneyInCentsWithTheTypeCarryingTheSign() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        when(finances.addTransaction(USER, EntryType.EXPENSE, 1_299, "Comida", "almoço", date))
                .thenReturn(new EntryView(UUID.randomUUID(), EntryType.EXPENSE, 1_299,
                        "Comida", "almoço", date));

        ToolOutcome outcome = tools().run(USER, "add_transaction", Map.of(
                "type", "EXPENSE",
                "amount_cents", 1_299,
                "category", "Comida",
                "description", "almoço",
                "date", "2026-09-10"), CONTEXT);

        assertThat(outcome.isError()).isFalse();
        assertThat(outcome.content()).contains("\"amountCents\":1299");
    }

    @Test
    void movesATaskByItsPublicState() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(projects.moveTask(USER, projectId, taskId, TaskState.DONE))
                .thenReturn(new TaskView(taskId, "Escrever", TaskState.DONE));

        ToolOutcome outcome = tools().run(USER, "move_task", Map.of(
                "project_id", projectId.toString(),
                "task_id", taskId.toString(),
                "state", "DONE"), CONTEXT);

        assertThat(outcome.isError()).isFalse();
        assertThat(outcome.content()).contains("DONE");
    }

    @Test
    void capsHowManyNotesTheModelCanPullAtOnce() {
        when(notes.recentNotes(USER, 50)).thenReturn(List.of(
                new NoteView(UUID.randomUUID(), "Ideias", "primeira linha",
                        Instant.parse("2026-09-11T08:00:00Z"))));

        tools().run(USER, "list_notes", Map.of("limit", 500), CONTEXT);

        verify(notes).recentNotes(USER, 50);
    }

    @Test
    void aDomainFailureComesBackAsItsStableCode() {
        UUID noteId = UUID.randomUUID();
        when(notes.note(USER, noteId)).thenThrow(
                new ApiException(HttpStatus.NOT_FOUND, "note_not_found", "Note not found."));

        ToolOutcome outcome = tools().run(USER, "read_note",
                Map.of("note_id", noteId.toString()), CONTEXT);

        assertThat(outcome.isError()).isTrue();
        assertThat(outcome.content()).isEqualTo("note_not_found");
    }

    @Test
    void badInputIsRejectedWithoutTouchingAnyModule() {
        ToolOutcome outcome = tools().run(USER, "read_note",
                Map.of("note_id", "not-a-uuid"), CONTEXT);

        assertThat(outcome.isError()).isTrue();
        assertThat(outcome.content()).isEqualTo("invalid_tool_input");
        verifyNoInteractions(notes);
    }

    @Test
    void anUnknownToolIsRejected() {
        ToolOutcome outcome = tools().run(USER, "delete_everything", Map.of(), CONTEXT);

        assertThat(outcome.isError()).isTrue();
        assertThat(outcome.content()).isEqualTo("invalid_tool_input");
        verifyNoInteractions(habits, notes, projects, calendar, finances);
    }

    @Test
    void aMissingRequiredFieldIsRejected() {
        ToolOutcome outcome = tools().run(USER, "create_habit", Map.of("name", " "), CONTEXT);

        assertThat(outcome.isError()).isTrue();
        assertThat(outcome.content()).isEqualTo("invalid_tool_input");
        verifyNoInteractions(habits);
    }
}
