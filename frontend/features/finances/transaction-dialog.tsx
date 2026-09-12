"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { TrashIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import { centsToInputValue, inputValueToCents } from "@/lib/money";
import type { TransactionResponse, TransactionType } from "@/lib/api";
import { useTransactionMutations } from "./use-finances";

type TransactionDialogProps = {
  transaction: TransactionResponse | null; // null → create
  defaultDate: string; // YYYY-MM-DD
  onClose: () => void;
};

export function TransactionDialog({
  transaction,
  defaultDate,
  onClose,
}: TransactionDialogProps) {
  const t = useTranslations("finances");
  const { create, update, remove } = useTransactionMutations();
  const titleId = useId();

  const [type, setType] = useState<TransactionType>(transaction?.type ?? "EXPENSE");
  const [amount, setAmount] = useState(
    transaction ? centsToInputValue(transaction.amountCents) : "",
  );
  const [category, setCategory] = useState(transaction?.category ?? "");
  const [description, setDescription] = useState(transaction?.description ?? "");
  const [date, setDate] = useState(transaction?.date ?? defaultDate);
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
    const amountCents = inputValueToCents(amount);
    if (amountCents === null) {
      setError(t("amountInvalid"));
      return;
    }
    const body = {
      type,
      amountCents,
      category: category.trim(),
      description: description.trim(),
      date,
    };
    try {
      if (transaction) {
        await update.mutateAsync({ id: transaction.id, body });
      } else {
        await create.mutateAsync(body);
      }
      onClose();
    } catch {
      setError(t("saveError"));
    }
  }

  async function handleDelete() {
    if (!transaction) return;
    try {
      await remove.mutateAsync(transaction.id);
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
          {transaction ? t("editTitle") : t("createTitle")}
        </h2>

        <div className="flex gap-2" role="group" aria-label={t("type")}>
          {(["EXPENSE", "INCOME"] as const).map((option) => (
            <button
              key={option}
              type="button"
              aria-pressed={type === option}
              onClick={() => setType(option)}
              className={cn(
                "flex-1 rounded-md border px-3 py-2 text-sm transition-colors duration-150",
                "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
                type === option
                  ? "border-transparent bg-accent/20 font-medium text-foreground"
                  : "border-border-glass text-muted hover:border-[var(--muted)]",
              )}
            >
              {t(option === "EXPENSE" ? "expense" : "income")}
            </button>
          ))}
        </div>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="text-muted">{t("amount")}</span>
          <Input
            autoFocus
            type="number"
            inputMode="decimal"
            step="0.01"
            min="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00"
          />
        </label>

        <div className="flex gap-3">
          <label className="flex flex-1 flex-col gap-1.5 text-sm">
            <span className="text-muted">{t("category")}</span>
            <Input
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder={t("categoryPlaceholder")}
              maxLength={100}
            />
          </label>
          <label className="flex flex-1 flex-col gap-1.5 text-sm">
            <span className="text-muted">{t("date")}</span>
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
        </div>

        <label className="flex flex-col gap-1.5 text-sm">
          <span className="text-muted">{t("description")}</span>
          <Input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t("descriptionPlaceholder")}
            maxLength={500}
          />
        </label>

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
          {transaction && (
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
