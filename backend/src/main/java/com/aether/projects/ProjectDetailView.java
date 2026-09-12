package com.aether.projects;

import java.util.List;
import java.util.UUID;

public record ProjectDetailView(
        UUID id,
        String name,
        String description,
        List<TaskView> tasks) {}
