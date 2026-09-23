// 설정 페이지 "일반"(프로젝트 이름/설명) 관련 API.

import { apiRequest } from "./api";

export interface ProjectSettings {
  id: number | null; // 아직 아무도 저장 안 했으면 null
  projectName: string;
  description: string;
  updatedBy: number | null;
  updatedAt: string | null;
}

export function getProjectSettings(): Promise<ProjectSettings> {
  return apiRequest<ProjectSettings>("/api/settings");
}

/** OWNER/ADMIN만 성공함 (서버에서 검증). */
export function updateProjectSettings(projectName: string, description: string): Promise<ProjectSettings> {
  return apiRequest<ProjectSettings>("/api/settings", {
    method: "PUT",
    body: JSON.stringify({ projectName, description }),
  });
}
