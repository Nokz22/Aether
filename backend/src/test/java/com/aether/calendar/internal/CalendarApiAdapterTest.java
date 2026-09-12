package com.aether.calendar.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aether.calendar.EventView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarApiAdapterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Instant FROM = Instant.parse("2026-09-12T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-09-13T00:00:00Z");

    @Mock
    private EventService events;

    @Test
    void eventsMapTitleDescriptionAndInstants() {
        UUID id = UUID.randomUUID();
        Instant start = Instant.parse("2026-09-12T14:00:00Z");
        Instant end = Instant.parse("2026-09-12T15:00:00Z");
        when(events.inRange(USER, FROM, TO)).thenReturn(List.of(
                new EventResponse(id, "Dentista", "consulta", start, end, FROM, FROM)));

        EventView view = new CalendarApiAdapter(events).events(USER, FROM, TO).getFirst();

        assertThat(view).isEqualTo(new EventView(id, "Dentista", "consulta", start, end));
    }

    @Test
    void createEventReturnsTheStoredEvent() {
        UUID id = UUID.randomUUID();
        Instant start = Instant.parse("2026-09-12T09:00:00Z");
        Instant end = Instant.parse("2026-09-12T10:00:00Z");
        when(events.create(USER, "Reunião", "", start, end))
                .thenReturn(new EventResponse(id, "Reunião", "", start, end, FROM, FROM));

        assertThat(new CalendarApiAdapter(events).createEvent(USER, "Reunião", "", start, end))
                .isEqualTo(new EventView(id, "Reunião", "", start, end));
    }
}
