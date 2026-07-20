"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiRequestError, authApi } from "@/lib/api";

type Mode = "login" | "register";

const KNOWN_ERROR_CODES = [
  "invalid_credentials",
  "registration_closed",
  "email_already_used",
  "validation_failed",
] as const;

type KnownErrorCode = (typeof KNOWN_ERROR_CODES)[number];

function toErrorKey(error: unknown): `errors.${KnownErrorCode | "network" | "unknown"}` {
  if (!(error instanceof ApiRequestError)) {
    return "errors.network";
  }
  const code = KNOWN_ERROR_CODES.find((known) => known === error.body.code);
  return code ? `errors.${code}` : "errors.unknown";
}

export function LoginForm() {
  const t = useTranslations("auth");
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("login");

  const submit = useMutation({
    mutationFn: (form: FormData) => {
      const email = String(form.get("email") ?? "");
      const password = String(form.get("password") ?? "");
      return mode === "register"
        ? authApi.register({ email, password, displayName: String(form.get("displayName") ?? "") })
        : authApi.login({ email, password });
    },
    onSuccess: () => router.push("/"),
  });

  function switchMode() {
    submit.reset();
    setMode(mode === "login" ? "register" : "login");
  }

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        submit.mutate(new FormData(event.currentTarget));
      }}
    >
      <h1 className="text-xl font-medium tracking-tight">
        {mode === "login" ? t("loginTitle") : t("registerTitle")}
      </h1>

      {mode === "register" && (
        <div className="flex flex-col gap-1.5">
          <label className="text-sm text-muted" htmlFor="displayName">
            {t("displayName")}
          </label>
          <Input
            id="displayName"
            name="displayName"
            autoComplete="name"
            maxLength={100}
            required
            disabled={submit.isPending}
          />
        </div>
      )}

      <div className="flex flex-col gap-1.5">
        <label className="text-sm text-muted" htmlFor="email">
          {t("email")}
        </label>
        <Input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          maxLength={320}
          required
          disabled={submit.isPending}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-sm text-muted" htmlFor="password">
          {t("password")}
        </label>
        <Input
          id="password"
          name="password"
          type="password"
          autoComplete={mode === "register" ? "new-password" : "current-password"}
          minLength={8}
          maxLength={72}
          required
          disabled={submit.isPending}
        />
      </div>

      {submit.isError && (
        <p role="alert" className="text-sm text-danger">
          {t(toErrorKey(submit.error))}
        </p>
      )}

      <Button type="submit" loading={submit.isPending}>
        {submit.isPending
          ? t("submitting")
          : mode === "login"
            ? t("submitLogin")
            : t("submitRegister")}
      </Button>

      <button
        type="button"
        onClick={switchMode}
        className="text-sm text-muted transition-colors duration-150 hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--focus-ring)]"
      >
        {mode === "login" ? t("switchToRegister") : t("switchToLogin")}
      </button>
    </form>
  );
}
