package com.aether.habits.internal;

import com.aether.shared.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class HabitService {

    private final HabitRepository habits;
    private final HabitCheckinRepository checkins;

    HabitService(HabitRepository habits, HabitCheckinRepository checkins) {
        this.habits = habits;
        this.checkins = checkins;
    }

    @Transactional(readOnly = true)
    List<HabitResponse> list(UUID userId, LocalDate today) {
        return habits.findAllByUserIdOrderByCreatedAt(userId).stream()
                .map(habit -> toResponse(habit, today))
                .toList();
    }

    @Transactional
    HabitSummary create(UUID userId, String name) {
        Habit habit = new Habit(userId, name.trim());
        habits.save(habit);
        return new HabitSummary(habit.id(), habit.name());
    }

    @Transactional
    void rename(UUID userId, UUID habitId, String name) {
        owned(userId, habitId).rename(name.trim());
    }

    @Transactional
    void delete(UUID userId, UUID habitId) {
        habits.delete(owned(userId, habitId));
    }

    @Transactional
    void checkIn(UUID userId, UUID habitId, LocalDate date, LocalDate today) {
        owned(userId, habitId);
        if (date.isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "future_checkin",
                    "A habit cannot be checked in for a future day.");
        }
        HabitCheckinId id = new HabitCheckinId(habitId, date);
        if (!checkins.existsById(id)) {
            checkins.save(new HabitCheckin(habitId, date));
        }
    }

    @Transactional
    void removeCheckin(UUID userId, UUID habitId, LocalDate date) {
        owned(userId, habitId);
        checkins.deleteById(new HabitCheckinId(habitId, date));
    }

    // Ownership failures surface as 404, not 403: the API must not reveal
    // whether someone else's habit id exists.
    private Habit owned(UUID userId, UUID habitId) {
        return habits.findByIdAndUserId(habitId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "habit_not_found",
                        "Habit not found."));
    }

    private HabitResponse toResponse(Habit habit, LocalDate today) {
        Set<LocalDate> done = checkins.findAllByIdHabitId(habit.id()).stream()
                .map(HabitCheckin::date)
                .collect(Collectors.toSet());
        List<HabitDay> week = IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> today.minusDays(6 - offset))
                .map(date -> new HabitDay(date, done.contains(date)))
                .toList();
        return new HabitResponse(habit.id(), habit.name(), done.contains(today),
                currentStreak(done, today), week);
    }

    // Consecutive done days ending today — or yesterday while today is still open,
    // so an unfinished day never reads as a broken streak.
    private static int currentStreak(Set<LocalDate> done, LocalDate today) {
        LocalDate cursor = done.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (done.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
