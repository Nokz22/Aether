package com.aether.habits.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HabitCheckinRepository extends JpaRepository<HabitCheckin, HabitCheckinId> {

    List<HabitCheckin> findAllByIdHabitId(UUID habitId);
}
