package com.aether.finances.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record TransactionResponse(
        UUID id,
        TransactionType type,
        long amountCents,
        String category,
        String description,
        LocalDate date,
        Instant createdAt,
        Instant updatedAt) {

    static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.id(), transaction.type(),
                transaction.amountCents(), transaction.category(), transaction.description(),
                transaction.date(), transaction.createdAt(), transaction.updatedAt());
    }
}
