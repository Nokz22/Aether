"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { aiApi, type ChatMessage } from "@/lib/api";
import { localDateISO, localOffsetMinutes } from "@/lib/dates";
import { useSession } from "@/features/auth/session-provider";

const CONVERSATION = ["ai", "messages"];

export function useConversation() {
  const { accessToken } = useSession();
  return useQuery({
    queryKey: CONVERSATION,
    queryFn: () => aiApi.history(accessToken),
  });
}

export function useAssistantMutations() {
  const { accessToken } = useSession();
  const queryClient = useQueryClient();

  const send = useMutation({
    // The calendar context is read at send time, so a session left open
    // overnight still tells the server the right day.
    mutationFn: (message: string) =>
      aiApi.chat(
        { message, today: localDateISO(), offsetMinutes: localOffsetMinutes() },
        accessToken,
      ),
    onSuccess: (reply, message) => {
      // Show the exchange the moment the answer arrives, then reconcile with
      // the stored conversation in the background.
      queryClient.setQueryData<ChatMessage[]>(CONVERSATION, (current = []) => [
        ...current,
        {
          id: `pending-${reply.id}`,
          role: "USER",
          content: message,
          toolsUsed: [],
          createdAt: new Date().toISOString(),
        },
        reply,
      ]);
      void queryClient.invalidateQueries({ queryKey: CONVERSATION });
    },
    // The message itself was stored before the model was called, so refetch to
    // show the conversation as it really is: asked, unanswered.
    onError: () => queryClient.invalidateQueries({ queryKey: CONVERSATION }),
  });

  const clear = useMutation({
    mutationFn: () => aiApi.clear(accessToken),
    onSuccess: () => queryClient.setQueryData<ChatMessage[]>(CONVERSATION, []),
  });

  return { send, clear };
}
