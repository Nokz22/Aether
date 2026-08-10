package com.aether.projects.internal;

import com.aether.auth.AuthApi;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
class ProjectController {

    private final ProjectService projectService;
    private final AuthApi authApi;

    ProjectController(ProjectService projectService, AuthApi authApi) {
        this.projectService = projectService;
        this.authApi = authApi;
    }

    @GetMapping
    List<ProjectSummary> list() {
        return projectService.list(authApi.currentUserId());
    }

    @PostMapping
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = projectService.create(
                authApi.currentUserId(), request.name(), request.description());
        return ResponseEntity.created(URI.create("/api/projects/" + project.id())).body(project);
    }

    @GetMapping("/{id}")
    ProjectResponse get(@PathVariable UUID id) {
        return projectService.get(authApi.currentUserId(), id);
    }

    @PatchMapping("/{id}")
    ProjectResponse update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(
                authApi.currentUserId(), id, request.name(), request.description());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(authApi.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tasks")
    ResponseEntity<TaskResponse> addTask(@PathVariable UUID id,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = projectService.addTask(
                authApi.currentUserId(), id, request.title());
        return ResponseEntity
                .created(URI.create("/api/projects/" + id + "/tasks/" + task.id()))
                .body(task);
    }

    @PatchMapping("/{id}/tasks/{taskId}")
    TaskResponse updateTask(@PathVariable UUID id, @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return projectService.updateTask(
                authApi.currentUserId(), id, taskId, request.title(), request.status());
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    ResponseEntity<Void> deleteTask(@PathVariable UUID id, @PathVariable UUID taskId) {
        projectService.deleteTask(authApi.currentUserId(), id, taskId);
        return ResponseEntity.noContent().build();
    }
}
