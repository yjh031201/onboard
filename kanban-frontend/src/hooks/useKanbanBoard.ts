import { useEffect, useState } from "react";
import { fetchCards, toTaskCards, type BoardEvent } from "../lib/board";
import { subscribeTopic } from "../lib/realtime";
import type { TaskCard } from "../types/dashboard";

/** 카드 목록을 불러오고, 이후에는 /topic/projects/{projectId}/board로 오는 실시간 변경(생성/이동/라벨 변경/삭제)을 그대로 반영한다. */
export function useKanbanBoard(projectId: number) {
  const [tasks, setTasks] = useState<TaskCard[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    fetchCards(projectId)
      .then((cards) => {
        if (!cancelled) setTasks(cards);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    const unsubscribe = subscribeTopic<BoardEvent>(`/topic/projects/${projectId}/board`, (event) => {
      setTasks(toTaskCards(event.cards));
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [projectId]);

  return { tasks, loading };
}
