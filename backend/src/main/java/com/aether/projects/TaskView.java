package com.aether.projects;

import java.util.UUID;

public record TaskView(UUID id, String title, TaskState state) {}
