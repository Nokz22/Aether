package com.aether.calendar.internal;

import java.time.Instant;
import java.util.UUID;

record EventResponse(
        UUID id,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt) {

    static EventResponse from(Event event) {
        return new EventResponse(event.id(), event.title(), event.description(),
                event.startsAt(), event.endsAt(), event.createdAt(), event.updatedAt());
    }
}
