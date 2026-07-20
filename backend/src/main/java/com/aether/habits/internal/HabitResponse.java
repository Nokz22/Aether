package com.aether.habits.internal;

import java.util.List;
import java.util.UUID;

record HabitResponse(
        UUID id,
        String name,
        boolean doneToday,
        int currentStreak,
        List<HabitDay> week) {}
