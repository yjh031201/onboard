import { useEffect, useState } from "react";
import { fetchCards, toTaskCards, type BoardEvent } from "../lib/board";
import { onRealtimeReconnect, subscribeTopic } from "../lib/realtime";
import { syncCommentReads } from "../lib/commentReads";
import type { TaskCard } from "../types/dashboard";

/**
 * 카드 목록을 불러오고, 이후에는 /topic/projects/{projectId}/board로 오는 실시간 변경(생성/이동/라벨 변경/삭제)을 그대로 반영한다.
 * 실시간 연결이 끊겼다 다시 붙으면(배포로 서버 재시작 등) 그 사이 바뀐 카드를 놓쳤을 수 있어 다시 불러온다.
 */
export function useKanbanBoard(projectId: number) {
  const [tasks, setTasks] = useState<TaskCard[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const load = () =>
      fetchCards(projectId)
        .then((cards) => {
          if (!cancelled) setTasks(cards);
        })
        .catch(() => {
          /* 실패하면 지금 카드를 그대로 두고, 다음 board 스냅샷 때 맞춰진다. */
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    void load();

    const unsubscribe = subscribeTopic<BoardEvent>(`/topic/projects/${projectId}/board`, (event) => {
      setTasks(toTaskCards(event.cards));
    });
    const unsubscribeReconnect = onRealtimeReconnect(() => void load());

    return () => {
      cancelled = true;
      unsubscribe();
      unsubscribeReconnect();
    };
  }, [projectId]);

  // 새 댓글(+N) 배지의 기준선 — 처음 보는 카드와 댓글이 줄어든 카드의 읽은 수를 맞춘다.
  useEffect(() => {
    if (!loading) syncCommentReads(projectId, tasks);
  }, [projectId, tasks, loading]);

  return { tasks, loading };
}
