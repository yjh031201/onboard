import { useEffect, useState } from "react";
import { getProject, type ProjectSummary } from "../lib/projects";

export type ProjectRole = ProjectSummary["myRole"];

/**
 * 이 프로젝트에서 내 role. 버튼 노출(수정/삭제 등)에 쓴다 — 실제 권한은 서버가 다시 검사한다.
 * 전역 getStoredUser().role은 멀티 프로젝트 전환 후 항상 MEMBER라서 프로젝트별 권한 판단에 쓰면 안 된다.
 * 불러오기 전이나 실패하면 null(관리자 권한 없음으로 취급).
 */
export function useMyProjectRole(projectId: number): ProjectRole | null {
  const [role, setRole] = useState<ProjectRole | null>(null);

  useEffect(() => {
    let cancelled = false;
    setRole(null);
    getProject(projectId)
      .then((project) => {
        if (!cancelled) setRole(project.myRole);
      })
      .catch(() => {
        // 못 불러와도 화면은 그대로 쓴다 — 관리자 전용 버튼만 안 보인다.
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  return role;
}

export function isProjectAdmin(role: ProjectRole | null): boolean {
  return role === "OWNER" || role === "ADMIN";
}
