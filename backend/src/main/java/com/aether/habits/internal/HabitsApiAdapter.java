package com.aether.habits.internal;

import com.aether.habits.HabitView;
import com.aether.habits.HabitsApi;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class HabitsApiAdapter implements HabitsApi {

    private final HabitService habits;

    HabitsApiAdapter(HabitService habits) {
        this.habits = habits;
    }

    @Override
    public List<HabitView> todayHabits(UUID userId, LocalDate today) {
        return habits.list(userId, today).stream()
                .map(habit -> new HabitView(habit.id(), habit.name(),
                        habit.doneToday(), habit.currentStreak()))
                .toList();
    }

    @Override
    public HabitView createHabit(UUID userId, String name) {
        HabitSummary created = habits.create(userId, name);
        return new HabitView(created.id(), created.name(), false, 0);
    }

    @Override
    public void checkIn(UUID userId, UUID habitId, LocalDate date, LocalDate today) {
        habits.checkIn(userId, habitId, date, today);
    }
}
