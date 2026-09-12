package com.aether.habits;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API of the habits module (ADR-002). Other modules read and change
 * habits only through here, never through the module's internals.
 *
 * <p>There is deliberately no delete: the AI layer must not be able to remove
 * a user's data.
 */
public interface HabitsApi {

    /** The user's habits with today's state and current streak. */
    List<HabitView> todayHabits(UUID userId, LocalDate today);

    HabitView createHabit(UUID userId, String name);

    /** Marks the habit done on {@code date}; already-marked days are a no-op. */
    void checkIn(UUID userId, UUID habitId, LocalDate date, LocalDate today);
}
