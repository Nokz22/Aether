"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { notesApi, type CreateNoteRequest, type UpdateNoteRequest } from "@/lib/api";
import { useSession } from "@/features/auth/session-provider";

export function useNotesList() {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["notes"],
    queryFn: () => notesApi.list(accessToken),
  });
}

export function useNote(noteId: string | null) {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["note", noteId],
    queryFn: () => notesApi.get(noteId as string, accessToken),
    enabled: noteId !== null,
  });
}

export function useNoteMutations() {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();
  const invalidateList = () => queryClient.invalidateQueries({ queryKey: ["notes"] });

  const create = useMutation({
    mutationFn: (body: CreateNoteRequest) => notesApi.create(body, accessToken),
    onSuccess: invalidateList,
  });

  const update = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateNoteRequest }) =>
      notesApi.update(id, body, accessToken),
    onSuccess: invalidateList,
  });

  const remove = useMutation({
    mutationFn: (id: string) => notesApi.remove(id, accessToken),
    onSuccess: invalidateList,
  });

  return { create, update, remove };
}
