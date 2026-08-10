"use client";

import { useCallback, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";
import { NoteEditor } from "./note-editor";
import { NoteListPane } from "./note-list-pane";
import { useNotesList } from "./use-notes";

export function NotesWorkspace() {
  const t = useTranslations("notes");
  const notes = useNotesList();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [draftKey, setDraftKey] = useState<string | null>(null);
  const draftCounter = useRef(0);

  const isDraft = draftKey !== null;
  // A draft only counts as "new/unsaved" for the list until its first save gives it an id.
  const draftUnsaved = isDraft && selectedId === null;
  const hasEditor = isDraft || selectedId !== null;

  function selectNote(id: string) {
    setDraftKey(null);
    setSelectedId(id);
  }

  function startNewNote() {
    draftCounter.current += 1;
    setDraftKey(`draft-${draftCounter.current}`);
    setSelectedId(null);
  }

  // The draft persisted: highlight it in the list, keep editing the same form.
  // Stable so it never retriggers the editor's autosave effect.
  const handleCreated = useCallback((id: string) => setSelectedId(id), []);

  const closeEditor = useCallback(() => {
    setDraftKey(null);
    setSelectedId(null);
  }, []);

  if (notes.isPending) {
    return <div className="h-full animate-pulse rounded-md bg-border-glass" aria-busy />;
  }

  if (notes.isError) {
    return (
      <div className="flex h-full flex-col items-start gap-3">
        <p role="alert" className="text-danger">
          {t("listError")}
        </p>
        <Button className="w-auto px-4" onClick={() => notes.refetch()}>
          {t("retry")}
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-full">
      <div
        className={cn(
          "w-full flex-col md:flex md:w-72 md:shrink-0 md:border-r md:border-border-glass md:pr-4",
          hasEditor ? "hidden" : "flex",
        )}
      >
        <NoteListPane
          notes={notes.data}
          selectedId={selectedId}
          isDraft={draftUnsaved}
          onSelect={selectNote}
          onNew={startNewNote}
        />
      </div>

      <div className={cn("min-w-0 flex-1 flex-col md:flex md:pl-6", hasEditor ? "flex" : "hidden")}>
        {hasEditor && (
          <button
            type="button"
            onClick={closeEditor}
            className="mb-4 self-start text-sm text-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)] md:hidden"
          >
            ← {t("back")}
          </button>
        )}
        <div className="min-h-0 flex-1">
          <NoteEditor
            selectedId={selectedId}
            isDraft={isDraft}
            onCreated={handleCreated}
            onDeleted={closeEditor}
          />
        </div>
      </div>
    </div>
  );
}
