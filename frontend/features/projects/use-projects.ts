"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { projectsApi, type UpdateProjectRequest, type UpdateTaskRequest } from "@/lib/api";
import { useSession } from "@/features/auth/session-provider";

export function useProjectsList() {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["projects"],
    queryFn: () => projectsApi.list(accessToken),
  });
}

export function useProject(projectId: string | null) {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["project", projectId],
    queryFn: () => projectsApi.get(projectId as string, accessToken),
    enabled: projectId !== null,
  });
}

/** Mutations for a single project's board; refreshes both the board and the list. */
export function useProjectMutations(projectId: string | null) {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["projects"] });
    if (projectId !== null) {
      queryClient.invalidateQueries({ queryKey: ["project", projectId] });
    }
  };

  const updateProject = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateProjectRequest }) =>
      projectsApi.update(id, body, accessToken),
    onSuccess: invalidate,
  });

  const removeProject = useMutation({
    mutationFn: (id: string) => projectsApi.remove(id, accessToken),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["projects"] }),
  });

  const addTask = useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) =>
      projectsApi.addTask(id, title, accessToken),
    onSuccess: invalidate,
  });

  const updateTask = useMutation({
    mutationFn: ({ id, taskId, body }: { id: string; taskId: string; body: UpdateTaskRequest }) =>
      projectsApi.updateTask(id, taskId, body, accessToken),
    onSuccess: invalidate,
  });

  const removeTask = useMutation({
    mutationFn: ({ id, taskId }: { id: string; taskId: string }) =>
      projectsApi.removeTask(id, taskId, accessToken),
    onSuccess: invalidate,
  });

  return { updateProject, removeProject, addTask, updateTask, removeTask };
}
