package com.aether.ai.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * One message from the user. The client sends its own calendar context — the
 * server never guesses a timezone.
 *
 * @param today         the caller's local date
 * @param offsetMinutes the caller's offset from UTC in minutes, east positive
 */
record ChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @NotNull LocalDate today,
        @NotNull @Min(-840) @Max(840) Integer offsetMinutes) {}
