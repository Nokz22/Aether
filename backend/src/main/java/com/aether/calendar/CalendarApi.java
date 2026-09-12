package com.aether.calendar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API of the calendar module (ADR-002). Deliberately no delete: the AI
 * layer must not be able to remove a user's events.
 */
public interface CalendarApi {

    /** Events overlapping {@code [from, to)}, earliest first. */
    List<EventView> events(UUID userId, Instant from, Instant to);

    EventView createEvent(UUID userId, String title, String description,
                          Instant startsAt, Instant endsAt);
}
