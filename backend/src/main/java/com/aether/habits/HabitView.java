package com.aether.habits;

import java.util.UUID;

/** A habit as seen from outside the module. */
public record HabitView(UUID id, String name, boolean doneToday, int currentStreak) {}
