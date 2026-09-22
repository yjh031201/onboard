// 팀원 접속 현황(presence) REST API. 실시간 반영은 subscribeTopic("/topic/presence", ...)로 처리한다 (realtime.ts).

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

export function fetchPresenceRoster(): Promise<PresenceEvent[]> {
  return apiRequest<PresenceEvent[]>("/api/presence");
}
