package com.aether.habits.internal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_checkins")
class HabitCheckin {

    @EmbeddedId
    private HabitCheckinId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HabitCheckin() {
        // JPA only
    }

    HabitCheckin(UUID habitId, LocalDate date) {
        this.id = new HabitCheckinId(habitId, date);
        this.createdAt = Instant.now();
    }

    LocalDate date() {
        return id.date();
    }
}
