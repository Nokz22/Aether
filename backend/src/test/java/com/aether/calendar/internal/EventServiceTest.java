package com.aether.calendar.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-08-20T14:00:00Z");
    private static final Instant END = START.plus(1, ChronoUnit.HOURS);

    @Mock
    private EventRepository events;

    private EventService service() {
        return new EventService(events);
    }

    @Test
    void createStoresAndReturnsTheEvent() {
        EventResponse event = service().create(USER, "  Dentist  ", null, START, END);

        assertThat(event.title()).isEqualTo("Dentist");
        assertThat(event.description()).isEmpty();
        assertThat(event.startsAt()).isEqualTo(START);
        assertThat(event.endsAt()).isEqualTo(END);
        verify(events).save(any(Event.class));
    }

    @Test
    void createRejectsAnEndBeforeStart() {
        assertThatThrownBy(() -> service().create(USER, "Bad", null, END, START))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_interval"));
        verify(events, never()).save(any());
    }

    @Test
    void rangeQueryRejectsAnEndBeforeOrEqualToStart() {
        assertThatThrownBy(() -> service().inRange(USER, END, START))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_range"));
    }

    @Test
    void rangeQueryDelegatesToTheOverlapQuery() {
        Instant to = END.plus(1, ChronoUnit.HOURS);
        when(events.findOverlapping(USER, START, to)).thenReturn(List.of());

        service().inRange(USER, START, to);

        verify(events).findOverlapping(eq(USER), eq(START), eq(to));
    }

    @Test
    void updateValidatesTheResultingIntervalAcrossPartialFields() {
        Event event = new Event(USER, "Meeting", "", START, END);
        when(events.findByIdAndUserId(event.id(), USER)).thenReturn(Optional.of(event));

        // Moving only the start past the existing end is rejected.
        assertThatThrownBy(() ->
                service().update(USER, event.id(), null, null, END.plus(1, ChronoUnit.HOURS), null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_interval"));
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Event event = new Event(USER, "Meeting", "old", START, END);
        when(events.findByIdAndUserId(event.id(), USER)).thenReturn(Optional.of(event));

        EventResponse updated = service().update(USER, event.id(), "Sync", null, null, null);

        assertThat(updated.title()).isEqualTo("Sync");
        assertThat(updated.description()).isEqualTo("old");
        assertThat(updated.startsAt()).isEqualTo(START);
    }

    @Test
    void someoneElsesEventIsNotFound() {
        UUID foreign = UUID.randomUUID();
        when(events.findByIdAndUserId(foreign, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(USER, foreign))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("event_not_found"));
    }
}
