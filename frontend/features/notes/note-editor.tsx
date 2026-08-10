"use client";

import { useTranslations } from "next-intl";
import { NoteForm } from "./note-form";
import { useNote } from "./use-notes";

type NoteEditorProps = {
  selectedId: string | null;
  isDraft: boolean;
  onCreated: (id: string) => void;
  onDeleted: () => void;
};

export function NoteEditor({ selectedId, isDraft, onCreated, onDeleted }: NoteEditorProps) {
  const t = useTranslations("notes");
  const note = useNote(isDraft ? null : selectedId);

  if (isDraft) {
    return <NoteForm key="draft" note={null} onCreated={onCreated} onDeleted={onDeleted} />;
  }

  if (selectedId === null) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-muted">{t("noSelection")}</p>
      </div>
    );
  }

  if (note.isPending) {
    return <div className="h-full animate-pulse rounded-md bg-border-glass" aria-busy />;
  }

  if (note.isError || !note.data) {
    return (
      <div className="flex h-full items-center justify-center">
        <p role="alert" className="text-danger">
          {t("loadError")}
        </p>
      </div>
    );
  }

  return (
    <NoteForm key={note.data.id} note={note.data} onCreated={onCreated} onDeleted={onDeleted} />
  );
}
