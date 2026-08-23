package com.aether.finances.internal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Partial update: a null field is left unchanged. */
record UpdateTransactionRequest(
        TransactionType type,
        @Positive Long amountCents,
        @Size(max = 100) String category,
        @Size(max = 500) String description,
        LocalDate date) {}
