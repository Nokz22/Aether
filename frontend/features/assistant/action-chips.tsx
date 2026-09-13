"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";

/**
 * Where each action the assistant can take lives, so a reply that changed
 * something links straight to it. Read-only tools are absent on purpose:
 * looking something up is not an action worth reporting.
 */
const ACTIONS: Record<string, { href: string; key: string }> = {
  create_habit: { href: "/habits", key: "createHabit" },
  check_in_habit: { href: "/habits", key: "checkInHabit" },
  create_note: { href: "/notes", key: "createNote" },
  create_project: { href: "/projects", key: "createProject" },
  add_task: { href: "/projects", key: "addTask" },
  move_task: { href: "/projects", key: "moveTask" },
  create_event: { href: "/calendar", key: "createEvent" },
  add_transaction: { href: "/finances", key: "addTransaction" },
};

export function ActionChips({ tools }: { tools: string[] }) {
  const t = useTranslations("assistant.actions");
  const actions = tools.filter((tool) => tool in ACTIONS);

  if (actions.length === 0) {
    return null;
  }

  return (
    <ul className="mt-2.5 flex flex-wrap gap-1.5">
      {actions.map((tool) => {
        const action = ACTIONS[tool];
        return (
          <li key={tool}>
            <Link
              href={action.href}
              className="inline-flex items-center rounded-full border border-border-glass px-2.5 py-1 text-xs text-muted transition-colors duration-150 hover:border-accent/40 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
            >
              {t(action.key)}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
