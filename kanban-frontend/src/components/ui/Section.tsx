import type { ReactNode } from "react";

interface SectionProps {
  title: string;
  description: string;
  danger?: boolean;
  children: ReactNode;
}

export default function Section({ title, description, danger, children }: SectionProps) {
  return (
    <div
      className={`flex w-full flex-col gap-[18px] rounded-[14px] border bg-white p-7 shadow-[0px_2px_8px_0px_rgba(0,0,0,0.03)] ${
        danger ? "border-[#fecaca]" : "border-[#f0f0f2]"
      }`}
    >
      <div className="flex w-full flex-col gap-1">
        <p className={`text-[16px] font-bold ${danger ? "text-[#ef4444]" : "text-[#111827]"}`}>
          {title}
        </p>
        <p className="text-[13px] text-[#6b7280]">{description}</p>
      </div>
      {children}
    </div>
  );
}

export function Divider() {
  return <div className="h-px w-full bg-[#f0f0f2]" />;
}
