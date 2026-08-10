"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ChevronLeftIcon, ChevronRightIcon, CloseIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import type { TaskResponse, TaskStatus } from "@/lib/api";

const ORDER: TaskStatus[] = ["TODO", "DOING", "DONE"];

type TaskCardProps = {
  task: TaskResponse;
  busy: boolean;
  onMove: (task: TaskResponse, status: TaskStatus) => void;
  onRename: (task: TaskResponse, title: string) => void;
  onDelete: (task: TaskResponse) => void;
};

export function TaskCard({ task, busy, onMove, onRename, onDelete }: TaskCardProps) {
  const t = useTranslations("projects");
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const index = ORDER.indexOf(task.status);
  const prev = ORDER[index - 1];
  const next = ORDER[index + 1];

  function commitRename(value: string) {
    const title = value.trim();
    if (title && title !== task.title) {
      onRename(task, title);
    }
    setEditing(false);
  }

  return (
    <div className="surface-card group flex flex-col gap-2 p-3">
      {editing ? (
        <input
          defaultValue={task.title}
          aria-label={t("renameTask", { title: task.title })}
          maxLength={300}
          autoFocus
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              commitRename(event.currentTarget.value);
            } else if (event.key === "Escape") {
              setEditing(false);
            }
          }}
          onBlur={(event) => commitRename(event.currentTarget.value)}
          className="rounded-sm bg-transparent text-sm focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--focus-ring)]"
        />
      ) : (
        <button
          type="button"
          onClick={() => setEditing(true)}
          className="text-left text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
        >
          {task.title}
        </button>
      )}

      {confirmingDelete ? (
        <div className="flex items-center gap-2 text-xs">
          <span className="text-muted">{t("confirmDelete")}</span>
          <button
            type="button"
            onClick={() => onDelete(task)}
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
        </div>
      ) : (
        <div className="flex items-center justify-between opacity-0 transition-opacity duration-150 group-focus-within:opacity-100 group-hover:opacity-100">
          <div className="flex gap-1">
            <MoveButton
              disabled={busy || !prev}
              label={t("moveLeft")}
              onClick={() => prev && onMove(task, prev)}
              icon={<ChevronLeftIcon width={15} height={15} />}
            />
            <MoveButton
              disabled={busy || !next}
              label={t("moveRight")}
              onClick={() => next && onMove(task, next)}
              icon={<ChevronRightIcon width={15} height={15} />}
            />
          </div>
          <button
            type="button"
            aria-label={t("deleteTask", { title: task.title })}
            onClick={() => setConfirmingDelete(true)}
            className="flex size-6 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-danger/10 hover:text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            <CloseIcon width={14} height={14} />
          </button>
        </div>
      )}
    </div>
  );
}

function MoveButton({
  disabled,
  label,
  onClick,
  icon,
}: {
  disabled: boolean;
  label: string;
  onClick: () => void;
  icon: React.ReactNode;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      aria-label={label}
      onClick={onClick}
      className={cn(
        "flex size-6 items-center justify-center rounded-md text-muted",
        "transition-colors duration-150 hover:bg-accent/10 hover:text-foreground",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
        "disabled:cursor-not-allowed disabled:opacity-30",
      )}
    >
      {icon}
    </button>
  );
}
