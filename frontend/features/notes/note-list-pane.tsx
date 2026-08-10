"use client";

import { useTranslations } from "next-intl";
import { PlusIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import type { NoteSummary } from "@/lib/api";

type NoteListPaneProps = {
  notes: NoteSummary[];
  selectedId: string | null;
  isDraft: boolean;
  onSelect: (id: string) => void;
  onNew: () => void;
};

export function NoteListPane({ notes, selectedId, isDraft, onSelect, onNew }: NoteListPaneProps) {
  const t = useTranslations("notes");

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between px-2 pb-3">
        <h1 className="text-lg font-medium tracking-tight">{t("title")}</h1>
        <button
          type="button"
          onClick={onNew}
          aria-label={t("newNote")}
          title={t("newNote")}
          className={cn(
            "flex size-9 items-center justify-center rounded-md text-muted",
            "transition-colors duration-150 hover:bg-accent/10 hover:text-foreground",
            "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
            isDraft && "bg-accent/15 text-foreground",
          )}
        >
          <PlusIcon />
        </button>
      </div>

      {notes.length === 0 && !isDraft ? (
        <p className="px-2 text-sm text-muted">{t("empty")}</p>
      ) : (
        <ul className="flex flex-col gap-1 overflow-y-auto">
          {notes.map((note) => {
            const label = note.title.trim() || note.preview || t("untitled");
            const active = note.id === selectedId && !isDraft;
            return (
              <li key={note.id}>
                <button
                  type="button"
                  onClick={() => onSelect(note.id)}
                  aria-current={active ? "true" : undefined}
                  className={cn(
                    "flex w-full flex-col gap-0.5 rounded-md px-3 py-2 text-left",
                    "transition-colors duration-150",
                    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
                    active ? "bg-accent/15" : "hover:bg-accent/10",
                  )}
                >
                  <span className="truncate text-sm font-medium">{label}</span>
                  {note.title.trim() && note.preview && (
                    <span className="truncate text-xs text-muted">{note.preview}</span>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
