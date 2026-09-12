package com.aether.finances;

import java.util.List;

/** Totals for a date range; all amounts are integer cents. */
public record MoneySummaryView(
        long incomeCents,
        long expenseCents,
        long netCents,
        List<CategoryTotalView> byCategory) {}
