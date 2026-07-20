import type { ReactNode } from "react";
import { SessionProvider } from "@/features/auth/session-provider";
import { Dock } from "@/features/shell/dock";
import { TopBar } from "@/features/shell/top-bar";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      <div className="flex min-h-dvh">
        <Dock />
        <div className="flex min-w-0 flex-1 flex-col">
          <TopBar />
          <main className="flex-1 p-8">{children}</main>
        </div>
      </div>
    </SessionProvider>
  );
}
