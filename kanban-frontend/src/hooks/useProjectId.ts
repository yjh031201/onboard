import { useParams } from "react-router-dom";

/** "/projects/:projectId/..." 하위 페이지에서 현재 보고 있는 프로젝트의 id를 가져온다. */
export function useProjectId(): number {
  const { projectId } = useParams<{ projectId: string }>();
  return Number(projectId);
}
