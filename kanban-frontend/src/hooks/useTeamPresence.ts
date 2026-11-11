import { useEffect, useState } from "react";
import { fetchPresenceRoster, toPresenceStatus, type PresenceEvent } from "../lib/presence";
import { onRealtimeReconnect, subscribeTopic } from "../lib/realtime";
import type { TeamMember } from "../types/dashboard";

function toTeamMember(event: PresenceEvent): TeamMember {
  return {
    id: String(event.userId),
    name: event.userName,
    initial: event.userName.slice(0, 1),
    status: toPresenceStatus(event.status),
  };
}

/** 이 프로젝트 팀 명단 + 현재 접속 상태를 불러오고, 이후 /topic/projects/{projectId}/presence로 오는 온/오프라인 변화를 반영한다. */
export function useTeamPresence(projectId: number) {
  const [members, setMembers] = useState<TeamMember[]>([]);

  useEffect(() => {
    let cancelled = false;

    const load = () =>
      fetchPresenceRoster(projectId)
        .then((roster) => {
          if (!cancelled) setMembers(roster.map(toTeamMember));
        })
        .catch(() => {
          /* 실패하면 지금 명단을 그대로 둔다. */
        });
    void load();
    // 끊긴 동안의 접속·종료는 이벤트로 못 받았으므로 다시 연결되면 명단을 새로 받는다.
    const unsubscribeReconnect = onRealtimeReconnect(() => void load());

    const unsubscribe = subscribeTopic<PresenceEvent>(`/topic/projects/${projectId}/presence`, (event) => {
      setMembers((prev) => {
        const updated = toTeamMember(event);
        const index = prev.findIndex((member) => member.id === updated.id);
        if (index === -1) return [...prev, updated];

        const next = [...prev];
        next[index] = updated;
        return next;
      });
    });

    return () => {
      cancelled = true;
      unsubscribe();
      unsubscribeReconnect();
    };
  }, [projectId]);

  return members;
}
