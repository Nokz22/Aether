"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  transactionsApi,
  type CreateTransactionRequest,
  type UpdateTransactionRequest,
} from "@/lib/api";
import { useSession } from "@/features/auth/session-provider";

export function useTransactions(from: string, to: string) {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["transactions", from, to],
    queryFn: () => transactionsApi.list(from, to, accessToken),
  });
}

export function useTransactionSummary(from: string, to: string) {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: ["transactions-summary", from, to],
    queryFn: () => transactionsApi.summary(from, to, accessToken),
  });
}

export function useTransactionMutations() {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();

  // Any write changes both the ledger and its totals.
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["transactions"] });
    queryClient.invalidateQueries({ queryKey: ["transactions-summary"] });
  };

  const create = useMutation({
    mutationFn: (body: CreateTransactionRequest) => transactionsApi.create(body, accessToken),
    onSuccess: invalidate,
  });

  const update = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateTransactionRequest }) =>
      transactionsApi.update(id, body, accessToken),
    onSuccess: invalidate,
  });

  const remove = useMutation({
    mutationFn: (id: string) => transactionsApi.remove(id, accessToken),
    onSuccess: invalidate,
  });

  return { create, update, remove };
}
