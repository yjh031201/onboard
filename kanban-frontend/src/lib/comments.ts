// 카드 댓글 REST API + 실시간 구독.
// 서버(CardCommentService)는 댓글이 바뀔 때마다 그 카드의 댓글 전체를 /topic/projects/{id}/card-comments로 보낸다.
// 카드 앞면의 댓글 수(commentCount)는 board 스냅샷으로 따로 갱신된다.

import { apiRequest } from "./api";
import { subscribeTopic } from "./realtime";

/** 댓글 글자 수 제한 (서버 CommentRequest와 같음). */
export const MAX_COMMENT_LENGTH = 1000;

export interface CardComment {
  id: number;
  cardId: number;
  authorId: number;
  authorName: string;
  content: string;
  createdAt: string;
  edited: boolean;
}

interface CommentEvent {
  cardId: number;
  comments: CardComment[];
}

function commentsPath(projectId: number, cardId: string): string {
  return `/api/projects/${projectId}/cards/${cardId}/comments`;
}

export function fetchComments(projectId: number, cardId: string): Promise<CardComment[]> {
  return apiRequest<CardComment[]>(commentsPath(projectId, cardId));
}

export function createComment(projectId: number, cardId: string, content: string): Promise<CardComment> {
  return apiRequest<CardComment>(commentsPath(projectId, cardId), {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function updateComment(projectId: number, cardId: string, commentId: number, content: string): Promise<CardComment> {
  return apiRequest<CardComment>(`${commentsPath(projectId, cardId)}/${commentId}`, {
    method: "PATCH",
    body: JSON.stringify({ content }),
  });
}

export function deleteComment(projectId: number, cardId: string, commentId: number): Promise<void> {
  return apiRequest<void>(`${commentsPath(projectId, cardId)}/${commentId}`, { method: "DELETE" });
}

/** 이 카드의 댓글이 바뀌면(누가 쓰든) 전체 목록을 받는다. 프로젝트에 토픽이 하나라 cardId로 거른다. */
export function subscribeCardComments(
  projectId: number,
  cardId: string,
  onChange: (comments: CardComment[]) => void,
): () => void {
  return subscribeTopic<CommentEvent>(`/topic/projects/${projectId}/card-comments`, (event) => {
    if (String(event.cardId) === cardId) onChange(event.comments);
  });
}

/** 삭제 버튼을 보일지 — 작성자 본인 또는 프로젝트 OWNER/ADMIN (서버와 같은 기준). 수정은 작성자만. */
export function canDeleteComment(comment: CardComment, myUserId: number | undefined, isAdmin: boolean): boolean {
  return comment.authorId === myUserId || isAdmin;
}

/** "2026-11-11T14:05:00" → 방금 전 / 5분 전 / 3시간 전 / 11. 9. 14:05 */
export function formatCommentTime(value: string, now: Date = new Date()): string {
  const date = new Date(value);
  const diffMinutes = Math.floor((now.getTime() - date.getTime()) / 60000);
  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffMinutes < 24 * 60) return `${Math.floor(diffMinutes / 60)}시간 전`;
  const time = `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
  const sameYear = date.getFullYear() === now.getFullYear();
  return `${sameYear ? "" : `${date.getFullYear()}. `}${date.getMonth() + 1}. ${date.getDate()}. ${time}`;
}
