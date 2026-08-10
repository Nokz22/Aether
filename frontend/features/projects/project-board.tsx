"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { TrashIcon } from "@/components/ui/icons";
import { Input } from "@/components/ui/input";
import type { ProjectResponse, TaskResponse, TaskStatus } from "@/lib/api";
import { TaskCard } from "./task-card";
import { useProject, useProjectMutations } from "./use-projects";

const COLUMNS: { status: TaskStatus; key: "todo" | "doing" | "done" }[] = [
  { status: "TODO", key: "todo" },
  { status: "DOING", key: "doing" },
  { status: "DONE", key: "done" },
];

export function ProjectBoard({
  projectId,
  onDeleted,
}: {
  projectId: string;
  onDeleted: () => void;
}) {
  const t = useTranslations("projects");
  const project = useProject(projectId);

  if (project.isPending) {
    return <div className="h-full animate-pulse rounded-md bg-border-glass" aria-busy />;
  }
  if (project.isError || !project.data) {
    return (
      <div className="flex h-full items-center justify-center">
        <p role="alert" className="text-danger">
          {t("loadError")}
        </p>
      </div>
    );
  }
  return <Board key={project.data.id} project={project.data} onDeleted={onDeleted} />;
}

function Board({ project, onDeleted }: { project: ProjectResponse; onDeleted: () => void }) {
  const t = useTranslations("projects");
  const { updateProject, removeProject, addTask, updateTask, removeTask } =
    useProjectMutations(project.id);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const busy = updateTask.isPending || removeTask.isPending;

  function saveName(value: string) {
    const name = value.trim();
    if (name && name !== project.name) {
      updateProject.mutate({ id: project.id, body: { name } });
    }
  }

  function saveDescription(value: string) {
    if (value !== project.description) {
      updateProject.mutate({ id: project.id, body: { description: value } });
    }
  }

  return (
    <div className="flex h-full flex-col gap-5">
      <header className="flex items-start gap-3">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <Input
            defaultValue={project.name}
            aria-label={t("projectName")}
            maxLength={200}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                event.currentTarget.blur();
              }
            }}
            onBlur={(event) => saveName(event.currentTarget.value)}
            className="h-9 border-transparent px-0 text-xl font-medium hover:border-transparent"
          />
          <Input
            defaultValue={project.description}
            placeholder={t("descriptionPlaceholder")}
            aria-label={t("projectDescription")}
            maxLength={2000}
            onBlur={(event) => saveDescription(event.currentTarget.value)}
            className="h-8 border-transparent px-0 text-sm text-muted hover:border-transparent"
          />
        </div>
        {confirmingDelete ? (
          <div className="flex shrink-0 items-center gap-2 text-sm">
            <span className="text-muted">{t("confirmDeleteProject")}</span>
            <button
              type="button"
              onClick={() => {
                removeProject.mutate(project.id, { onSuccess: onDeleted });
              }}
              className="font-medium text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t("confirmYes")}
            </button>
            <button
              type="button"
              onClick={() => setConfirmingDelete(false)}
              className="text-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t("confirmNo")}
            </button>
          </div>
        ) : (
          <button
            type="button"
            aria-label={t("deleteProject")}
            title={t("deleteProject")}
            onClick={() => setConfirmingDelete(true)}
            className="flex size-9 shrink-0 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-danger/10 hover:text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
          >
            <TrashIcon width={17} height={17} />
          </button>
        )}
      </header>

      <div className="grid min-h-0 flex-1 gap-4 sm:grid-cols-3">
        {COLUMNS.map((column) => {
          const tasks = project.tasks.filter((task) => task.status === column.status);
          return (
            <section
              key={column.status}
              aria-label={t(`status.${column.key}`)}
              className="flex min-h-0 flex-col gap-3"
            >
              <div className="flex items-center justify-between px-1">
                <h3 className="text-sm font-medium">{t(`status.${column.key}`)}</h3>
                <span className="text-xs text-muted">{tasks.length}</span>
              </div>
              <div className="flex flex-col gap-2 overflow-y-auto">
                {tasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    busy={busy}
                    onMove={(target, status) =>
                      updateTask.mutate({ id: project.id, taskId: target.id, body: { status } })
                    }
                    onRename={(target, title) =>
                      updateTask.mutate({ id: project.id, taskId: target.id, body: { title } })
                    }
                    onDelete={(target: TaskResponse) =>
                      removeTask.mutate({ id: project.id, taskId: target.id })
                    }
                  />
                ))}
                {column.status === "TODO" && (
                  <AddTaskForm
                    creating={addTask.isPending}
                    onAdd={(title) => addTask.mutate({ id: project.id, title })}
                  />
                )}
              </div>
            </section>
          );
        })}
      </div>

      {(updateTask.isError || removeTask.isError || addTask.isError || updateProject.isError) && (
        <p role="alert" className="text-sm text-danger">
          {t("actionError")}
        </p>
      )}
    </div>
  );
}

function AddTaskForm({ creating, onAdd }: { creating: boolean; onAdd: (title: string) => void }) {
  const t = useTranslations("projects");
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const title = String(new FormData(form).get("title") ?? "").trim();
        if (title) {
          onAdd(title);
          form.reset();
        }
      }}
    >
      <Input
        name="title"
        placeholder={t("newTask")}
        aria-label={t("newTask")}
        maxLength={300}
        disabled={creating}
        className="h-9 text-sm"
      />
    </form>
  );
}
