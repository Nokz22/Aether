package com.aether.finances;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API of the finances module (ADR-002). Deliberately no delete: the AI
 * layer must not be able to remove a user's transactions.
 */
public interface FinancesApi {

    /** Income, expense, net and per-category totals for {@code [from, to]}. */
    MoneySummaryView summary(UUID userId, LocalDate from, LocalDate to);

    List<EntryView> transactions(UUID userId, LocalDate from, LocalDate to);

    EntryView addTransaction(UUID userId, EntryType type, long amountCents,
                             String category, String description, LocalDate date);
}
