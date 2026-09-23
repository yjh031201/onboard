// 칸반 카드 REST API. 실시간 반영은 subscribeTopic("/topic/board", ...)로 별도 처리한다 (realtime.ts).

import { apiRequest } from "./api";
import type { CardStatus, TaskCard } from "../types/dashboard";

interface CardDto {
  id: number;
  title: string;
  status: CardStatus;
  position: number;
  createdByName: string;
  createdAt: string;
}

export interface BoardEvent {
  type: "CARD_CREATED" | "CARD_MOVED";
  cards: CardDto[];
  actorName: string;
}

function toTaskCard(dto: CardDto): TaskCard {
  return {
    id: String(dto.id),
    title: dto.title,
    status: dto.status,
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

export function createCard(title: string, status: CardStatus): Promise<TaskCard> {
  return apiRequest<CardDto>("/api/cards", {
    method: "POST",
    body: JSON.stringify({ title, status }),
  }).then(toTaskCard);
}

/** position은 이동할 컬럼 내에서의 0-based 목표 인덱스. */
export function moveCard(cardId: string, status: CardStatus, position: number): Promise<TaskCard> {
  return apiRequest<CardDto>(`/api/cards/${cardId}/move`, {
    method: "PATCH",
    body: JSON.stringify({ status, position }),
  }).then(toTaskCard);
}
