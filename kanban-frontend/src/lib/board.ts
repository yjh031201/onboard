// 칸반 카드 REST API. 실시간 반영은 subscribeTopic("/topic/board", ...)로 별도 처리한다 (realtime.ts).

import { ApiError, apiRequest } from "./api";
import { getStoredUser } from "./auth";
import type { CardStatus, TaskCard } from "../types/dashboard";

interface CardDto {
  id: number;
  title: string;
  status: CardStatus;
  position: number;
  labelId: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

export interface BoardEvent {
  type: "CARD_CREATED" | "CARD_MOVED" | "CARD_LABEL_CHANGED" | "CARD_DELETED";
  cards: CardDto[];
  actorName: string;
}

function toTaskCard(dto: CardDto): TaskCard {
  return {
    id: String(dto.id),
    title: dto.title,
    status: dto.status,
    labelId: dto.labelId,
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

export function createCard(title: string, status: CardStatus, labelId: string | null): Promise<TaskCard> {
  return apiRequest<CardDto>("/api/cards", {
    method: "POST",
    body: JSON.stringify({ title, status, labelId }),
  }).then(toTaskCard);
}

/** position은 이동할 컬럼 내에서의 0-based 목표 인덱스. */
export function moveCard(cardId: string, status: CardStatus, position: number): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}/move`, {
    method: "PATCH",
    body: JSON.stringify({ status, position }),
  }).then(toTaskCard);
}

/** labelId가 null이면 카드에서 라벨을 뗀다. */
export function changeCardLabel(cardId: string, labelId: string | null): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}/label`, {
    method: "PATCH",
    body: JSON.stringify({ labelId }),
  }).then(toTaskCard);
}

export function deleteCard(cardId: string): Promise<void> {
  return apiRequest<void>(`/api/cards/${cardId}`, { method: "DELETE" });
}

/** 작성자 본인이거나 OWNER/ADMIN만 삭제 가능 (서버에서도 동일하게 검증됨). */
export function canDeleteCard(task: TaskCard): boolean {
  const user = getStoredUser();
  return task.createdById === user?.id || user?.role === "OWNER" || user?.role === "ADMIN";
}

/** 확인 창을 띄운 뒤 삭제한다. 화면 반영은 board 스냅샷을 통해서만 하고, 실패하면 이유를 알려준다. */
export function confirmAndDeleteCard(task: TaskCard): void {
  if (!window.confirm(`'${task.title}' 카드를 삭제할까요?`)) return;
  deleteCard(task.id).catch((err) => {
    window.alert(err instanceof ApiError ? err.message : "카드를 삭제하지 못했어요.");
  });
}
