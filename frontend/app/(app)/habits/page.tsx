import { getTranslations } from "next-intl/server";
import { HabitsList } from "@/features/habits/habits-list";

export default async function HabitsPage() {
  const t = await getTranslations("habits");

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-medium tracking-tight">{t("title")}</h1>
      <HabitsList />
    </div>
  );
}
