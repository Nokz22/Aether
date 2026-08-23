package com.aether.finances.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

record CreateTransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Positive Long amountCents,
        @Size(max = 100) String category,
        @Size(max = 500) String description,
        @NotNull LocalDate date) {}
