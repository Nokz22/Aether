"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";

type NewHabitFormProps = {
  onCreate: (name: string) => void;
  creating: boolean;
};

export function NewHabitForm({ onCreate, creating }: NewHabitFormProps) {
  const t = useTranslations("habits");

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const name = String(new FormData(form).get("name") ?? "").trim();
        if (name) {
          onCreate(name);
          form.reset();
        }
      }}
    >
      <Input
        name="name"
        placeholder={t("newPlaceholder")}
        aria-label={t("newPlaceholder")}
        maxLength={100}
        required
        disabled={creating}
      />
    </form>
  );
}
