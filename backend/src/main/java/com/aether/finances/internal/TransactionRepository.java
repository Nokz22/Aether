package com.aether.finances.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.date >= :from and t.date <= :to
            order by t.date desc, t.createdAt desc
            """)
    List<Transaction> findInRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
}
