"use client";

import { useTranslations } from "next-intl";
import { useSession } from "@/features/auth/session-provider";

function greetingKey(hour: number): "greetingMorning" | "greetingAfternoon" | "greetingEvening" {
  if (hour >= 6 && hour < 13) return "greetingMorning";
  if (hour >= 13 && hour < 20) return "greetingAfternoon";
  return "greetingEvening";
}

export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const { user } = useSession();

  return (
    <div className="flex h-full flex-col">
      <h1 className="text-2xl font-medium tracking-tight">
        {t(greetingKey(new Date().getHours()), { name: user.displayName })}
      </h1>
      <p className="mt-2 text-muted">{t("empty")}</p>
    </div>
  );
}
