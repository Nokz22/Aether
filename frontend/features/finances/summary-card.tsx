"use client";

import { useLocale, useTranslations } from "next-intl";
import { cn } from "@/lib/cn";
import { formatCents } from "@/lib/money";
import type { TransactionSummary } from "@/lib/api";

const TOP_CATEGORIES = 5;

export function SummaryCard({ summary }: { summary: TransactionSummary }) {
  const t = useTranslations("finances");
  const locale = useLocale();

  const expensesByCategory = summary.byCategory
    .filter((entry) => entry.type === "EXPENSE")
    .slice(0, TOP_CATEGORIES);
  const largest = expensesByCategory[0]?.totalCents ?? 0;

  return (
    <section className="surface-card flex flex-col gap-5 p-6">
      <div className="flex flex-wrap gap-x-10 gap-y-4">
        <Figure label={t("income")} value={formatCents(summary.incomeCents, locale)} />
        <Figure label={t("expense")} value={formatCents(summary.expenseCents, locale)} />
        <Figure
          label={t("net")}
          value={formatCents(summary.netCents, locale)}
          className={summary.netCents < 0 ? "text-danger" : "text-accent"}
          emphasis
        />
      </div>

      {expensesByCategory.length > 0 && (
        <div className="flex flex-col gap-2">
          <h3 className="text-xs font-medium uppercase tracking-wide text-muted">
            {t("byCategory")}
          </h3>
          <ul className="flex flex-col gap-2">
            {expensesByCategory.map((entry) => (
              <li key={`${entry.category}-${entry.type}`} className="flex items-center gap-3">
                <span className="w-28 shrink-0 truncate text-sm">
                  {entry.category || t("uncategorized")}
                </span>
                <span className="h-2 min-w-0 flex-1 overflow-hidden rounded-full bg-border-glass">
                  <span
                    className="block h-full rounded-full bg-accent"
                    style={{
                      width: largest > 0 ? `${(entry.totalCents / largest) * 100}%` : "0%",
                    }}
                  />
                </span>
                <span className="w-24 shrink-0 text-right text-sm text-muted">
                  {formatCents(entry.totalCents, locale)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function Figure({
  label,
  value,
  className,
  emphasis = false,
}: {
  label: string;
  value: string;
  className?: string;
  emphasis?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs uppercase tracking-wide text-muted">{label}</span>
      <span className={cn(emphasis ? "text-2xl font-medium" : "text-lg", className)}>{value}</span>
    </div>
  );
}
