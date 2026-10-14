// 팀원 접속 현황(presence) REST API — 프로젝트별로 그 프로젝트의 멤버만 보여준다.
// 실시간 반영은 subscribeTopic(`/topic/projects/${projectId}/presence`, ...)로 처리한다 (realtime.ts).

import { apiRequest } from "./api";
import type { PresenceStatus } from "../types/dashboard";

export interface PresenceEvent {
  userId: number;
  userName: string;
  status: "ONLINE" | "OFFLINE";
}

export function toPresenceStatus(status: "ONLINE" | "OFFLINE"): PresenceStatus {
  return status === "ONLINE" ? "online" : "offline";
}

export function fetchPresenceRoster(projectId: number): Promise<PresenceEvent[]> {
  return apiRequest<PresenceEvent[]>(`/api/projects/${projectId}/presence`);
}
