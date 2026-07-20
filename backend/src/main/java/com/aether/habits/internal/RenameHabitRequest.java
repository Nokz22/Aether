package com.aether.habits.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RenameHabitRequest(@NotBlank @Size(max = 100) String name) {}
