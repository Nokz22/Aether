"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TrashIcon } from "@/components/ui/icons";
import { toDateTimeLocalValue } from "@/lib/dates";
import type { EventResponse } from "@/lib/api";
import { useEventMutations } from "./use-calendar";

type EventDialogProps = {
  event: EventResponse | null; // null → create
  defaultStart: Date;
  defaultEnd: Date;
  onClose: () => void;
};

export function EventDialog({ event, defaultStart, defaultEnd, onClose }: EventDialogProps) {
  const t = useTranslations("calendar");
  const { create, update, remove } = useEventMutations();
  const titleId = useId();

  const [title, setTitle] = useState(event?.title ?? "");
  const [description, setDescription] = useState(event?.description ?? "");
  const [start, setStart] = useState(
    toDateTimeLocalValue(event ? new Date(event.startsAt) : defaultStart),
  );
  const [end, setEnd] = useState(
    toDateTimeLocalValue(event ? new Date(event.endsAt) : defaultEnd),
  );
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const busy = create.isPending || update.isPending || remove.isPending;

  async function save() {
    const startsAt = new Date(start);
    const endsAt = new Date(end);
    if (!title.trim()) {
      setError(t("titleRequired"));
      return;
    }
    if (!(endsAt.getTime() > startsAt.getTime())) {
      setError(t("endBeforeStart"));
      return;
    }
    const body = {
      title: title.trim(),
      description: description.trim(),
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
    };
    try {
      if (event) {
        await update.mutateAsync({ id: event.id, body });
      } else {
        await create.mutateAsync(body);
      }
      onClose();
    } catch {
      setError(t("saveError"));
    }
  }

  async function handleDelete() {
    if (!event) return;
    try {
      await remove.mutateAsync(event.id);
      onClose();
    } catch {
      setError(t("saveError"));
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="absolute inset-0 bg-black/50" aria-hidden />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="surface-card relative z-10 flex w-full max-w-md flex-col gap-4 p-6"
      >
        <h2 id={titleId} className="text-lg font-medium">
          {event ? t("editTitle") : t("createTitle")}
        </h2>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="text-muted">{t("eventTitle")}</span>
          <Input
            autoFocus
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder={t("eventTitlePlaceholder")}
            maxLength={300}
          />
        </label>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="text-muted">{t("description")}</span>
          <Input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t("descriptionPlaceholder")}
            maxLength={2000}
          />
        </label>

        <div className="flex gap-3">
          <label className="flex flex-1 flex-col gap-1.5 text-sm">
            <span className="text-muted">{t("start")}</span>
            <Input type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} />
          </label>
          <label className="flex flex-1 flex-col gap-1.5 text-sm">
            <span className="text-muted">{t("end")}</span>
            <Input type="datetime-local" value={end} onChange={(e) => setEnd(e.target.value)} />
          </label>
        </div>

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}

        <div className="mt-1 flex items-center gap-3">
          <Button className="flex-1" loading={busy} onClick={save}>
            {t("save")}
          </Button>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="rounded-md px-4 py-2 text-sm text-muted transition-colors duration-150 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)] disabled:opacity-60"
          >
            {t("cancel")}
          </button>
          {event && (
            <button
              type="button"
              aria-label={t("delete")}
              title={t("delete")}
              onClick={handleDelete}
              disabled={busy}
              className="flex size-10 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-danger/10 hover:text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)] disabled:opacity-60"
            >
              <TrashIcon width={17} height={17} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
