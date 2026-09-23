import { useEffect, useState } from "react";
import { fetchPresenceRoster, toPresenceStatus, type PresenceEvent } from "../lib/presence";
import { subscribeTopic } from "../lib/realtime";
import type { TeamMember } from "../types/dashboard";

function toTeamMember(event: PresenceEvent): TeamMember {
  return {
    id: String(event.userId),
    name: event.userName,
    initial: event.userName.slice(0, 1),
    status: toPresenceStatus(event.status),
  };
}

/** 팀 전체 명단 + 현재 접속 상태를 불러오고, 이후 /topic/presence로 오는 온/오프라인 변화를 반영한다. */
export function useTeamPresence() {
  const [members, setMembers] = useState<TeamMember[]>([]);

  useEffect(() => {
    let cancelled = false;

    fetchPresenceRoster().then((roster) => {
      if (!cancelled) setMembers(roster.map(toTeamMember));
    });

    const unsubscribe = subscribeTopic<PresenceEvent>("/topic/presence", (event) => {
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
    };
  }, []);

  return members;
}
