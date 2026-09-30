// 칸반 보드 컬럼 REST API — 설정 페이지 "보드 컬럼" 섹션과 칸반보드/대시보드 위젯이 공유한다.
// 실시간 반영은 subscribeTopic("/topic/columns", ...)로 처리한다 (useBoardColumns 훅).
// 카드는 컬럼 id를 status로 저장한다 — 기본 컬럼 id는 TODO / IN_PROGRESS / DONE.

import { apiRequest } from "./api";

export interface ColumnDef {
  id: string;
  name: string;
  color: string;
}

export function fetchColumns(): Promise<ColumnDef[]> {
  return apiRequest<ColumnDef[]>("/api/columns");
}

export function createColumn(name: string, color: string): Promise<ColumnDef> {
  return apiRequest<ColumnDef>("/api/columns", {
    method: "POST",
    body: JSON.stringify({ name, color }),
  });
}

export function updateColumn(id: string, name: string, color: string): Promise<ColumnDef> {
  return apiRequest<ColumnDef>(`/api/columns/${id}`, {
    method: "PUT",
    body: JSON.stringify({ name, color }),
  });
}

/** 카드가 남아 있거나 마지막 하나 남은 컬럼이면 서버가 거절한다 (409). */
export function deleteColumn(id: string): Promise<void> {
  return apiRequest<void>(`/api/columns/${id}`, { method: "DELETE" });
}

/** 새 순서대로 나열한 전체 컬럼 id를 보낸다. 그 사이 컬럼이 추가/삭제됐으면 서버가 거절한다 (409). */
export function reorderColumns(columnIds: string[]): Promise<ColumnDef[]> {
  return apiRequest<ColumnDef[]>("/api/columns/order", {
    method: "PUT",
    body: JSON.stringify({ columnIds }),
  });
}
