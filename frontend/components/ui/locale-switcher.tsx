"use client";

import { useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { cn } from "@/lib/cn";
import { LOCALES, LOCALE_COOKIE, type Locale } from "@/i18n/locales";

function persistLocale(locale: Locale) {
  document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=31536000; samesite=lax`;
}

export function LocaleSwitcher() {
  const router = useRouter();
  const active = useLocale();

  function select(locale: Locale) {
    persistLocale(locale);
    router.refresh();
  }

  return (
    <div className="flex gap-1" role="group" aria-label="Language / Idioma">
      {LOCALES.map((locale) => (
        <button
          key={locale}
          type="button"
          aria-pressed={locale === active}
          onClick={() => select(locale)}
          className={cn(
            "rounded-sm px-2 py-1 text-xs uppercase tracking-wide",
            "transition-colors duration-150",
            "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]",
            locale === active ? "text-foreground" : "text-muted hover:text-foreground",
          )}
        >
          {locale}
        </button>
      ))}
    </div>
  );
}
