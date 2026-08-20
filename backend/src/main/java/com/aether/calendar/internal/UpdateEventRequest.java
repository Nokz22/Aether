package com.aether.calendar.internal;

import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Partial update: a null field is left unchanged. */
record UpdateEventRequest(
        @Size(max = 300) String title,
        @Size(max = 2000) String description,
        Instant startsAt,
        Instant endsAt) {}
