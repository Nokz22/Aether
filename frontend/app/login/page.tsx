import { getTranslations } from "next-intl/server";
import { LocaleSwitcher } from "@/features/auth/locale-switcher";
import { LoginForm } from "@/features/auth/login-form";

export default async function LoginPage() {
  const t = await getTranslations("app");

  return (
    <main className="relative flex min-h-dvh flex-col items-center justify-center p-6">
      <div className="aurora" aria-hidden />

      <div className="glass-card relative w-full max-w-sm p-8">
        <header className="mb-8 flex items-start justify-between">
          <div>
            <p className="text-sm font-medium tracking-[0.2em]">{t("name")}</p>
            <p className="mt-1 text-sm text-muted">{t("tagline")}</p>
          </div>
          <LocaleSwitcher />
        </header>
        <LoginForm />
      </div>
    </main>
  );
}
