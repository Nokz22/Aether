package com.aether.projects.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock
    private ProjectRepository projects;

    @Mock
    private TaskRepository tasks;

    private ProjectService service() {
        return new ProjectService(projects, tasks);
    }

    private Task taskWith(UUID projectId, TaskStatus status) {
        Task task = new Task(projectId, "t");
        task.update(null, status);
        return task;
    }

    @Test
    void listAggregatesTaskCountsPerStatus() {
        Project project = new Project(USER, "Website", "");
        when(projects.findAllByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of(project));
        when(tasks.findAllByProjectIdInOrderByCreatedAt(List.of(project.id()))).thenReturn(List.of(
                taskWith(project.id(), TaskStatus.TODO),
                taskWith(project.id(), TaskStatus.TODO),
                taskWith(project.id(), TaskStatus.DOING),
                taskWith(project.id(), TaskStatus.DONE)));

        ProjectSummary summary = service().list(USER).getFirst();

        assertThat(summary.taskCounts()).isEqualTo(new TaskCounts(2, 1, 1));
    }

    @Test
    void listGivesZeroCountsForAProjectWithoutTasks() {
        Project project = new Project(USER, "Empty", "");
        when(projects.findAllByUserIdOrderByCreatedAtDesc(USER)).thenReturn(List.of(project));
        when(tasks.findAllByProjectIdInOrderByCreatedAt(List.of(project.id()))).thenReturn(List.of());

        assertThat(service().list(USER).getFirst().taskCounts()).isEqualTo(new TaskCounts(0, 0, 0));
    }

    @Test
    void newTaskStartsAsTodo() {
        Project project = new Project(USER, "P", "");
        when(projects.findByIdAndUserId(project.id(), USER)).thenReturn(Optional.of(project));

        TaskResponse task = service().addTask(USER, project.id(), "  Draft the brief  ");

        assertThat(task.title()).isEqualTo("Draft the brief");
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        verify(tasks).save(any(Task.class));
    }

    @Test
    void updateTaskChangesOnlyProvidedFields() {
        Project project = new Project(USER, "P", "");
        Task task = new Task(project.id(), "Old");
        when(projects.findByIdAndUserId(project.id(), USER)).thenReturn(Optional.of(project));
        when(tasks.findByIdAndProjectId(task.id(), project.id())).thenReturn(Optional.of(task));

        TaskResponse updated = service().updateTask(USER, project.id(), task.id(), null, TaskStatus.DOING);

        assertThat(updated.title()).isEqualTo("Old");
        assertThat(updated.status()).isEqualTo(TaskStatus.DOING);
    }

    @Test
    void someoneElsesProjectIsNotFound() {
        UUID foreign = UUID.randomUUID();
        when(projects.findByIdAndUserId(foreign, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(USER, foreign))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("project_not_found"));
    }

    @Test
    void aTaskInSomeoneElsesProjectIsNotFound() {
        UUID foreignProject = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(projects.findByIdAndUserId(foreignProject, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().deleteTask(USER, foreignProject, taskId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("project_not_found"));
        verify(tasks, never()).delete(any());
    }

    @Test
    void aTaskThatIsNotInTheProjectIsNotFound() {
        Project project = new Project(USER, "P", "");
        UUID taskId = UUID.randomUUID();
        when(projects.findByIdAndUserId(project.id(), USER)).thenReturn(Optional.of(project));
        when(tasks.findByIdAndProjectId(taskId, project.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().updateTask(USER, project.id(), taskId, "x", null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("task_not_found"));
    }
}
