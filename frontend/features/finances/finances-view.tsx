"use client";

import { useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ChevronLeftIcon, ChevronRightIcon, PlusIcon } from "@/components/ui/icons";
import { cn } from "@/lib/cn";
import { addMonths, endOfMonth, localDateISO, startOfMonth } from "@/lib/dates";
import { formatCents } from "@/lib/money";
import type { TransactionResponse } from "@/lib/api";
import { SummaryCard } from "./summary-card";
import { TransactionDialog } from "./transaction-dialog";
import { useTransactionSummary, useTransactions } from "./use-finances";

type DialogState = { transaction: TransactionResponse | null };

export function FinancesView() {
  const t = useTranslations("finances");
  const locale = useLocale();

  const [month, setMonth] = useState(() => startOfMonth(new Date()));
  const [dialog, setDialog] = useState<DialogState | null>(null);

  const from = localDateISO(month);
  const to = localDateISO(endOfMonth(month));

  const transactions = useTransactions(from, to);
  const summary = useTransactionSummary(from, to);

  // New entries default to today when we're on the current month, else day one.
  const today = new Date();
  const defaultDate =
    today >= month && today <= endOfMonth(month) ? localDateISO(today) : from;

  const monthLabel = month.toLocaleDateString(locale, { month: "long", year: "numeric" });

  return (
    <div className="flex h-full flex-col gap-5">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-medium tracking-tight">{t("title")}</h1>
          <span className="text-sm text-muted first-letter:uppercase">{monthLabel}</span>
        </div>
        <div className="flex items-center gap-2">
          <NavButton label={t("prevMonth")} onClick={() => setMonth(addMonths(month, -1))}>
            <ChevronLeftIcon width={16} height={16} />
          </NavButton>
          <button
            type="button"
            onClick={() => setMonth(startOfMonth(new Date()))}
            className="whitespace-nowrap rounded-md px-3 py-1.5 text-sm text-muted transition-colors duration-150 hover:bg-accent/10 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            {t("thisMonth")}
          </button>
          <NavButton label={t("nextMonth")} onClick={() => setMonth(addMonths(month, 1))}>
            <ChevronRightIcon width={16} height={16} />
          </NavButton>
          <Button
            className="ml-1 h-9 w-auto gap-1.5 px-3 text-sm"
            onClick={() => setDialog({ transaction: null })}
          >
            <PlusIcon width={16} height={16} />
            {t("newTransaction")}
          </Button>
        </div>
      </header>

      {transactions.isError || summary.isError ? (
        <div className="flex flex-col items-start gap-3">
          <p role="alert" className="text-danger">
            {t("loadError")}
          </p>
          <Button
            className="w-auto px-4"
            onClick={() => {
              transactions.refetch();
              summary.refetch();
            }}
          >
            {t("retry")}
          </Button>
        </div>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col gap-5">
          {summary.isPending ? (
            <div className="surface-card h-40 animate-pulse" aria-busy />
          ) : (
            <SummaryCard summary={summary.data} />
          )}

          {transactions.isPending ? (
            <div className="h-40 animate-pulse rounded-md bg-border-glass" aria-busy />
          ) : transactions.data.length === 0 ? (
            <p className="text-muted">{t("empty")}</p>
          ) : (
            <ul className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
              {transactions.data.map((transaction) => (
                <li key={transaction.id}>
                  <button
                    type="button"
                    onClick={() => setDialog({ transaction })}
                    className="flex w-full items-center gap-4 rounded-md px-3 py-2.5 text-left transition-colors duration-150 hover:bg-accent/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
                  >
                    <div className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate text-sm">
                        {transaction.description || transaction.category || t("uncategorized")}
                      </span>
                      <span className="truncate text-xs text-muted">
                        {new Date(`${transaction.date}T00:00:00`).toLocaleDateString(locale, {
                          day: "numeric",
                          month: "short",
                        })}
                        {transaction.description && transaction.category
                          ? ` · ${transaction.category}`
                          : ""}
                      </span>
                    </div>
                    <span
                      className={cn(
                        "shrink-0 text-sm tabular-nums",
                        transaction.type === "INCOME" ? "text-accent" : "text-foreground",
                      )}
                    >
                      {transaction.type === "INCOME" ? "+" : "−"}
                      {formatCents(transaction.amountCents, locale)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {dialog && (
        <TransactionDialog
          transaction={dialog.transaction}
          defaultDate={defaultDate}
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
