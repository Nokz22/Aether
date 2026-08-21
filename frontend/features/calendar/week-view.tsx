"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ChevronLeftIcon, ChevronRightIcon, PlusIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import { addDays, isSameDay, startOfDay, startOfWeek } from "@/lib/dates";
import type { EventResponse } from "@/lib/api";
import { EventDialog } from "./event-dialog";
import { layoutDay } from "./layout";
import { useEventsInRange } from "./use-calendar";

const HOUR_HEIGHT = 48;
const HOURS = 24;
const GRID_HEIGHT = HOUR_HEIGHT * HOURS;

type DialogState = { event: EventResponse | null; start: Date; end: Date };

export function WeekView() {
  const t = useTranslations("calendar");
  const locale = useLocale();
  const scrollRef = useRef<HTMLDivElement>(null);

  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [dialog, setDialog] = useState<DialogState | null>(null);

  const rangeFrom = weekStart;
  const rangeTo = addDays(weekStart, 7);
  const days = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart],
  );

  const events = useEventsInRange(rangeFrom.toISOString(), rangeTo.toISOString());

  // Open on the morning rather than midnight. Wait a frame so the grid has been
  // laid out and the container is actually scrollable.
  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      if (scrollRef.current) scrollRef.current.scrollTop = 7 * HOUR_HEIGHT;
    });
    return () => cancelAnimationFrame(frame);
  }, []);

  const now = new Date();

  function openCreateAt(day: Date, hour: number) {
    const start = new Date(day);
    start.setHours(hour, 0, 0, 0);
    setDialog({ event: null, start, end: new Date(start.getTime() + 3_600_000) });
  }

  function openCreateDefault() {
    const inWeek = now >= rangeFrom && now < rangeTo;
    const start = inWeek ? startOfDay(now) : new Date(weekStart);
    start.setHours(inWeek ? Math.min(now.getHours() + 1, 23) : 9, 0, 0, 0);
    setDialog({ event: null, start, end: new Date(start.getTime() + 3_600_000) });
  }

  const rangeLabel = `${fmtDay(days[0], locale)} – ${fmtDay(days[6], locale)}`;

  return (
    <div className="flex h-full flex-col gap-4">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-medium tracking-tight">{t("title")}</h1>
          <span className="text-sm text-muted">{rangeLabel}</span>
        </div>
        <div className="flex items-center gap-2">
          <NavButton label={t("prevWeek")} onClick={() => setWeekStart(addDays(weekStart, -7))}>
            <ChevronLeftIcon width={16} height={16} />
          </NavButton>
          <button
            type="button"
            onClick={() => setWeekStart(startOfWeek(new Date()))}
            className="rounded-md px-3 py-1.5 text-sm text-muted transition-colors duration-150 hover:bg-accent/10 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            {t("today")}
          </button>
          <NavButton label={t("nextWeek")} onClick={() => setWeekStart(addDays(weekStart, 7))}>
            <ChevronRightIcon width={16} height={16} />
          </NavButton>
          <Button className="ml-1 h-9 w-auto gap-1.5 px-3 text-sm" onClick={openCreateDefault}>
            <PlusIcon width={16} height={16} />
            {t("newEvent")}
          </Button>
        </div>
      </header>

      {events.isError ? (
        <div className="flex flex-1 flex-col items-start gap-3">
          <p role="alert" className="text-danger">
            {t("loadError")}
          </p>
          <Button className="w-auto px-4" onClick={() => events.refetch()}>
            {t("retry")}
          </Button>
        </div>
      ) : (
        <div ref={scrollRef} className="min-h-0 flex-1 overflow-auto rounded-md border border-border-glass">
          <div className="min-w-[760px]">
            {/* Weekday header, sticky so it stays while scrolling the hours. */}
            <div className="sticky top-0 z-10 flex border-b border-border-glass bg-[var(--background)]">
              <div className="w-14 shrink-0" />
              {days.map((day) => {
                const today = isSameDay(day, now);
                return (
                  <div
                    key={day.toISOString()}
                    className="flex flex-1 items-baseline justify-center gap-1.5 py-2"
                  >
                    <span className={cn("text-xs uppercase", today ? "text-accent" : "text-muted")}>
                      {day.toLocaleDateString(locale, { weekday: "short" })}
                    </span>
                    <span
                      className={cn(
                        "text-sm",
                        today ? "font-semibold text-accent" : "text-foreground",
                      )}
                    >
                      {day.getDate()}
                    </span>
                  </div>
                );
              })}
            </div>

            {/* Hour grid. */}
            <div className="flex" style={{ height: GRID_HEIGHT }}>
              <div className="relative w-14 shrink-0">
                {Array.from({ length: HOURS }, (_, hour) => (
                  <div
                    key={hour}
                    className="absolute right-2 -translate-y-1/2 text-[11px] text-muted"
                    style={{ top: hour * HOUR_HEIGHT }}
                  >
                    {hour === 0 ? "" : `${String(hour).padStart(2, "0")}:00`}
                  </div>
                ))}
              </div>

              {days.map((day) => {
                const placed = layoutDay(events.data ?? [], day);
                const showNow = isSameDay(day, now);
                const nowTop = ((now.getHours() * 60 + now.getMinutes()) / 1440) * GRID_HEIGHT;
                return (
                  <div key={day.toISOString()} className="relative flex-1 border-l border-border-glass">
                    {Array.from({ length: HOURS }, (_, hour) => (
                      <button
                        key={hour}
                        type="button"
                        aria-label={t("createAt", {
                          day: day.toLocaleDateString(locale, { weekday: "long" }),
                          hour: `${String(hour).padStart(2, "0")}:00`,
                        })}
                        onClick={() => openCreateAt(day, hour)}
                        className="block w-full border-t border-border-glass/60 hover:bg-accent/5 focus-visible:bg-accent/10 focus-visible:outline-none"
                        style={{ height: HOUR_HEIGHT }}
                      />
                    ))}

                    {placed.map(({ event, top, height, lane, laneCount }) => (
                      <button
                        key={event.id}
                        type="button"
                        onClick={() =>
                          setDialog({
                            event,
                            start: new Date(event.startsAt),
                            end: new Date(event.endsAt),
                          })
                        }
                        className="absolute overflow-hidden rounded-md border-l-2 border-accent bg-accent/15 px-1.5 py-1 text-left transition-colors duration-150 hover:bg-accent/25 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--focus-ring)]"
                        style={{
                          top: top * GRID_HEIGHT,
                          height: Math.max(height * GRID_HEIGHT, 18),
                          left: `calc(${(lane / laneCount) * 100}% + 2px)`,
                          width: `calc(${100 / laneCount}% - 4px)`,
                        }}
                      >
                        <span className="block truncate text-[11px] text-muted">
                          {new Date(event.startsAt).toLocaleTimeString(locale, {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                        <span className="block truncate text-xs font-medium">{event.title}</span>
                      </button>
                    ))}

                    {showNow && (
                      <div
                        aria-hidden
                        className="pointer-events-none absolute left-0 right-0 z-20 border-t border-danger"
                        style={{ top: nowTop }}
                      >
                        <span className="absolute -left-1 -top-1 size-2 rounded-full bg-danger" />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {dialog && (
        <EventDialog
          event={dialog.event}
          defaultStart={dialog.start}
          defaultEnd={dialog.end}
          onClose={() => setDialog(null)}
        />
      )}
    </div>
  );
}

function NavButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="flex size-9 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-accent/10 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
    >
      {children}
    </button>
  );
}

function fmtDay(day: Date, locale: string): string {
  return day.toLocaleDateString(locale, { day: "numeric", month: "short" });
}
