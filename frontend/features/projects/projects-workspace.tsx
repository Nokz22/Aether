"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";
import { projectsApi } from "@/lib/api";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSession } from "@/features/auth/session-provider";
import { ProjectBoard } from "./project-board";
import { ProjectListPane } from "./project-list-pane";
import { useProjectsList } from "./use-projects";

export function ProjectsWorkspace() {
  const t = useTranslations("projects");
  const { accessToken } = useSession();
  const queryClient = useQueryClient();
  const projects = useProjectsList();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const createProject = useMutation({
    mutationFn: (name: string) => projectsApi.create(name, accessToken),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      setSelectedId(project.id);
    },
  });

  if (projects.isPending) {
    return <div className="h-full animate-pulse rounded-md bg-border-glass" aria-busy />;
  }

  if (projects.isError) {
    return (
      <div className="flex h-full flex-col items-start gap-3">
        <p role="alert" className="text-danger">
          {t("listError")}
        </p>
        <Button className="w-auto px-4" onClick={() => projects.refetch()}>
          {t("retry")}
        </Button>
      </div>
    );
  }

  const hasBoard = selectedId !== null;

  return (
    <div className="flex h-full">
      <div
        className={cn(
          "w-full flex-col md:flex md:w-72 md:shrink-0 md:border-r md:border-border-glass md:pr-4",
          hasBoard ? "hidden" : "flex",
        )}
      >
        <ProjectListPane
          projects={projects.data}
          selectedId={selectedId}
          creating={createProject.isPending}
          onSelect={setSelectedId}
          onCreate={(name) => createProject.mutate(name)}
        />
      </div>

      <div className={cn("min-w-0 flex-1 flex-col md:flex md:pl-6", hasBoard ? "flex" : "hidden")}>
        {hasBoard ? (
          <>
            <button
              type="button"
              onClick={() => setSelectedId(null)}
              className="mb-4 self-start text-sm text-muted hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)] md:hidden"
            >
              ← {t("back")}
            </button>
            <div className="min-h-0 flex-1">
              <ProjectBoard projectId={selectedId} onDeleted={() => setSelectedId(null)} />
            </div>
          </>
        ) : (
          <div className="hidden h-full items-center justify-center md:flex">
            <p className="text-muted">{t("noSelection")}</p>
          </div>
        )}
      </div>
    </div>
  );
}
