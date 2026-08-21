"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/cn";
import {
  CalendarIcon,
  FolderIcon,
  HomeIcon,
  NoteIcon,
  RepeatIcon,
  WalletIcon,
} from "@/components/ui/icons";

const MODULES = [
  { key: "home", href: "/", Icon: HomeIcon },
  { key: "projects", href: "/projects", Icon: FolderIcon },
  { key: "calendar", href: "/calendar", Icon: CalendarIcon },
  { key: "habits", href: "/habits", Icon: RepeatIcon },
  { key: "finances", href: null, Icon: WalletIcon },
  { key: "notes", href: "/notes", Icon: NoteIcon },
] as const;

export function Dock() {
  const t = useTranslations("shell");
  const pathname = usePathname();

  return (
    <nav
      aria-label={t("navigation")}
      className="flex w-16 flex-col items-center gap-2 border-r border-border-glass py-4"
    >
      <p className="mb-4 text-[10px] font-semibold tracking-[0.2em]" aria-hidden>
        AE
      </p>
      {MODULES.map(({ key, href, Icon }) =>
        href ? (
          <Link
            key={key}
            href={href}
            aria-label={t(key)}
            aria-current={pathname === href ? "page" : undefined}
            className={cn(
              "flex size-10 items-center justify-center rounded-md",
              "transition-colors duration-150",
              "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
              pathname === href
                ? "bg-accent/15 text-foreground"
                : "text-muted hover:bg-accent/10 hover:text-foreground",
            )}
          >
            <Icon />
          </Link>
        ) : (
          <span
            key={key}
            role="img"
            aria-label={`${t(key)} — ${t("soon")}`}
            title={`${t(key)} — ${t("soon")}`}
            className="flex size-10 cursor-not-allowed items-center justify-center rounded-md text-muted opacity-40"
          >
            <Icon />
          </span>
        ),
      )}
    </nav>
  );
}
