import type { ReactNode } from "react";
import Logo from "../ui/Logo";

interface AuthCardProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

export default function AuthCard({ title, subtitle, children }: AuthCardProps) {
  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-[#fafafa] px-4">
      <div className="flex w-[420px] max-w-full flex-col gap-[22px] rounded-2xl border border-[#f0f0f2] bg-white p-10 shadow-[0px_4px_16px_0px_rgba(0,0,0,0.05)]">
        <div className="flex w-full justify-center">
          <Logo iconSize="size-6" textClassName="text-[14px] font-bold text-[#6366f1]" />
        </div>
        <p className="w-full text-center text-[23px] font-bold text-[#111827]">{title}</p>
        <p className="w-full text-center text-[13px] text-[#6b7280]">{subtitle}</p>
        {children}
      </div>
    </div>
  );
}
