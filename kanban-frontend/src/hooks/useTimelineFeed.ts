import { useEffect, useState } from "react";
import { fetchTimeline } from "../lib/timeline";
import { subscribeTopic } from "../lib/realtime";
import type { TimelineEventDto } from "../types/dashboard";

/** 최근 활동 이력을 불러오고, 이후 /topic/timeline으로 오는 새 이벤트를 맨 앞에 이어붙인다. */
export function useTimelineFeed(limit = 50) {
  const [events, setEvents] = useState<TimelineEventDto[]>([]);

  useEffect(() => {
    let cancelled = false;

    fetchTimeline(limit).then((data) => {
      if (!cancelled) setEvents(data);
    });

    const unsubscribe = subscribeTopic<TimelineEventDto>("/topic/timeline", (event) => {
      setEvents((prev) => [event, ...prev]);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [limit]);

  return events;
}
