package com.aether.projects.internal;

import com.aether.shared.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProjectService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;

    ProjectService(ProjectRepository projects, TaskRepository tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    List<ProjectSummary> list(UUID userId) {
        List<Project> owned = projects.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, TaskCounts> countsByProject = countsFor(owned);
        return owned.stream()
                .map(project -> new ProjectSummary(project.id(), project.name(),
                        countsByProject.getOrDefault(project.id(), TaskCounts.of(0, 0, 0)),
                        project.updatedAt()))
                .toList();
    }

    @Transactional
    ProjectResponse create(UUID userId, String name, String description) {
        Project project = new Project(userId, name.trim(), normalize(description));
        projects.save(project);
        return toResponse(project, List.of());
    }

    @Transactional(readOnly = true)
    ProjectResponse get(UUID userId, UUID projectId) {
        Project project = ownedProject(userId, projectId);
        return toResponse(project, tasks.findAllByProjectIdOrderByCreatedAt(projectId));
    }

    @Transactional
    ProjectResponse update(UUID userId, UUID projectId, String name, String description) {
        Project project = ownedProject(userId, projectId);
        project.update(name == null ? null : name.trim(), description);
        return toResponse(project, tasks.findAllByProjectIdOrderByCreatedAt(projectId));
    }

    @Transactional
    void delete(UUID userId, UUID projectId) {
        projects.delete(ownedProject(userId, projectId));
    }

    @Transactional
    TaskResponse addTask(UUID userId, UUID projectId, String title) {
        ownedProject(userId, projectId);
        Task task = new Task(projectId, title.trim());
        tasks.save(task);
        return TaskResponse.from(task);
    }

    @Transactional
    TaskResponse updateTask(UUID userId, UUID projectId, UUID taskId,
                            String title, TaskStatus status) {
        Task task = ownedTask(userId, projectId, taskId);
        task.update(title == null ? null : title.trim(), status);
        return TaskResponse.from(task);
    }

    @Transactional
    void deleteTask(UUID userId, UUID projectId, UUID taskId) {
        tasks.delete(ownedTask(userId, projectId, taskId));
    }

    // Ownership failures surface as 404, never revealing another user's data exists.
    private Project ownedProject(UUID userId, UUID projectId) {
        return projects.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "project_not_found",
                        "Project not found."));
    }

    private Task ownedTask(UUID userId, UUID projectId, UUID taskId) {
        ownedProject(userId, projectId);
        return tasks.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "task_not_found",
                        "Task not found."));
    }

    private Map<UUID, TaskCounts> countsFor(List<Project> owned) {
        if (owned.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = owned.stream().map(Project::id).toList();
        return tasks.findAllByProjectIdInOrderByCreatedAt(ids).stream()
                .collect(Collectors.groupingBy(Task::projectId))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> countByStatus(entry.getValue())));
    }

    private static TaskCounts countByStatus(List<Task> projectTasks) {
        Map<TaskStatus, Long> byStatus = projectTasks.stream()
                .collect(Collectors.groupingBy(Task::status, Collectors.counting()));
        return TaskCounts.of(
                byStatus.getOrDefault(TaskStatus.TODO, 0L),
                byStatus.getOrDefault(TaskStatus.DOING, 0L),
                byStatus.getOrDefault(TaskStatus.DONE, 0L));
    }

    private static ProjectResponse toResponse(Project project, List<Task> projectTasks) {
        return new ProjectResponse(project.id(), project.name(), project.description(),
                project.createdAt(), project.updatedAt(),
                projectTasks.stream().map(TaskResponse::from).toList());
    }

    private static String normalize(String description) {
        return description == null ? "" : description.trim();
    }
}
