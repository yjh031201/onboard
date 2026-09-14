import type { ReactNode } from "react";
import TopBar from "./TopBar";

interface PageShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

export default function PageShell({ title, subtitle, children }: PageShellProps) {
  return (
    <div className="flex h-full flex-1 flex-col gap-7 overflow-auto px-9 py-8">
      <TopBar title={title} subtitle={subtitle} userInitial="양" />
      {children}
    </div>
  );
}
