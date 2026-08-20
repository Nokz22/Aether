package com.aether.calendar.internal;

import com.aether.shared.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EventService {

    private final EventRepository events;

    EventService(EventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    List<EventResponse> inRange(UUID userId, Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_range",
                    "The range end must be after its start.");
        }
        return events.findOverlapping(userId, from, to).stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional
    EventResponse create(UUID userId, String title, String description,
                         Instant startsAt, Instant endsAt) {
        requireOrderedInterval(startsAt, endsAt);
        Event event = new Event(userId, title.trim(), normalize(description), startsAt, endsAt);
        events.save(event);
        return EventResponse.from(event);
    }

    @Transactional
    EventResponse update(UUID userId, UUID eventId, String title, String description,
                         Instant startsAt, Instant endsAt) {
        Event event = owned(userId, eventId);
        // Validate the interval the event will have after this partial update.
        Instant effectiveStart = startsAt != null ? startsAt : event.startsAt();
        Instant effectiveEnd = endsAt != null ? endsAt : event.endsAt();
        requireOrderedInterval(effectiveStart, effectiveEnd);
        event.update(title == null ? null : title.trim(),
                description == null ? null : description.trim(), startsAt, endsAt);
        return EventResponse.from(event);
    }

    @Transactional
    void delete(UUID userId, UUID eventId) {
        events.delete(owned(userId, eventId));
    }

    // Ownership failures surface as 404, never revealing another user's event exists.
    private Event owned(UUID userId, UUID eventId) {
        return events.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "event_not_found",
                        "Event not found."));
    }

    private static void requireOrderedInterval(Instant startsAt, Instant endsAt) {
        if (endsAt.isBefore(startsAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_interval",
                    "An event cannot end before it starts.");
        }
    }

    private static String normalize(String description) {
        return description == null ? "" : description.trim();
    }
}
