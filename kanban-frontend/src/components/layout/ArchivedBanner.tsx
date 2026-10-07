import { useEffect, useState } from "react";
import { useProjectId } from "../../hooks/useProjectId";
import { getProject, subscribeProjectSettings } from "../../lib/projects";

/** 지금 보고 있는 프로젝트가 보관 중이면 어느 페이지에서든 읽기 전용임을 알려 준다. */
export default function ArchivedBanner() {
  const projectId = useProjectId();
  const [archived, setArchived] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getProject(projectId)
      .then((project) => {
        if (!cancelled) setArchived(project.archived);
      })
      .catch(() => {
        // 못 불러와도 화면은 그대로 쓴다 — 쓰기 요청은 어차피 서버가 막는다.
      });
    const unsubscribe = subscribeProjectSettings(projectId, (project) => setArchived(project.archived));
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [projectId]);

  if (!archived) return null;

  return (
    <div className="pointer-events-none fixed inset-x-0 top-2 z-40 flex justify-center">
      <p className="rounded-full border border-[#fde68a] bg-[#fffbeb] px-3.5 py-1.5 text-[12.5px] font-medium text-[#92400e] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.06)]">
        📦 보관된 프로젝트예요 — 읽기 전용
      </p>
    </div>
  );
}
