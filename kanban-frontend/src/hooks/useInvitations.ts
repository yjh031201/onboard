import { useEffect, useState } from "react";
import {
  acceptInvitation,
  declineInvitation,
  listMyInvitations,
  type InvitationDto,
} from "../lib/projectMembers";

/** 내가 받은 프로젝트 초대 목록 — 메인화면 알림 벨과 "나에게 온 초대" 섹션이 같이 쓴다. */
export function useInvitations() {
  const [invitations, setInvitations] = useState<InvitationDto[]>([]);
  const [loading, setLoading] = useState(true);

  const reload = () => {
    setLoading(true);
    listMyInvitations()
      .then(setInvitations)
      .catch(() => {
        /* 실패하면 그냥 빈 목록으로 둔다 — 메인화면 핵심 기능(프로젝트 목록)은 아니라서. */
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
  }, []);

  const accept = (projectId: number) =>
    acceptInvitation(projectId).then((result) => {
      setInvitations((prev) => prev.filter((i) => i.projectId !== projectId));
      return result;
    });

  const decline = (projectId: number) =>
    declineInvitation(projectId).then(() => {
      setInvitations((prev) => prev.filter((i) => i.projectId !== projectId));
    });

  return { invitations, loading, accept, decline, reload };
}
