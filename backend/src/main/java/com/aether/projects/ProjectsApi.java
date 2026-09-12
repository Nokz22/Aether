package com.aether.projects;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the projects module (ADR-002). Deliberately no delete: the AI
 * layer must not be able to remove a user's projects or tasks.
 */
public interface ProjectsApi {

    List<ProjectView> projects(UUID userId);

    ProjectDetailView project(UUID userId, UUID projectId);

    ProjectView createProject(UUID userId, String name);

    /** Adds a task to the project; it starts in {@link TaskState#TODO}. */
    TaskView addTask(UUID userId, UUID projectId, String title);

    TaskView moveTask(UUID userId, UUID projectId, UUID taskId, TaskState state);
}
