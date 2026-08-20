package com.aether.calendar.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
class EventController {

    private final EventService eventService;
    private final AuthApi authApi;

    EventController(EventService eventService, AuthApi authApi) {
        this.eventService = eventService;
        this.authApi = authApi;
    }

    @GetMapping
    List<EventResponse> inRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return eventService.inRange(authApi.currentUserId(), from, to);
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.create(authApi.currentUserId(),
                request.title(), request.description(), request.startsAt(), request.endsAt());
        return ResponseEntity.created(URI.create("/api/events/" + event.id())).body(event);
    }

    @PatchMapping("/{id}")
    EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.update(authApi.currentUserId(), id,
                request.title(), request.description(), request.startsAt(), request.endsAt());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.delete(authApi.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
