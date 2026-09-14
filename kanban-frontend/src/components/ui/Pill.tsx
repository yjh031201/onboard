import type { ReactNode } from "react";

interface PillProps {
  children: ReactNode;
  bg: string;
  text: string;
}

export default function Pill({ children, bg, text }: PillProps) {
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-1 text-[11.5px] font-medium"
      style={{ backgroundColor: bg, color: text }}
    >
      {children}
    </span>
  );
}
