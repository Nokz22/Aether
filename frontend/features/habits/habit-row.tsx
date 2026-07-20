"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { CheckIcon, CloseIcon, FlameIcon } from "@/components/ui/icons";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/cn";
import type { HabitResponse } from "@/lib/api";

type HabitRowProps = {
  habit: HabitResponse;
  onToggle: (habit: HabitResponse) => void;
  onRename: (id: string, name: string) => void;
  onDelete: (id: string) => void;
  busy: boolean;
};

export function HabitRow({ habit, onToggle, onRename, onDelete, busy }: HabitRowProps) {
  const t = useTranslations("habits");
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  function commitRename(value: string) {
    const name = value.trim();
    if (name && name !== habit.name) {
      onRename(habit.id, name);
    }
    setEditing(false);
  }

  return (
    <li
      className={cn(
        "group flex items-center gap-4 rounded-md px-3 py-3",
        "transition-opacity duration-150",
        habit.doneToday && !editing && "opacity-60",
      )}
    >
      <button
        type="button"
        aria-pressed={habit.doneToday}
        aria-label={t(habit.doneToday ? "markUndone" : "markDone", { name: habit.name })}
        disabled={busy}
        onClick={() => onToggle(habit)}
        className={cn(
          "check-circle flex size-7 shrink-0 items-center justify-center rounded-full",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
          "disabled:cursor-not-allowed",
        )}
      >
        <CheckIcon
          width={14}
          height={14}
          className={cn(
            "text-accent-foreground transition-opacity duration-150",
            habit.doneToday ? "opacity-100" : "opacity-0",
          )}
        />
      </button>

      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        {editing ? (
          <Input
            defaultValue={habit.name}
            aria-label={t("renameLabel", { name: habit.name })}
            maxLength={100}
            autoFocus
            className="h-8"
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                commitRename(event.currentTarget.value);
              } else if (event.key === "Escape") {
                setEditing(false);
              }
            }}
            onBlur={() => setEditing(false)}
          />
        ) : (
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="truncate text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            {habit.name}
          </button>
        )}
        <div className="flex gap-1.5" aria-hidden>
          {habit.week.map((day) => (
            <span
              key={day.date}
              className={cn(
                "size-1.5 rounded-full transition-colors duration-150",
                day.done ? "bg-accent" : "bg-border-glass",
              )}
            />
          ))}
        </div>
      </div>

      {confirmingDelete ? (
        <div className="flex items-center gap-3 text-sm">
          <span className="text-muted">{t("confirmDelete")}</span>
          <button
            type="button"
            disabled={busy}
            onClick={() => onDelete(habit.id)}
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
        <div className="flex items-center gap-3">
          <p
            className="flex items-center gap-1 text-sm text-muted"
            aria-label={t("streak", { count: habit.currentStreak })}
          >
            <FlameIcon width={15} height={15} />
            {habit.currentStreak}
          </p>
          <button
            type="button"
            aria-label={t("delete", { name: habit.name })}
            onClick={() => setConfirmingDelete(true)}
            className={cn(
              "flex size-7 items-center justify-center rounded-md text-muted",
              "opacity-40 transition-all duration-150",
              "hover:bg-danger/10 hover:text-danger hover:opacity-100",
              "group-hover:opacity-100 focus-visible:opacity-100",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
            )}
          >
            <CloseIcon width={15} height={15} />
          </button>
        </div>
      )}
    </li>
  );
}
