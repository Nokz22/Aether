"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { TrashIcon } from "@/components/ui/icons";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/cn";
import type { NoteResponse } from "@/lib/api";
import { useNoteMutations } from "./use-notes";

const AUTOSAVE_DELAY_MS = 600;

type SaveStatus = "clean" | "saving" | "saved";

/**
 * Editor for one note with debounced autosave.
 * A draft (note === null) is only persisted once its content is non-blank;
 * from then on it patches the created note in place — no remount, no lost keys.
 */
export function NoteForm({
  note,
  onCreated,
  onDeleted,
}: {
  note: NoteResponse | null;
  onCreated: (id: string) => void;
  onDeleted: () => void;
}) {
  const t = useTranslations("notes");
  const { create, update, remove } = useNoteMutations();
  const createNote = create.mutateAsync;
  const updateNote = update.mutateAsync;

  const [title, setTitle] = useState(note?.title ?? "");
  const [content, setContent] = useState(note?.content ?? "");
  const [status, setStatus] = useState<SaveStatus>("clean");
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const persistedId = useRef<string | null>(note?.id ?? null);
  const lastSaved = useRef({ title: note?.title ?? "", content: note?.content ?? "" });

  useEffect(() => {
    const dirty = title !== lastSaved.current.title || content !== lastSaved.current.content;
    if (!dirty) return;
    // An untouched draft with no content has nothing to persist.
    if (persistedId.current === null && content.trim() === "") return;

    setStatus("saving");
    const timer = setTimeout(async () => {
      try {
        if (persistedId.current === null) {
          const created = await createNote({ title, content });
          persistedId.current = created.id;
          onCreated(created.id);
        } else {
          await updateNote({ id: persistedId.current, body: { title, content } });
        }
        lastSaved.current = { title, content };
        setStatus("saved");
      } catch {
        setStatus("clean");
      }
    }, AUTOSAVE_DELAY_MS);

    return () => clearTimeout(timer);
  }, [title, content, createNote, updateNote, onCreated]);

  async function handleDelete() {
    if (persistedId.current !== null) {
      await remove.mutateAsync(persistedId.current);
    }
    onDeleted();
  }

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex items-center gap-3">
        <Input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder={t("titlePlaceholder")}
          aria-label={t("titleLabel")}
          maxLength={200}
          className="h-10 border-transparent px-0 text-lg font-medium hover:border-transparent"
        />
        <span className="shrink-0 text-xs text-muted" role="status" aria-live="polite">
          {status === "saving" ? t("saving") : status === "saved" ? t("saved") : ""}
        </span>
        {confirmingDelete ? (
          <span className="flex shrink-0 items-center gap-2 text-sm">
            <span className="text-muted">{t("confirmDelete")}</span>
            <button
              type="button"
              onClick={handleDelete}
              className="font-medium text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t("confirmYes")}
            </button>
            <button
              type="button"
              onClick={() => setConfirmingDelete(false)}
              className="text-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t("confirmNo")}
            </button>
          </span>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmingDelete(true)}
            aria-label={t("delete")}
            title={t("delete")}
            className="flex size-9 shrink-0 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-danger/10 hover:text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            <TrashIcon width={17} height={17} />
          </button>
        )}
      </div>

      <textarea
        value={content}
        onChange={(event) => setContent(event.target.value)}
        placeholder={t("contentPlaceholder")}
        aria-label={t("contentLabel")}
        maxLength={50000}
        className={cn(
          "flex-1 resize-none rounded-md bg-transparent leading-relaxed text-foreground",
          "placeholder:text-muted focus-visible:outline-none",
        )}
      />
    </div>
  );
}
