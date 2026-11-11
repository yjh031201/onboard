import { useEffect, useState } from "react";
import { fetchTimeline } from "../lib/timeline";
import { onRealtimeReconnect, subscribeTopic } from "../lib/realtime";
import type { TimelineEventDto } from "../types/dashboard";

/**
 * 최근 활동 이력을 불러오고, 이후 /topic/projects/{projectId}/timeline으로 오는 새 이벤트를 맨 앞에 이어붙인다.
 * /topic/projects/{projectId}/timeline-deleted로 삭제 알림이 오면 해당 항목을 목록에서 뺀다.
 * 실시간 연결이 끊겼다 다시 붙으면 목록을 다시 불러온다.
 */
export function useTimelineFeed(projectId: number, limit = 50) {
  const [events, setEvents] = useState<TimelineEventDto[]>([]);

  useEffect(() => {
    let cancelled = false;

    const load = () =>
      fetchTimeline(projectId, limit)
        .then((data) => {
          if (!cancelled) setEvents(data);
        })
        .catch(() => {
          /* 실패하면 지금 목록을 그대로 둔다. */
        });
    void load();

    const unsubscribe = subscribeTopic<TimelineEventDto>(`/topic/projects/${projectId}/timeline`, (event) => {
      setEvents((prev) => [event, ...prev]);
    });
    const unsubscribeDeleted = subscribeTopic<{ id: number }>(`/topic/projects/${projectId}/timeline-deleted`, ({ id }) => {
      setEvents((prev) => prev.filter((event) => event.id !== id));
    });
    const unsubscribeReconnect = onRealtimeReconnect(() => void load());

    return () => {
      cancelled = true;
      unsubscribe();
      unsubscribeDeleted();
      unsubscribeReconnect();
    };
  }, [projectId, limit]);

  return events;
}
