// 칸반 카드 REST API. 실시간 반영은 subscribeTopic("/topic/board", ...)로 별도 처리한다 (realtime.ts).

import { ApiError, apiRequest } from "./api";
import { getStoredUser } from "./auth";
import type { CardStatus, TaskCard } from "../types/dashboard";

/** 카드 하나에 붙일 수 있는 라벨 수 (서버 Card.MAX_LABELS와 같음). */
export const MAX_CARD_LABELS = 2;

/**
 * 카드 제목 스타일 — 칸반 페이지와 대시보드 위젯이 같이 쓴다.
 * 한글은 단어 단위로 줄바꿈(break-keep)하되, URL처럼 띄어쓰기 없는 긴 문자열은 어디서든 끊어서(overflow-wrap:anywhere)
 * 카드 밖으로 넘치지 않고 줄이 늘어나게 한다.
 * min-w-[40%]: 오른쪽 라벨·버튼에 밀려 제목이 한두 글자 폭으로 찌그러지지 않게 한다 —
 * 자리가 모자라면 제목이 줄어드는 대신 라벨·버튼이 다음 줄로 내려간다 (부모가 flex-wrap).
 */
export const CARD_TITLE_CLASS =
  "min-w-[40%] flex-1 break-keep text-[13px] text-[#111827] [overflow-wrap:anywhere]";

/** 카드 설명 글자 수 제한 (서버 UpdateCardRequest와 같음). */
export const MAX_CARD_DESCRIPTION_LENGTH = 2000;

/** 카드 제목 입력값 정리 — 붙여넣은 줄바꿈은 공백으로 바꾼다 (제목은 한 문단). */
export function normalizeCardTitle(value: string): string {
  return value.replace(/\s*\n\s*/g, " ");
}

interface CardDto {
  id: number;
  title: string;
  description: string | null;
  status: CardStatus;
  position: number;
  labelIds: string[];
  customLabel: string | null;
  dueAt: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

export interface BoardEvent {
  type: "CARD_CREATED" | "CARD_MOVED" | "CARD_UPDATED" | "CARD_LABEL_CHANGED" | "CARD_DELETED";
  cards: CardDto[];
  actorName: string;
}

function toTaskCard(dto: CardDto): TaskCard {
  return {
    id: String(dto.id),
    title: dto.title,
    description: dto.description ?? null,
    status: dto.status,
    labelIds: dto.labelIds ?? [],
    customLabel: dto.customLabel ?? null,
    dueAt: dto.dueAt,
    createdById: dto.createdById,
    createdByName: dto.createdByName,
    createdAt: dto.createdAt,
  };
}

export function toTaskCards(dtos: CardDto[]): TaskCard[] {
  return dtos.map(toTaskCard);
}

export function fetchCards(): Promise<TaskCard[]> {
  return apiRequest<CardDto[]>("/api/cards").then(toTaskCards);
}

/** customLabel은 "기타" 라벨에 직접 적은 글자(없으면 null). dueAt은 "YYYY-MM-DDTHH:mm" 또는 null(마감 없음). */
export function createCard(
  title: string,
  status: CardStatus,
  labelIds: string[],
  customLabel: string | null,
  dueAt: string | null,
): Promise<TaskCard> {
  return apiRequest<CardDto>("/api/cards", {
    method: "POST",
    body: JSON.stringify({ title, status, labelIds, customLabel, dueAt }),
  }).then(toTaskCard);
}

/** 제목·설명·마감 수정. description이 비어 있으면 설명을 지운다. dueAt은 "YYYY-MM-DDTHH:mm" 또는 null(마감 없음). */
export function updateCard(
  cardId: string,
  title: string,
  description: string,
  dueAt: string | null,
): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}`, {
    method: "PATCH",
    body: JSON.stringify({ title, description, dueAt }),
  }).then(toTaskCard);
}

/** position은 이동할 컬럼 내에서의 0-based 목표 인덱스. */
export function moveCard(cardId: string, status: CardStatus, position: number): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}/move`, {
    method: "PATCH",
    body: JSON.stringify({ status, position }),
  }).then(toTaskCard);
}

/**
 * 카드의 라벨 전체를 바꾼다. 빈 배열이면 라벨을 모두 뗀다.
 * customLabel은 "기타" 라벨에 직접 적은 글자 — 기타 라벨이 labelIds에 없으면 서버가 버린다.
 */
export function changeCardLabels(
  cardId: string,
  labelIds: string[],
  customLabel: string | null,
): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}/label`, {
    method: "PATCH",
    body: JSON.stringify({ labelIds, customLabel }),
  }).then(toTaskCard);
}

export function deleteCard(cardId: string): Promise<void> {
  return apiRequest<void>(`/api/cards/${cardId}`, { method: "DELETE" });
}

/** 작성자 본인이거나 OWNER/ADMIN만 수정·삭제 가능 (서버에서도 동일하게 검증됨). */
export function canManageCard(task: TaskCard): boolean {
  const user = getStoredUser();
  return task.createdById === user?.id || user?.role === "OWNER" || user?.role === "ADMIN";
}

/** 라벨 선택 토글 — 이미 있으면 빼고, 없으면 최대 개수 안에서 뒤에 붙인다. */
export function toggleLabelId(labelIds: string[], labelId: string): string[] {
  if (labelIds.includes(labelId)) return labelIds.filter((id) => id !== labelId);
  if (labelIds.length >= MAX_CARD_LABELS) return labelIds;
  return [...labelIds, labelId];
}

/** 마감 표시용 — "10/2 18:00", 지났으면 overdue. */
export function describeDue(dueAt: string): { text: string; overdue: boolean } {
  const due = new Date(dueAt);
  const text = `${due.getMonth() + 1}/${due.getDate()} ${String(due.getHours()).padStart(2, "0")}:${String(
    due.getMinutes(),
  ).padStart(2, "0")}`;
  return { text, overdue: due.getTime() < Date.now() };
}

/** 확인 창을 띄운 뒤 삭제한다. 화면 반영은 board 스냅샷을 통해서만 하고, 실패하면 이유를 알려준다. */
export function confirmAndDeleteCard(task: TaskCard): void {
  if (!window.confirm(`'${task.title}' 카드를 삭제할까요?`)) return;
  deleteCard(task.id).catch((err) => {
    window.alert(err instanceof ApiError ? err.message : "카드를 삭제하지 못했어요.");
  });
}

/** 서버가 거절한 이유(권한, 라벨 개수 등)를 알려준다. 화면은 다음 board 스냅샷으로 다시 맞춰진다. */
export function alertCardError(err: unknown, fallback: string): void {
  window.alert(err instanceof ApiError ? err.message : fallback);
}
