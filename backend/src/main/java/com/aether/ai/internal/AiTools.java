package com.aether.ai.internal;

import com.aether.calendar.CalendarApi;
import com.aether.finances.EntryType;
import com.aether.finances.FinancesApi;
import com.aether.habits.HabitsApi;
import com.aether.notes.NotesApi;
import com.aether.projects.ProjectsApi;
import com.aether.projects.TaskState;
import com.aether.shared.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The tools the assistant may use, and how each one is carried out.
 *
 * <p>Every call goes through a module's public API (ADR-002) — never its
 * internals. There is deliberately no tool that deletes anything: the
 * assistant can create and update, never destroy.
 */
@Component
class AiTools {

    private final HabitsApi habits;
    private final NotesApi notes;
    private final ProjectsApi projects;
    private final CalendarApi calendar;
    private final FinancesApi finances;
    private final ObjectMapper objectMapper;

    AiTools(HabitsApi habits, NotesApi notes, ProjectsApi projects, CalendarApi calendar,
            FinancesApi finances, ObjectMapper objectMapper) {
        this.habits = habits;
        this.notes = notes;
        this.projects = projects;
        this.calendar = calendar;
        this.finances = finances;
        this.objectMapper = objectMapper;
    }

    List<ToolSpec> specs() {
        return List.of(
                new ToolSpec("list_today_habits",
                        "The user's habits with whether each is done today and its current streak.",
                        object(Map.of(), List.of())),
                new ToolSpec("list_events",
                        "Calendar events overlapping a time window. Times are ISO-8601 instants.",
                        object(Map.of(
                                "from", property("string", "Window start, ISO-8601 instant."),
                                "to", property("string", "Window end, ISO-8601 instant.")),
                                List.of("from", "to"))),
                new ToolSpec("finance_summary",
                        "Income, expense, net and per-category totals for a date range.",
                        object(Map.of(
                                "from", property("string", "First day, YYYY-MM-DD."),
                                "to", property("string", "Last day, YYYY-MM-DD.")),
                                List.of("from", "to"))),
                new ToolSpec("list_transactions",
                        "The user's transactions in a date range, newest first.",
                        object(Map.of(
                                "from", property("string", "First day, YYYY-MM-DD."),
                                "to", property("string", "Last day, YYYY-MM-DD.")),
                                List.of("from", "to"))),
                new ToolSpec("list_notes",
                        "The user's notes, most recently edited first, with a short preview.",
                        object(Map.of("limit", property("integer", "How many to return, at most 50.")),
                                List.of())),
                new ToolSpec("read_note",
                        "The full body of one note.",
                        object(Map.of("note_id", property("string", "The note's id.")),
                                List.of("note_id"))),
                new ToolSpec("list_projects",
                        "The user's projects with how many tasks sit in each state.",
                        object(Map.of(), List.of())),
                new ToolSpec("read_project",
                        "One project with its tasks and their states.",
                        object(Map.of("project_id", property("string", "The project's id.")),
                                List.of("project_id"))),

                new ToolSpec("create_habit",
                        "Create a daily habit.",
                        object(Map.of("name", property("string", "The habit's name.")),
                                List.of("name"))),
                new ToolSpec("check_in_habit",
                        "Mark a habit as done on a day. Defaults to today. Never a future day.",
                        object(Map.of(
                                "habit_id", property("string", "The habit's id."),
                                "date", property("string", "Day to mark, YYYY-MM-DD. Defaults to today.")),
                                List.of("habit_id"))),
                new ToolSpec("create_note",
                        "Create a note. The title may be empty.",
                        object(Map.of(
                                "title", property("string", "Short title, may be empty."),
                                "content", property("string", "The note body.")),
                                List.of("content"))),
                new ToolSpec("create_project",
                        "Create a project.",
                        object(Map.of("name", property("string", "The project's name.")),
                                List.of("name"))),
                new ToolSpec("add_task",
                        "Add a task to a project. It starts in TODO.",
                        object(Map.of(
                                "project_id", property("string", "The project's id."),
                                "title", property("string", "The task's title.")),
                                List.of("project_id", "title"))),
                new ToolSpec("move_task",
                        "Move a task to another state.",
                        object(Map.of(
                                "project_id", property("string", "The project's id."),
                                "task_id", property("string", "The task's id."),
                                "state", enumProperty("TODO, DOING or DONE.",
                                        List.of("TODO", "DOING", "DONE"))),
                                List.of("project_id", "task_id", "state"))),
                new ToolSpec("create_event",
                        "Create a calendar event. Times are ISO-8601 instants; end must be after start.",
                        object(Map.of(
                                "title", property("string", "The event's title."),
                                "description", property("string", "Optional detail."),
                                "starts_at", property("string", "Start, ISO-8601 instant."),
                                "ends_at", property("string", "End, ISO-8601 instant.")),
                                List.of("title", "starts_at", "ends_at"))),
                new ToolSpec("add_transaction",
                        "Record income or an expense. Amounts are integer cents, always positive.",
                        object(Map.of(
                                "type", enumProperty("INCOME or EXPENSE.", List.of("INCOME", "EXPENSE")),
                                "amount_cents", property("integer", "Amount in cents, greater than zero."),
                                "category", property("string", "Optional category, e.g. Food."),
                                "description", property("string", "Optional description."),
                                "date", property("string", "Day, YYYY-MM-DD. Defaults to today.")),
                                List.of("type", "amount_cents"))));
    }

