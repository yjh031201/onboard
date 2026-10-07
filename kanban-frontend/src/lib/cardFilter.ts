// 칸반 보드 검색·필터 — 화면에서만 걸러 보여 준다 (서버 요청 없음).

import type { TaskCard } from "../types/dashboard";

export interface CardFilter {
  /** 제목·설명·직접 적은 라벨 또는 작성자 이름에 들어 있는 글자 (대소문자 구분 없음). */
  query: string;
  /** 고른 라벨 중 하나라도 붙어 있으면 통과. 비어 있으면 라벨로 거르지 않는다. */
  labelIds: string[];
  /** 내가 작성한 카드만. */
  mineOnly: boolean;
  /** 마감이 지났거나 DUE_SOON_DAYS일 안에 닥치는 카드만. */
  dueSoon: boolean;
}

export const EMPTY_CARD_FILTER: CardFilter = { query: "", labelIds: [], mineOnly: false, dueSoon: false };

export const DUE_SOON_DAYS = 3;

export function isFilterActive(filter: CardFilter): boolean {
  return filter.query.trim() !== "" || filter.labelIds.length > 0 || filter.mineOnly || filter.dueSoon;
}

/** 조건은 모두 만족해야 한다(AND). myId는 로그인한 사용자 id. */
export function matchesFilter(
  task: TaskCard,
  filter: CardFilter,
  myId: number | undefined,
  now: number = Date.now(),
): boolean {
  const query = filter.query.trim().toLowerCase();
  if (
    query &&
    !task.title.toLowerCase().includes(query) &&
    !(task.description ?? "").toLowerCase().includes(query) &&
    !(task.customLabel ?? "").toLowerCase().includes(query) &&
    !task.createdByName.toLowerCase().includes(query)
  ) {
    return false;
  }
  if (filter.labelIds.length > 0 && !filter.labelIds.some((id) => task.labelIds.includes(id))) {
    return false;
  }
  if (filter.mineOnly && task.createdById !== myId) {
    return false;
  }
  if (filter.dueSoon) {
    if (!task.dueAt) return false;
    const deadline = now + DUE_SOON_DAYS * 24 * 60 * 60 * 1000;
    if (new Date(task.dueAt).getTime() > deadline) return false;
  }
  return true;
}
