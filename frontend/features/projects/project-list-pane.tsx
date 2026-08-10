"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/cn";
import type { ProjectSummary } from "@/lib/api";

type ProjectListPaneProps = {
  projects: ProjectSummary[];
  selectedId: string | null;
  creating: boolean;
  onSelect: (id: string) => void;
  onCreate: (name: string) => void;
};

export function ProjectListPane({
  projects,
  selectedId,
  creating,
  onSelect,
  onCreate,
}: ProjectListPaneProps) {
  const t = useTranslations("projects");

  return (
    <div className="flex h-full flex-col gap-3">
      <h1 className="px-2 text-lg font-medium tracking-tight">{t("title")}</h1>

      {projects.length === 0 ? (
        <p className="px-2 text-sm text-muted">{t("empty")}</p>
      ) : (
        <ul className="flex flex-col gap-1 overflow-y-auto">
          {projects.map((project) => {
            const total = project.taskCounts.todo + project.taskCounts.doing + project.taskCounts.done;
            const active = project.id === selectedId;
            return (
              <li key={project.id}>
                <button
                  type="button"
                  onClick={() => onSelect(project.id)}
                  aria-current={active ? "true" : undefined}
                  className={cn(
                    "flex w-full flex-col gap-1 rounded-md px-3 py-2 text-left",
                    "transition-colors duration-150",
                    "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
                    active ? "bg-accent/15" : "hover:bg-accent/10",
                  )}
                >
                  <span className="truncate text-sm font-medium">{project.name}</span>
                  <span className="text-xs text-muted">
                    {t("progress", { done: project.taskCounts.done, total })}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <form
        className="mt-auto"
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
          placeholder={t("newProject")}
          aria-label={t("newProject")}
          maxLength={200}
          disabled={creating}
          className="h-9 text-sm"
        />
      </form>
    </div>
  );
}
