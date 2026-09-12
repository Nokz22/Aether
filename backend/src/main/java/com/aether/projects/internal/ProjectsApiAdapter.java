package com.aether.projects.internal;

import com.aether.projects.ProjectDetailView;
import com.aether.projects.ProjectView;
import com.aether.projects.ProjectsApi;
import com.aether.projects.TaskState;
import com.aether.projects.TaskView;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProjectsApiAdapter implements ProjectsApi {

    private final ProjectService projects;

    ProjectsApiAdapter(ProjectService projects) {
        this.projects = projects;
    }

    @Override
    public List<ProjectView> projects(UUID userId) {
        return projects.list(userId).stream()
                .map(project -> new ProjectView(project.id(), project.name(),
                        project.taskCounts().todo(), project.taskCounts().doing(),
                        project.taskCounts().done()))
                .toList();
    }

    @Override
    public ProjectDetailView project(UUID userId, UUID projectId) {
        ProjectResponse project = projects.get(userId, projectId);
        return new ProjectDetailView(project.id(), project.name(), project.description(),
                project.tasks().stream().map(ProjectsApiAdapter::toView).toList());
    }

    @Override
    public ProjectView createProject(UUID userId, String name) {
        ProjectResponse created = projects.create(userId, name, null);
        return new ProjectView(created.id(), created.name(), 0, 0, 0);
    }

    @Override
    public TaskView addTask(UUID userId, UUID projectId, String title) {
        return toView(projects.addTask(userId, projectId, title));
    }

    @Override
    public TaskView moveTask(UUID userId, UUID projectId, UUID taskId, TaskState state) {
        return toView(projects.updateTask(userId, projectId, taskId, null, toStatus(state)));
    }

    private static TaskView toView(TaskResponse task) {
        return new TaskView(task.id(), task.title(), toState(task.status()));
    }

    private static TaskState toState(TaskStatus status) {
        return switch (status) {
            case TODO -> TaskState.TODO;
            case DOING -> TaskState.DOING;
            case DONE -> TaskState.DONE;
        };
    }

    private static TaskStatus toStatus(TaskState state) {
        return switch (state) {
            case TODO -> TaskStatus.TODO;
            case DOING -> TaskStatus.DOING;
            case DONE -> TaskStatus.DONE;
        };
    }
}