    ToolOutcome run(UUID userId, String tool, Map<String, Object> input, ChatContext context) {
        try {
            return ToolOutcome.ok(json(dispatch(userId, tool, input, context)));
        } catch (ApiException e) {
            // The code is safe to show the model; it is a stable identifier, not data.
            return ToolOutcome.failed(e.code());
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ToolOutcome.failed("invalid_tool_input");
        }
    }

    private Object dispatch(UUID userId, String tool, Map<String, Object> input,
                            ChatContext context) {
        return switch (tool) {
            case "list_today_habits" -> habits.todayHabits(userId, context.today());
            case "list_events" -> calendar.events(userId,
                    instant(input, "from"), instant(input, "to"));
            case "finance_summary" -> finances.summary(userId,
                    date(input, "from"), date(input, "to"));
            case "list_transactions" -> finances.transactions(userId,
                    date(input, "from"), date(input, "to"));
            case "list_notes" -> notes.recentNotes(userId,
                    Math.min(optionalInt(input, "limit", 10), 50));
            case "read_note" -> notes.note(userId, uuid(input, "note_id"));
            case "list_projects" -> projects.projects(userId);
            case "read_project" -> projects.project(userId, uuid(input, "project_id"));

            case "create_habit" -> habits.createHabit(userId, text(input, "name"));
            case "check_in_habit" -> {
                LocalDate day = optionalDate(input, "date", context.today());
                habits.checkIn(userId, uuid(input, "habit_id"), day, context.today());
                yield Map.of("checkedIn", day.toString());
            }
            case "create_note" -> notes.createNote(userId,
                    optionalText(input, "title", ""), text(input, "content"));
            case "create_project" -> projects.createProject(userId, text(input, "name"));
            case "add_task" -> projects.addTask(userId,
                    uuid(input, "project_id"), text(input, "title"));
            case "move_task" -> projects.moveTask(userId,
                    uuid(input, "project_id"), uuid(input, "task_id"),
                    TaskState.valueOf(text(input, "state")));
            case "create_event" -> calendar.createEvent(userId,
                    text(input, "title"), optionalText(input, "description", ""),
                    instant(input, "starts_at"), instant(input, "ends_at"));
            case "add_transaction" -> finances.addTransaction(userId,
                    EntryType.valueOf(text(input, "type")),
                    longValue(input, "amount_cents"),
                    optionalText(input, "category", ""),
                    optionalText(input, "description", ""),
                    optionalDate(input, "date", context.today()));

            default -> throw new IllegalArgumentException("unknown tool");
        };
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("unserializable tool result", e);
        }
    }

    // ── input helpers ───────────────────────────────────────────────────────

    private static String text(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value.toString();
    }

    private static String optionalText(Map<String, Object> input, String key, String fallback) {
        Object value = input.get(key);
        return value == null ? fallback : value.toString();
    }

    private static UUID uuid(Map<String, Object> input, String key) {
        return UUID.fromString(text(input, key));
    }

    private static LocalDate date(Map<String, Object> input, String key) {
        return LocalDate.parse(text(input, key));
    }

    private static LocalDate optionalDate(Map<String, Object> input, String key,
                                          LocalDate fallback) {
        Object value = input.get(key);
        return value == null ? fallback : LocalDate.parse(value.toString());
    }

    private static Instant instant(Map<String, Object> input, String key) {
        return Instant.parse(text(input, key));
    }

    private static int optionalInt(Map<String, Object> input, String key, int fallback) {
        Object value = input.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static long longValue(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(text(input, key));
    }

    // ── schema helpers ──────────────────────────────────────────────────────

    private static Map<String, Object> object(Map<String, Object> properties,
                                              List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required);
    }

    private static Map<String, Object> property(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> enumProperty(String description, List<String> values) {
        return Map.of("type", "string", "description", description, "enum", values);
    }
}
