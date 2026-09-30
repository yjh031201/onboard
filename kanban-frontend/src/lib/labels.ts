// 카드 라벨 REST API — 설정 페이지 "라벨" 섹션과 칸반보드 카드가 공유한다.
// 실시간 반영은 subscribeTopic("/topic/labels", ...)로 처리한다 (useLabels 훅).
// 카드에는 라벨 id만 저장되므로(cards.label_id), 이름/색을 바꾸면 모든 카드에 바로 반영된다.

import { apiRequest } from "./api";

export interface LabelDef {
  id: string;
  name: string;
  color: string;
}

export function findLabel(labels: LabelDef[], id: string | null | undefined): LabelDef | undefined {
  return id ? labels.find((label) => label.id === id) : undefined;
}

export function fetchLabels(): Promise<LabelDef[]> {
  return apiRequest<LabelDef[]>("/api/labels");
}

export function createLabel(name: string, color: string): Promise<LabelDef> {
  return apiRequest<LabelDef>("/api/labels", {
    method: "POST",
    body: JSON.stringify({ name, color }),
  });
}

export function updateLabel(id: string, name: string, color: string): Promise<LabelDef> {
  return apiRequest<LabelDef>(`/api/labels/${id}`, {
    method: "PUT",
    body: JSON.stringify({ name, color }),
  });
}

/** 삭제하면 그 라벨이 붙어 있던 카드들은 라벨 없음이 된다 (서버에서 처리). */
export function deleteLabel(id: string): Promise<void> {
  return apiRequest<void>(`/api/labels/${id}`, { method: "DELETE" });
}
