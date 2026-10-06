// 프로젝트 목록/생성/조회/수정 API. 메인화면(ProjectSelectPage)과 설정 페이지가 사용한다.
// 한 사람이 여러 프로젝트를 가질 수 있고, 각 프로젝트는 독립된 보드/라벨/컬럼/타임라인을 가진다.

import { apiRequest } from "./api";

export interface ProjectSummary {
  id: number;
  name: string;
  description: string | null;
  /** 내가 이 프로젝트에서 가진 role. */
  myRole: "OWNER" | "ADMIN" | "MEMBER";
  createdAt: string;
  updatedAt: string;
}

/** 내가 멤버인 프로젝트 목록 — 메인화면이 보여주는 목록. */
export function listProjects(): Promise<ProjectSummary[]> {
  return apiRequest<ProjectSummary[]>("/api/projects");
}

export function getProject(projectId: number): Promise<ProjectSummary> {
  return apiRequest<ProjectSummary>(`/api/projects/${projectId}`);
}

/** 생성한 사람이 바로 OWNER가 되고, 기본 컬럼 3개 + 기본 라벨 5개가 같이 만들어진다 (서버에서 처리). */
export function createProject(name: string, description: string): Promise<ProjectSummary> {
  return apiRequest<ProjectSummary>("/api/projects", {
    method: "POST",
    body: JSON.stringify({ name, description }),
  });
}

/** OWNER/ADMIN만 성공함 (서버에서 검증). */
export function updateProject(projectId: number, name: string, description: string): Promise<ProjectSummary> {
  return apiRequest<ProjectSummary>(`/api/projects/${projectId}`, {
    method: "PUT",
    body: JSON.stringify({ name, description }),
  });
}
