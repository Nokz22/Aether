"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { habitsApi, type HabitResponse } from "@/lib/api";
import { localDateISO } from "@/lib/dates";
import { useSession } from "@/features/auth/session-provider";

export function useHabits() {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();
  const today = localDateISO();

  const habits = useQuery({
    queryKey: ["habits", today],
    queryFn: () => habitsApi.list(today, accessToken),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["habits"] });

  const create = useMutation({
    mutationFn: (name: string) => habitsApi.create(name, accessToken),
    onSuccess: invalidate,
  });

  const toggleToday = useMutation({
    mutationFn: (habit: HabitResponse) =>
      habit.doneToday
        ? habitsApi.removeCheckin(habit.id, today, accessToken)
        : habitsApi.checkIn(habit.id, today, today, accessToken),
    onSuccess: invalidate,
  });

  const rename = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) =>
      habitsApi.rename(id, name, accessToken),
    onSuccess: invalidate,
  });

  const remove = useMutation({
    mutationFn: (id: string) => habitsApi.remove(id, accessToken),
    onSuccess: invalidate,
  });

  return { habits, create, toggleToday, rename, remove };
}
