// 타임라인/알림 REST API. 실시간 반영은 subscribeTopic("/topic/timeline", ...)로 처리한다 (realtime.ts).

import { apiRequest } from "./api";
import type { TimelineEventDto } from "../types/dashboard";

export function fetchTimeline(limit = 50): Promise<TimelineEventDto[]> {
  return apiRequest<TimelineEventDto[]>(`/api/timeline?limit=${limit}`);
}

/** 본인 기록이거나 OWNER/ADMIN만 성공함 (서버에서 검증). */
export function deleteTimelineEvent(id: number): Promise<void> {
  return apiRequest<void>(`/api/timeline/${id}`, { method: "DELETE" });
}
