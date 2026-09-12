package com.aether.calendar;

import java.time.Instant;
import java.util.UUID;

/** An event as seen from outside the module; times are UTC instants. */
public record EventView(
        UUID id,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt) {}
