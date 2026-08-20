package com.aether.calendar.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventRepository extends JpaRepository<Event, UUID> {

    // Half-open overlap with [from, to): starts before the window ends and ends after it starts.
    @Query("""
            select e from Event e
            where e.userId = :userId and e.startsAt < :to and e.endsAt > :from
            order by e.startsAt
            """)
    List<Event> findOverlapping(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    Optional<Event> findByIdAndUserId(UUID id, UUID userId);
}
