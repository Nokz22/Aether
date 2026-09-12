package com.aether.projects.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.projects.ProjectView;
import com.aether.projects.TaskState;
import com.aether.projects.TaskView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectsApiAdapterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @Mock
    private ProjectService projects;

    @Test
    void projectsExposePerStateCounts() {
        UUID id = UUID.randomUUID();
        when(projects.list(USER)).thenReturn(List.of(
                new ProjectSummary(id, "Website", new TaskCounts(2, 1, 3), NOW)));

        assertThat(new ProjectsApiAdapter(projects).projects(USER))
                .containsExactly(new ProjectView(id, "Website", 2, 1, 3));
    }

    @Test
    void everyInternalStatusMapsToItsPublicState() {
        UUID id = UUID.randomUUID();
        when(projects.get(USER, id)).thenReturn(new ProjectResponse(id, "P", "d", NOW, NOW, List.of(
                new TaskResponse(UUID.randomUUID(), "a", TaskStatus.TODO, NOW, NOW),
                new TaskResponse(UUID.randomUUID(), "b", TaskStatus.DOING, NOW, NOW),
                new TaskResponse(UUID.randomUUID(), "c", TaskStatus.DONE, NOW, NOW))));

        List<TaskView> tasks = new ProjectsApiAdapter(projects).project(USER, id).tasks();

        assertThat(tasks).extracting(TaskView::state)
                .containsExactly(TaskState.TODO, TaskState.DOING, TaskState.DONE);
    }

    @Test
    void moveTaskTranslatesThePublicStateAndLeavesTheTitleAlone() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(projects.updateTask(USER, projectId, taskId, null, TaskStatus.DONE))
                .thenReturn(new TaskResponse(taskId, "Publicar", TaskStatus.DONE, NOW, NOW));

        TaskView moved = new ProjectsApiAdapter(projects)
                .moveTask(USER, projectId, taskId, TaskState.DONE);

        assertThat(moved).isEqualTo(new TaskView(taskId, "Publicar", TaskState.DONE));
        verify(projects).updateTask(eq(USER), eq(projectId), eq(taskId), isNull(), eq(TaskStatus.DONE));
    }

    @Test
    void createdProjectStartsWithNoTasks() {
        UUID id = UUID.randomUUID();
        when(projects.create(USER, "Novo", null))
                .thenReturn(new ProjectResponse(id, "Novo", "", NOW, NOW, List.of()));

        assertThat(new ProjectsApiAdapter(projects).createProject(USER, "Novo"))
                .isEqualTo(new ProjectView(id, "Novo", 0, 0, 0));
    }
}
