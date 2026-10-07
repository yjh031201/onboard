// 칸반 보드 컬럼 REST API — 설정 페이지 "보드 컬럼" 섹션과 칸반보드/대시보드 위젯이 공유한다.
// 실시간 반영은 subscribeTopic(`/topic/projects/${projectId}/columns`, ...)로 처리한다 (useBoardColumns 훅).
// 카드는 컬럼 id를 status로 저장한다 — 기본 컬럼은 프로젝트 생성 시 서버가 3개를 시드한다.

import { apiRequest } from "./api";

export interface ColumnDef {
  id: string;
  name: string;
  color: string;
}

export function fetchColumns(projectId: number): Promise<ColumnDef[]> {
  return apiRequest<ColumnDef[]>(`/api/projects/${projectId}/board-columns`);
}

export function createColumn(projectId: number, name: string, color: string): Promise<ColumnDef> {
  return apiRequest<ColumnDef>(`/api/projects/${projectId}/board-columns`, {
    method: "POST",
    body: JSON.stringify({ name, color }),
  });
}

export function updateColumn(projectId: number, id: string, name: string, color: string): Promise<ColumnDef> {
  return apiRequest<ColumnDef>(`/api/projects/${projectId}/board-columns/${id}`, {
    method: "PUT",
    body: JSON.stringify({ name, color }),
  });
}

/** 카드가 남아 있거나 마지막 하나 남은 컬럼이면 서버가 거절한다 (409). */
export function deleteColumn(projectId: number, id: string): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}/board-columns/${id}`, { method: "DELETE" });
}

/** 새 순서대로 나열한 전체 컬럼 id를 보낸다. 그 사이 컬럼이 추가/삭제됐으면 서버가 거절한다 (409). */
export function reorderColumns(projectId: number, columnIds: string[]): Promise<ColumnDef[]> {
  return apiRequest<ColumnDef[]>(`/api/projects/${projectId}/board-columns/order`, {
    method: "PUT",
    body: JSON.stringify({ columnIds }),
  });
}
