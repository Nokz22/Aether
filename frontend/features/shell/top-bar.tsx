"use client";

import { useTranslations } from "next-intl";
import { LocaleSwitcher } from "@/components/ui/locale-switcher";
import { LogoutIcon } from "@/components/ui/icons";
import { useSession } from "@/features/auth/session-provider";

export function TopBar() {
  const t = useTranslations("shell");
  const { user, logout } = useSession();

  return (
    <header className="flex h-14 items-center justify-end gap-4 border-b border-border-glass px-6">
      <LocaleSwitcher />
      <p className="text-sm text-muted">{user.displayName}</p>
      <button
        type="button"
        onClick={logout}
        aria-label={t("logout")}
        title={t("logout")}
        className="flex size-9 items-center justify-center rounded-md text-muted transition-colors duration-150 hover:bg-accent/10 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
      >
        <LogoutIcon />
      </button>
    </header>
  );
}
