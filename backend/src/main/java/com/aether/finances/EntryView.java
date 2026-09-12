package com.aether.finances;

import java.time.LocalDate;
import java.util.UUID;

/** A transaction as seen from outside the module; amounts are integer cents. */
public record EntryView(
        UUID id,
        EntryType type,
        long amountCents,
        String category,
        String description,
        LocalDate date) {}
