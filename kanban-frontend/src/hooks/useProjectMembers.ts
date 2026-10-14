import { useEffect, useState } from "react";
import { listProjectMembers, type ProjectMemberDto } from "../lib/projectMembers";

/** 프로젝트 멤버 목록 — 카드 담당자 고르기에 쓴다. 실시간 갱신은 없고, 프로젝트를 바꾸면 다시 불러온다. */
export function useProjectMembers(projectId: number) {
  const [members, setMembers] = useState<ProjectMemberDto[]>([]);

  useEffect(() => {
    let cancelled = false;

    listProjectMembers(projectId)
      .then((data) => {
        if (!cancelled) setMembers(data);
      })
      .catch(() => {
        /* 실패하면 담당자 목록 없이 보여준다 — 담당자 없이도 카드는 만들 수 있다. */
      });

    return () => {
      cancelled = true;
    };
  }, [projectId]);

  return members;
}
