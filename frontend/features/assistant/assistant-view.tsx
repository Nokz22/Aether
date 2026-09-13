"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { SparkIcon } from "@/components/ui/icons";
import { ApiRequestError } from "@/lib/api";
import { Composer } from "./composer";
import { MessageTurn } from "./message-turn";
import { useAssistantMutations, useConversation } from "./use-assistant";

const SUGGESTIONS = ["today", "expense", "plan"] as const;

export function AssistantView() {
  const t = useTranslations("assistant");
  const conversation = useConversation();
  const { send, clear } = useAssistantMutations();

  const [draft, setDraft] = useState("");
  const [confirmingClear, setConfirmingClear] = useState(false);
  const scroller = useRef<HTMLDivElement>(null);

  const messages = conversation.data ?? [];
  const pendingMessage = send.isPending ? (send.variables ?? null) : null;

  // Keep the newest turn in view as the conversation grows.
  useEffect(() => {
    const element = scroller.current;
    if (element) {
      requestAnimationFrame(() => element.scrollTo({ top: element.scrollHeight }));
    }
  }, [messages.length, pendingMessage]);

  function sendDraft() {
    const message = draft.trim();
    if (message.length === 0 || send.isPending) {
      return;
    }
    // Cleared on success only, so a failed send leaves the text to retry.
    send.mutate(message, { onSuccess: () => setDraft("") });
  }

  const notConfigured =
    send.error instanceof ApiRequestError && send.error.body.code === "ai_not_configured";

  return (
    <div className="mx-auto flex h-full w-full max-w-[720px] flex-col">
      <header className="flex items-center justify-between gap-3 pb-4">
        <div className="flex items-center gap-2">
          <SparkIcon width={18} height={18} className="text-accent" />
          <h1 className="text-xl font-medium tracking-tight">{t("title")}</h1>
        </div>
        {messages.length > 0 && (
          <div className="flex items-center gap-2 text-xs">
            {confirmingClear ? (
              <>
                <span className="text-muted">{t("confirmClear")}</span>
                <button
                  type="button"
                  onClick={() => {
                    clear.mutate();
                    setConfirmingClear(false);
                  }}
                  className="font-medium text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
                >
                  {t("confirmYes")}
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmingClear(false)}
                  className="text-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
                >
                  {t("confirmNo")}
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmingClear(true)}
                className="rounded-md px-2 py-1 text-muted transition-colors duration-150 hover:bg-accent/10 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
              >
                {t("clear")}
              </button>
            )}
          </div>
        )}
      </header>

      <div
        ref={scroller}
        // The fade keeps scrolled turns from touching the header.
        className="flex min-h-0 flex-1 flex-col overflow-y-auto [mask-image:linear-gradient(to_bottom,transparent,black_24px)]"
      >
        {conversation.isPending ? (
          <div className="h-24 animate-pulse rounded-md bg-border-glass" aria-busy />
        ) : conversation.isError ? (
          <div className="flex flex-col items-start gap-3">
            <p role="alert" className="text-danger">
              {t("loadError")}
            </p>
            <Button className="w-auto px-4" onClick={() => conversation.refetch()}>
              {t("retry")}
            </Button>
          </div>
        ) : messages.length === 0 && pendingMessage === null ? (
          <EmptyState onPick={setDraft} />
        ) : (
          <ul
            // mt-auto rests a short conversation on the composer, where the eye
            // already is, instead of stranding it at the top of the page.
            className="mt-auto flex flex-col gap-6 pb-4"
          >
            {messages.map((message) => (
              <MessageTurn key={message.id} message={message} />
            ))}
            {pendingMessage !== null && (
              <>
                <MessageTurn
                  message={{
                    id: "pending",
                    role: "USER",
                    content: pendingMessage,
                    toolsUsed: [],
                    createdAt: new Date().toISOString(),
                  }}
                />
                <li aria-live="polite" className="text-sm text-muted">
                  <span className="assistant-thinking">{t("thinking")}</span>
                </li>
              </>
            )}
          </ul>
        )}
      </div>

      <div className="shrink-0 pt-4">
        {send.isError && (
          <p role="alert" className="pb-2 text-sm text-danger">
            {notConfigured ? t("notConfigured") : t("sendError")}
          </p>
        )}
        <Composer
          value={draft}
          disabled={send.isPending}
          onChange={setDraft}
          onSend={sendDraft}
        />
        <p className="pt-2 text-center text-xs text-muted">{t("disclaimer")}</p>
      </div>
    </div>
  );
}

function EmptyState({ onPick }: { onPick: (value: string) => void }) {
  const t = useTranslations("assistant");

  return (
    <div className="flex h-full flex-col items-center justify-center gap-5 text-center">
      <div className="space-y-1.5">
        <p className="text-lg">{t("emptyTitle")}</p>
        <p className="max-w-sm text-sm text-muted">{t("emptyBody")}</p>
      </div>
      <ul className="flex flex-wrap justify-center gap-2">
        {SUGGESTIONS.map((key) => (
          <li key={key}>
            <button
              type="button"
              onClick={() => onPick(t(`suggestions.${key}`))}
              className="rounded-full border border-border-glass px-3 py-1.5 text-sm text-muted transition-colors duration-150 hover:border-accent/40 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t(`suggestions.${key}`)}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
