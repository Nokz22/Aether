package com.aether.finances.internal;

import java.util.List;

/** Totals for a date range: income, expense, net, and a per-category breakdown. */
record TransactionSummary(
        long incomeCents,
        long expenseCents,
        long netCents,
        List<CategoryTotal> byCategory) {}
