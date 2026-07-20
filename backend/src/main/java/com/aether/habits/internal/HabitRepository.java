package com.aether.habits.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HabitRepository extends JpaRepository<Habit, UUID> {

    List<Habit> findAllByUserIdOrderByCreatedAt(UUID userId);

    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);
}
