package com.aether.habits.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateHabitRequest(@NotBlank @Size(max = 100) String name) {}
