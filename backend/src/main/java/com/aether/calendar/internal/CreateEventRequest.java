package com.aether.calendar.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record CreateEventRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {}
