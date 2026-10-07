// 프로젝트 목록/생성/조회/수정/보관/삭제 API. 메인화면(ProjectSelectPage)과 설정 페이지가 사용한다.
// 한 사람이 여러 프로젝트를 가질 수 있고, 각 프로젝트는 독립된 보드/라벨/컬럼/타임라인을 가진다.

import { apiRequest } from "./api";
import { subscribeTopic } from "./realtime";

export interface ProjectSummary {
  id: number;
  name: string;
  description: string | null;
  /** 내가 이 프로젝트에서 가진 role. */
  myRole: "OWNER" | "ADMIN" | "MEMBER";
  /** 보관 중이면 이 프로젝트 전체가 읽기 전용 — 서버가 쓰기 요청을 423으로 거절한다. */
  archived: boolean;
  archivedAt: string | null;
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

/** 프로젝트를 읽기 전용으로 전환. OWNER/ADMIN만 성공함. */
export function archiveProject(projectId: number): Promise<ProjectSummary> {
  return apiRequest<ProjectSummary>(`/api/projects/${projectId}/archive`, { method: "POST" });
}

export function unarchiveProject(projectId: number): Promise<ProjectSummary> {
  return apiRequest<ProjectSummary>(`/api/projects/${projectId}/unarchive`, { method: "POST" });
}

/**
 * 이 프로젝트의 카드·컬럼·라벨·타임라인·멤버십을 영구 삭제한다. 되돌릴 수 없고 OWNER만 성공함.
 * (팀원 계정 자체와, 아직 프로젝트별로 나뉘지 않은 일정·파일은 남는다.)
 */
export function deleteProject(projectId: number): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}`, { method: "DELETE" });
}

type ProjectSettingsHandler = (project: ProjectSummary) => void;

/** 이름/설명/보관 상태 등 프로젝트 설정이 바뀌면 불린다 — 다른 관리자가 바꾼 것도 바로 반영된다. */
export function subscribeProjectSettings(projectId: number, handler: ProjectSettingsHandler): () => void {
  return subscribeTopic<ProjectSummary>(`/topic/projects/${projectId}/settings`, handler);
}
