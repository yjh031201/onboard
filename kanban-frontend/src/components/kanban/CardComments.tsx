// 카드 상세 창 아래의 댓글 영역 — 팀원 누구나 읽고 쓴다.
// 목록은 열 때 한 번 불러오고, 이후에는 실시간 이벤트(그 카드의 댓글 전체)로 통째로 바꾼다.
// 내가 쓴 댓글도 응답이 아니라 실시간 이벤트로 반영되지만, 연결이 늦을 때를 대비해 응답으로도 한 번 맞춘다.

import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { markCommentsRead, readCommentCount } from "../../lib/commentReads";
import { getStoredUser } from "../../lib/auth";
import { alertCardError } from "../../lib/board";
import {
  MAX_COMMENT_LENGTH,
  canDeleteComment,
  createComment,
  deleteComment,
  fetchComments,
  formatCommentTime,
  subscribeCardComments,
  updateComment,
  type CardComment,
} from "../../lib/comments";
import { DESCRIPTION_INPUT_CLASS } from "./CardForms";
import { useAutoGrow } from "./cardFormUtils";

interface CardCommentsProps {
  projectId: number;
  cardId: string;
  /** 이 프로젝트의 OWNER/ADMIN이면 남의 댓글도 지울 수 있다. */
  isAdmin: boolean;
}

/** Enter = 보내기, Shift+Enter = 줄바꿈. 한글 조합 중 Enter는 무시. Esc는 창을 닫지 않고 onEscape만. */
function handleSubmitKeys(e: KeyboardEvent<HTMLTextAreaElement>, onSubmit: () => void, onEscape?: () => void) {
  if (e.nativeEvent.isComposing) return;
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    onSubmit();
  } else if (e.key === "Escape" && onEscape) {
    e.stopPropagation();
    onEscape();
  }
}

