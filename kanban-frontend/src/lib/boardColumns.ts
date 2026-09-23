import type { CardStatus } from "../types/dashboard";

export interface ColumnDef {
  id: CardStatus;
  title: string;
  tagColor: string;
}

/** 백엔드 CardStatus enum과 1:1로 대응 — 칸반보드/대시보드 위젯이 공유한다. */
export const COLUMN_DEFS: ColumnDef[] = [
  { id: "TODO", title: "할 일", tagColor: "bg-[#94a3b8]" },
  { id: "IN_PROGRESS", title: "진행 중", tagColor: "bg-[#f59e0b]" },
  { id: "DONE", title: "완료", tagColor: "bg-[#10b981]" },
];
