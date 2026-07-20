"use client";

import { useTranslations } from "next-intl";
import { CheckIcon, FlameIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import type { HabitResponse } from "@/lib/api";

type HabitRowProps = {
  habit: HabitResponse;
  onToggle: (habit: HabitResponse) => void;
  toggling: boolean;
};

export function HabitRow({ habit, onToggle, toggling }: HabitRowProps) {
  const t = useTranslations("habits");

  return (
    <li
      className={cn(
        "flex items-center gap-4 rounded-md px-3 py-3",
        "transition-opacity duration-150",
        habit.doneToday && "opacity-60",
      )}
    >
      <button
        type="button"
        aria-pressed={habit.doneToday}
        aria-label={t(habit.doneToday ? "markUndone" : "markDone", { name: habit.name })}
        disabled={toggling}
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
        <p className="truncate">{habit.name}</p>
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

      <p
        className="flex items-center gap-1 text-sm text-muted"
        aria-label={t("streak", { count: habit.currentStreak })}
      >
        <FlameIcon width={15} height={15} />
        {habit.currentStreak}
      </p>
    </li>
  );
}
