package com.aether.finances.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
class Transaction {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Always a positive minor-unit amount (cents); the type carries the sign. */
    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
        // JPA only
    }

    Transaction(UUID userId, TransactionType type, long amountCents,
                String category, String description, LocalDate date) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.amountCents = amountCents;
        this.category = category;
        this.description = description;
        this.date = date;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Applies a partial update; null fields are left unchanged. */
    void update(TransactionType type, Long amountCents, String category,
                String description, LocalDate date) {
        if (type != null) {
            this.type = type;
        }
        if (amountCents != null) {
            this.amountCents = amountCents;
        }
        if (category != null) {
            this.category = category;
        }
        if (description != null) {
            this.description = description;
        }
        if (date != null) {
            this.date = date;
        }
        this.updatedAt = Instant.now();
    }

    UUID id() {
        return id;
    }

    TransactionType type() {
        return type;
    }

    long amountCents() {
        return amountCents;
    }

    String category() {
        return category;
    }

    String description() {
        return description;
    }

    LocalDate date() {
        return date;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
