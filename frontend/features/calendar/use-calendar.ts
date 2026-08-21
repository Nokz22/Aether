"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { eventsApi, type CreateEventRequest, type UpdateEventRequest } from "@/lib/api";
import { useSession } from "@/features/auth/session-provider";

/** Events overlapping [fromIso, toIso). The range is part of the key so weeks cache independently. */
export function useEventsInRange(fromIso: string, toIso: string) {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["events", fromIso, toIso],
    queryFn: () => eventsApi.inRange(fromIso, toIso, accessToken),
  });
}

export function useEventMutations() {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["events"] });

  const create = useMutation({
    mutationFn: (body: CreateEventRequest) => eventsApi.create(body, accessToken),
    onSuccess: invalidate,
  });

  const update = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateEventRequest }) =>
      eventsApi.update(id, body, accessToken),
    onSuccess: invalidate,
  });

  const remove = useMutation({
    mutationFn: (id: string) => eventsApi.remove(id, accessToken),
    onSuccess: invalidate,
  });

  return { create, update, remove };
}