export default function CardComments({ projectId, cardId, isAdmin }: CardCommentsProps) {
  const myUserId = getStoredUser()?.id;
  const [comments, setComments] = useState<CardComment[] | null>(null);
  const [loadError, setLoadError] = useState(false);
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);
  // 수정 중인 댓글 id와 고치는 글.
  const [editing, setEditing] = useState<{ id: number; text: string } | null>(null);
  const inputRef = useAutoGrow(text);
  const editRef = useAutoGrow(editing?.text ?? "");

  // NEW 표시할 댓글 — 열 때 안 읽었던 것 + 열어 둔 사이에 팀원이 단 것 (내 댓글은 빼고).
  const [newIds, setNewIds] = useState<ReadonlySet<number>>(() => new Set());
  const commentsRef = useRef<CardComment[] | null>(null);

  useEffect(() => {
    commentsRef.current = comments;
    // 창에 보이는 만큼 읽은 것으로 — 카드 앞면의 +N 배지가 사라진다.
    if (comments) markCommentsRead(projectId, cardId, comments.length);
  }, [projectId, cardId, comments]);

  useEffect(() => {
    let cancelled = false;
    const me = getStoredUser()?.id;
    const othersIds = (list: CardComment[]) => list.filter((c) => c.authorId !== me).map((c) => c.id);
    setComments(null);
    setLoadError(false);
    setNewIds(new Set());
    fetchComments(projectId, cardId)
      .then((list) => {
        if (cancelled) return;
        const read = readCommentCount(projectId, cardId);
        if (read !== undefined && list.length > read) setNewIds(new Set(othersIds(list.slice(read))));
        setComments(list);
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      });
    const unsubscribe = subscribeCardComments(projectId, cardId, (list) => {
      const prev = commentsRef.current;
      if (prev) {
        const known = new Set(prev.map((c) => c.id));
        const fresh = othersIds(list.filter((c) => !known.has(c.id)));
        if (fresh.length > 0) setNewIds((ids) => new Set([...ids, ...fresh]));
      }
      setComments(list);
    });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [projectId, cardId]);

  /** 응답으로 받은 댓글을 목록에 넣거나 바꾼다 (실시간 이벤트가 먼저 왔으면 그대로). */
  const upsert = (comment: CardComment) =>
    setComments((prev) => {
      if (!prev) return [comment];
      return prev.some((c) => c.id === comment.id) ? prev.map((c) => (c.id === comment.id ? comment : c)) : [...prev, comment];
    });

  const send = async () => {
    const content = text.trim();
    if (!content || sending) return;
    setSending(true);
    try {
      upsert(await createComment(projectId, cardId, content));
      setText("");
    } catch (err) {
      alertCardError(err, "댓글을 남기지 못했어요.");
    } finally {
      setSending(false);
    }
  };

  const saveEdit = async () => {
    if (!editing || sending) return;
    const content = editing.text.trim();
    if (!content) return;
    setSending(true);
    try {
      upsert(await updateComment(projectId, cardId, editing.id, content));
      setEditing(null);
    } catch (err) {
      alertCardError(err, "댓글을 고치지 못했어요.");
    } finally {
      setSending(false);
    }
  };

  const remove = async (comment: CardComment) => {
    if (!window.confirm("이 댓글을 지울까요?")) return;
    try {
      await deleteComment(projectId, cardId, comment.id);
      setComments((prev) => prev?.filter((c) => c.id !== comment.id) ?? prev);
    } catch (err) {
      alertCardError(err, "댓글을 지우지 못했어요.");
    }
  };

  return (
    <div className="flex w-full flex-col gap-3 border-t border-[#f0f0f2] pt-4">
      <p className="text-[13px] font-medium text-[#111827]">
        댓글{comments && comments.length > 0 && <span className="ml-1 text-[#6366f1]">{comments.length}</span>}
      </p>

      {loadError ? (
        <p className="text-[12px] text-[#ef4444]">댓글을 불러오지 못했어요.</p>
      ) : comments === null ? (
        <p className="text-[12px] text-[#9ca3af]">불러오는 중…</p>
      ) : comments.length === 0 ? (
        <p className="text-[12px] text-[#9ca3af]">아직 댓글이 없어요. 첫 댓글을 남겨 보세요.</p>
      ) : (
        <ul className="flex w-full flex-col gap-3">
          {comments.map((comment) => {
            const mine = comment.authorId === myUserId;
            const isEditing = editing?.id === comment.id;
            const isNew = newIds.has(comment.id);
            return (
              <li
                key={comment.id}
                className={`group/comment -mx-2 flex gap-2.5 rounded-lg px-2 py-1 transition-colors ${isNew ? "bg-[#eeeefe]" : ""}`}
              >
                <span
                  aria-hidden
                  className={`flex size-7 shrink-0 items-center justify-center rounded-full text-[12px] font-semibold ${
                    mine ? "bg-[#6366f1] text-white" : "bg-[#eeeefe] text-[#4f46e5]"
                  }`}
                >
                  {comment.authorName.slice(0, 1)}
                </span>
                <div className="flex min-w-0 flex-1 flex-col gap-1">
                  <div className="flex items-center gap-1.5">
                    <span className="text-[12.5px] font-semibold text-[#111827]">{comment.authorName}</span>
                    <span className="text-[11px] text-[#9ca3af]" title={comment.createdAt.replace("T", " ").slice(0, 16)}>
                      {formatCommentTime(comment.createdAt)}
                      {comment.edited && " (수정됨)"}
                    </span>
                    {isNew && (
                      <span className="rounded-full bg-[#6366f1] px-1.5 text-[9.5px] font-bold leading-[15px] text-white">NEW</span>
                    )}
                    {!isEditing && (
                      <span className="ml-auto flex shrink-0 gap-0.5 opacity-0 group-hover/comment:opacity-100 focus-within:opacity-100">
                        {mine && (
                          <button
                            type="button"
                            onClick={() => setEditing({ id: comment.id, text: comment.content })}
                            className="rounded px-1.5 py-0.5 text-[11px] text-[#9ca3af] hover:bg-[#f0f0f2] hover:text-[#6366f1]"
                          >
                            수정
                          </button>
                        )}
                        {canDeleteComment(comment, myUserId, isAdmin) && (
                          <button
                            type="button"
                            onClick={() => void remove(comment)}
                            className="rounded px-1.5 py-0.5 text-[11px] text-[#9ca3af] hover:bg-[#fee2e2] hover:text-[#ef4444]"
                          >
                            삭제
                          </button>
                        )}
                      </span>
                    )}
                  </div>
                  {isEditing ? (
                    <div className="flex flex-col gap-1.5">
                      <textarea
                        ref={editRef}
                        autoFocus
                        rows={1}
                        maxLength={MAX_COMMENT_LENGTH}
                        value={editing.text}
                        onChange={(e) => setEditing({ id: comment.id, text: e.target.value })}
                        onKeyDown={(e) => handleSubmitKeys(e, () => void saveEdit(), () => setEditing(null))}
                        aria-label="댓글 수정"
                        className={`${DESCRIPTION_INPUT_CLASS} min-h-[40px] text-[12.5px]`}
                      />
                      <div className="flex justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => setEditing(null)}
                          className="rounded-md px-2.5 py-1 text-[12px] text-[#6b7280] hover:bg-[#f0f0f2]"
                        >
                          취소
                        </button>
                        <button
                          type="button"
                          onClick={() => void saveEdit()}
                          disabled={!editing.text.trim() || sending}
                          className="rounded-md bg-[#6366f1] px-2.5 py-1 text-[12px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
                        >
                          저장
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="break-keep whitespace-pre-wrap text-[12.5px] text-[#374151] [overflow-wrap:anywhere]">
                      {comment.content}
                    </p>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <div className="flex w-full items-end gap-2">
        <textarea
          ref={inputRef}
          rows={1}
          maxLength={MAX_COMMENT_LENGTH}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => handleSubmitKeys(e, () => void send())}
          placeholder="댓글 남기기 (Shift+Enter 줄바꿈)"
          aria-label="댓글 입력"
          className={`${DESCRIPTION_INPUT_CLASS} min-h-[38px] flex-1 text-[12.5px]`}
        />
        <button
          type="button"
          onClick={() => void send()}
          disabled={!text.trim() || sending}
          className="shrink-0 rounded-md bg-[#6366f1] px-3 py-2 text-[12.5px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
        >
          {sending && !editing ? "보내는 중…" : "보내기"}
        </button>
      </div>
    </div>
  );
}
