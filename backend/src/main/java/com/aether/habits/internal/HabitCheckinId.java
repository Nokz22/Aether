package com.aether.habits.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class HabitCheckinId implements Serializable {

    @Column(name = "habit_id")
    private UUID habitId;

    @Column(name = "date")
    private LocalDate date;

    protected HabitCheckinId() {
        // JPA only
    }

    HabitCheckinId(UUID habitId, LocalDate date) {
        this.habitId = habitId;
        this.date = date;
    }

    LocalDate date() {
        return date;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HabitCheckinId that
                && Objects.equals(habitId, that.habitId)
                && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(habitId, date);
    }
}
