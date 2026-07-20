package com.aether.habits.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/habits")
class HabitController {

    private final HabitService habitService;
    private final AuthApi authApi;

    HabitController(HabitService habitService, AuthApi authApi) {
        this.habitService = habitService;
        this.authApi = authApi;
    }

    @GetMapping
    List<HabitResponse> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return habitService.list(authApi.currentUserId(), today);
    }

    @PostMapping
    ResponseEntity<HabitSummary> create(@Valid @RequestBody CreateHabitRequest request) {
        HabitSummary habit = habitService.create(authApi.currentUserId(), request.name());
        return ResponseEntity.created(URI.create("/api/habits/" + habit.id())).body(habit);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> rename(@PathVariable UUID id,
            @Valid @RequestBody RenameHabitRequest request) {
        habitService.rename(authApi.currentUserId(), id, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        habitService.delete(authApi.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/checkins/{date}")
    ResponseEntity<Void> checkIn(@PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        habitService.checkIn(authApi.currentUserId(), id, date, today);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/checkins/{date}")
    ResponseEntity<Void> removeCheckin(@PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        habitService.removeCheckin(authApi.currentUserId(), id, date);
        return ResponseEntity.noContent().build();
    }
}
