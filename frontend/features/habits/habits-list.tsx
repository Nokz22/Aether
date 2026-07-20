"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { HabitRow } from "./habit-row";
import { NewHabitForm } from "./new-habit-form";
import { useHabits } from "./use-habits";

export function HabitsList() {
  const t = useTranslations("habits");
  const { habits, create, toggleToday } = useHabits();

  if (habits.isPending) {
    return (
      <div className="flex flex-col gap-2" aria-busy>
        {[0, 1, 2].map((row) => (
          <div key={row} className="h-14 animate-pulse rounded-md bg-border-glass" />
        ))}
      </div>
    );
  }

  if (habits.isError) {
    return (
      <div className="flex flex-col items-start gap-3">
        <p role="alert" className="text-danger">
          {t("loadError")}
        </p>
        <Button className="w-auto px-4" onClick={() => habits.refetch()}>
          {t("retry")}
        </Button>
      </div>
    );
  }

  return (
    <div className="flex max-w-xl flex-col gap-4">
      {habits.data.length === 0 ? (
        <p className="text-muted">{t("empty")}</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {habits.data.map((habit) => (
            <HabitRow
              key={habit.id}
              habit={habit}
              onToggle={(target) => toggleToday.mutate(target)}
              toggling={toggleToday.isPending}
            />
          ))}
        </ul>
      )}

      <NewHabitForm onCreate={(name) => create.mutate(name)} creating={create.isPending} />

      {(create.isError || toggleToday.isError) && (
        <p role="alert" className="text-sm text-danger">
          {t("actionError")}
        </p>
      )}
    </div>
  );
}
