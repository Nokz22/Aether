package com.aether.calendar.internal;

import com.aether.calendar.CalendarApi;
import com.aether.calendar.EventView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CalendarApiAdapter implements CalendarApi {

    private final EventService events;

    CalendarApiAdapter(EventService events) {
        this.events = events;
    }

    @Override
    public List<EventView> events(UUID userId, Instant from, Instant to) {
        return events.inRange(userId, from, to).stream()
                .map(CalendarApiAdapter::toView)
                .toList();
    }

    @Override
    public EventView createEvent(UUID userId, String title, String description,
                                 Instant startsAt, Instant endsAt) {
        return toView(events.create(userId, title, description, startsAt, endsAt));
    }

    private static EventView toView(EventResponse event) {
        return new EventView(event.id(), event.title(), event.description(),
                event.startsAt(), event.endsAt());
    }
}
