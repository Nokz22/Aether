"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { authApi, type TokenResponse, type UserResponse } from "@/lib/api";

type SessionValue = {
  user: UserResponse;
  accessToken: string;
  logout: () => void;
};

const SessionContext = createContext<SessionValue | null>(null);

// Access tokens last 15 min; renew with margin so requests never carry a stale one.
const RENEW_INTERVAL_MS = 13 * 60 * 1000;

// Shared in-flight refresh: a double-mounted effect (React strict mode) must not
// send the same single-use refresh token twice.
let inflightRefresh: Promise<TokenResponse> | null = null;

function refreshOnce(): Promise<TokenResponse> {
  inflightRefresh ??= authApi.refresh().finally(() => {
    inflightRefresh = null;
  });
  return inflightRefresh;
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [session, setSession] = useState<TokenResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    async function establish() {
      try {
        const tokens = await refreshOnce();
        if (cancelled) return;
        setSession(tokens);
        timer = setTimeout(establish, RENEW_INTERVAL_MS);
      } catch {
        if (!cancelled) router.replace("/login");
      }
    }

    establish();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [router]);

  if (!session) {
    return (
      <div className="flex min-h-dvh items-center justify-center" aria-busy>
        <p className="animate-pulse text-sm font-medium tracking-[0.2em] text-muted">
          AETHER
        </p>
      </div>
    );
  }

  const logout = () => {
    authApi.logout().finally(() => router.replace("/login"));
  };

  return (
    <SessionContext.Provider
      value={{ user: session.user, accessToken: session.accessToken, logout }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession(): SessionValue {
  const value = useContext(SessionContext);
  if (!value) {
    throw new Error("useSession must be used inside <SessionProvider>");
  }
  return value;
}
