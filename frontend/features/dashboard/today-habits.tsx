"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { CheckIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import { useHabits } from "@/features/habits/use-habits";
import type { HabitResponse } from "@/lib/api";

function ProgressRing({ done, total }: { done: number; total: number }) {
  const size = 76;
  const stroke = 6;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const fraction = total === 0 ? 0 : done / total;
  const center = size / 2;

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden>
      <circle
        cx={center}
        cy={center}
        r={radius}
        fill="none"
        stroke="var(--border-glass)"
        strokeWidth={stroke}
      />
      <circle
        cx={center}
        cy={center}
        r={radius}
        fill="none"
        stroke="var(--accent)"
        strokeWidth={stroke}
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={circumference * (1 - fraction)}
        transform={`rotate(-90 ${center} ${center})`}
        style={{ transition: "stroke-dashoffset var(--duration-slow) var(--ease)" }}
      />
      <text
        x="50%"
        y="50%"
        textAnchor="middle"
        dominantBaseline="central"
        fill="var(--foreground)"
        style={{ fontSize: 16, fontWeight: 500 }}
      >
        {done}/{total}
      </text>
    </svg>
  );
}

function HabitChip({
  habit,
  onToggle,
  disabled,
}: {
  habit: HabitResponse;
  onToggle: (habit: HabitResponse) => void;
  disabled: boolean;
}) {
  const t = useTranslations("habits");

  return (
    <button
      type="button"
      aria-pressed={habit.doneToday}
      aria-label={t(habit.doneToday ? "markUndone" : "markDone", { name: habit.name })}
      disabled={disabled}
      onClick={() => onToggle(habit)}
      className={cn(
        "inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm",
        "transition-colors duration-150",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
        "disabled:cursor-not-allowed",
        habit.doneToday
          ? "border-transparent bg-accent/15 text-muted"
          : "border-border-glass hover:border-[var(--muted)]",
      )}
    >
      <span
        aria-hidden
        className={cn(
          "flex size-4 items-center justify-center rounded-full",
          habit.doneToday ? "bg-accent text-accent-foreground" : "border border-[var(--muted)]",
        )}
      >
        {habit.doneToday && <CheckIcon width={11} height={11} />}
      </span>
      <span className="max-w-[12rem] truncate">{habit.name}</span>
    </button>
  );
}

export function TodayHabits() {
  const t = useTranslations("dashboard");
  const { habits, toggleToday } = useHabits();

  if (habits.isPending) {
    return <div className="surface-card h-32 animate-pulse" aria-busy />;
  }

  if (habits.isError) {
    return (
      <section className="surface-card flex flex-col items-start gap-3 p-6">
        <p role="alert" className="text-danger">
          {t("loadError")}
        </p>
        <button
          type="button"
          onClick={() => habits.refetch()}
          className="rounded-md px-3 py-1.5 text-sm text-accent hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
        >
          {t("retry")}
        </button>
      </section>
    );
  }

  const data = habits.data;

  if (data.length === 0) {
    return (
      <section className="surface-card flex flex-col items-start gap-3 p-6">
        <h2 className="text-sm font-medium tracking-wide">{t("todayTitle")}</h2>
        <p className="text-muted">{t("todayEmpty")}</p>
        <Link
          href="/habits"
          className="text-sm text-accent hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
        >
          {t("createHabit")}
        </Link>
      </section>
    );
  }

  const done = data.filter((habit) => habit.doneToday).length;
  const total = data.length;
  const allDone = done === total;

  return (
    <section className="surface-card flex flex-col gap-5 p-6">
      <h2 className="text-sm font-medium tracking-wide">{t("todayTitle")}</h2>

      <div className="flex items-center gap-5">
        <ProgressRing done={done} total={total} />
        <div className="flex min-w-0 flex-col gap-3">
          <p className={cn("text-sm", allDone ? "text-foreground" : "text-muted")}>
            {allDone ? t("allDone") : t("progress", { done, total })}
          </p>
          <div className="flex flex-wrap gap-2">
            {data.map((habit) => (
              <HabitChip
                key={habit.id}
                habit={habit}
                onToggle={(target) => toggleToday.mutate(target)}
                disabled={toggleToday.isPending}
              />
            ))}
          </div>
        </div>
      </div>

      {toggleToday.isError && (
        <p role="alert" className="text-sm text-danger">
          {t("actionError")}
        </p>
      )}

      <Link
        href="/habits"
        className="text-sm text-muted transition-colors duration-150 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
      >
        {t("viewHabits")}
      </Link>
    </section>
  );
}
