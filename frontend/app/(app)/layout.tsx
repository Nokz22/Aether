import type { ReactNode } from "react";
import { SessionProvider } from "@/features/auth/session-provider";
import { Dock } from "@/features/shell/dock";
import { TopBar } from "@/features/shell/top-bar";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <SessionProvider>
      {/* Fixed to the viewport so the content region (main) is the scroller and
          pages using h-full can bound their own internal scroll areas. */}
      <div className="flex h-dvh overflow-hidden">
        <Dock />
        <div className="flex min-w-0 flex-1 flex-col">
          <TopBar />
          <main className="min-h-0 flex-1 overflow-y-auto p-8">{children}</main>
        </div>
      </div>
    </SessionProvider>
  );
}
