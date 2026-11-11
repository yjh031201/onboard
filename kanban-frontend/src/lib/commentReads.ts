// 카드별 "읽은 댓글 수" — 카드 앞면의 +N(새 댓글) 배지에 쓴다.
// 사용자·프로젝트별로 이 브라우저(localStorage)에만 저장한다. 서버에는 읽음 기록이 없다.
//
// 처음 보는 카드는 지금 댓글 수를 "읽음"으로 잡아서(기준선), 기능을 켠 날 옛 댓글이 전부 새 댓글로 뜨지 않게 한다.
// 이후 늘어난 만큼이 새 댓글이고, 카드 상세 창을 열면 그때 댓글 수로 다시 맞춘다.
// 댓글이 지워져 수가 줄면 읽은 수도 그만큼 내린다 (안 그러면 다음 새 댓글이 가려진다).

import { useSyncExternalStore } from "react";
import { getStoredUser } from "./auth";
import type { TaskCard } from "../types/dashboard";

type ReadCounts = Record<string, number>;

const EMPTY: ReadCounts = {};
const cache = new Map<string, ReadCounts>();
const listeners = new Set<() => void>();

function storageKey(projectId: number): string {
  return `kanban_comment_reads:${getStoredUser()?.id ?? 0}:${projectId}`;
}

function load(projectId: number): ReadCounts {
  const key = storageKey(projectId);
  const cached = cache.get(key);
  if (cached) return cached;
  let reads: ReadCounts = EMPTY;
  try {
    const raw = localStorage.getItem(key);
    if (raw) reads = JSON.parse(raw) as ReadCounts;
  } catch {
    // 못 읽으면 빈 기록으로 시작한다.
  }
  cache.set(key, reads);
  return reads;
}

function save(projectId: number, reads: ReadCounts) {
  const key = storageKey(projectId);
  cache.set(key, reads);
  try {
    localStorage.setItem(key, JSON.stringify(reads));
  } catch {
    // localStorage를 못 쓰면 이번 세션에서만 기억한다.
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** 카드 목록을 받을 때마다 부른다 — 처음 보는 카드는 기준선을 잡고, 댓글이 줄어든 카드는 읽은 수를 내린다. */
export function syncCommentReads(projectId: number, tasks: TaskCard[]) {
  const reads = load(projectId);
  let next: ReadCounts | null = null;
  for (const task of tasks) {
    const read = reads[task.id];
    if (read === undefined || read > task.commentCount) {
      next ??= { ...reads };
      next[task.id] = task.commentCount;
    }
  }
  if (next) save(projectId, next);
}

/** 상세 창에서 댓글을 봤을 때 — 지금 댓글 수까지 읽은 것으로 한다. */
export function markCommentsRead(projectId: number, cardId: string, count: number) {
  const reads = load(projectId);
  if (reads[cardId] === count) return;
  save(projectId, { ...reads, [cardId]: count });
}

/** 이 카드에서 읽은 댓글 수. 기준선이 아직 없으면 undefined. */
export function readCommentCount(projectId: number, cardId: string): number | undefined {
  return load(projectId)[cardId];
}

/** 아직 안 읽은 댓글 수. 기준선이 아직 없으면 0. */
export function unreadCommentCount(reads: ReadCounts, task: TaskCard): number {
  const read = reads[task.id];
  return read === undefined ? 0 : Math.max(0, task.commentCount - read);
}

/** 읽음 기록 — 바뀌면 다시 그린다. */
export function useCommentReads(projectId: number): ReadCounts {
  return useSyncExternalStore(subscribe, () => load(projectId));
}
