// 설정 페이지 "일반"(프로젝트 이름/설명)·"프로젝트 관리"(보관/삭제) 관련 API.

import { apiRequest } from "./api";
import { subscribeTopic } from "./realtime";

export interface ProjectSettings {
  id: number | null; // 아직 아무도 저장 안 했으면 null
  projectName: string;
  description: string;
  /** 팀원 페이지의 팀 이름. 아직 아무도 바꾸지 않았으면 빈 문자열. */
  teamName: string;
  updatedBy: number | null;
  updatedAt: string | null;
  /** 보관 중이면 프로젝트 전체가 읽기 전용 — 서버가 쓰기 요청을 423으로 거절한다. */
  archived: boolean;
  archivedAt: string | null;
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

/** 팀 이름 변경. OWNER/ADMIN만 성공함 (서버에서 검증). */
export function updateTeamName(teamName: string): Promise<ProjectSettings> {
  return apiRequest<ProjectSettings>("/api/settings/team-name", {
    method: "PUT",
    body: JSON.stringify({ teamName }),
  });
}

type SettingsHandler =(settings: ProjectSettings) => void;
const localHandlers = new Set<SettingsHandler>();

function notifyLocal(settings: ProjectSettings): ProjectSettings {
  localHandlers.forEach((handler) => handler(settings));
  return settings;
}

/**
 * 보관 상태 등 프로젝트 설정이 바뀌면 불린다. 다른 사람이 바꾼 것은 /topic/settings로 오고,
 * 내가 바꾼 것은 실시간 연결이 끊겨 있어도 바로 반영되도록 응답을 받자마자 직접 알린다.
 */
export function subscribeProjectSettings(handler: SettingsHandler): () => void {
  localHandlers.add(handler);
  const unsubscribe = subscribeTopic<ProjectSettings>("/topic/settings", handler);
  return () => {
    localHandlers.delete(handler);
    unsubscribe();
  };
}

/** 프로젝트를 읽기 전용으로 전환. OWNER/ADMIN만 성공함. */
export function archiveProject(): Promise<ProjectSettings> {
  return apiRequest<ProjectSettings>("/api/settings/archive", { method: "POST" }).then(notifyLocal);
}

export function unarchiveProject(): Promise<ProjectSettings> {
  return apiRequest<ProjectSettings>("/api/settings/unarchive", { method: "POST" }).then(notifyLocal);
}

/**
 * 카드·일정·파일·타임라인·프로젝트 이름/설명을 모두 지운다. 되돌릴 수 없고 OWNER만 성공함.
 * 팀원 계정과 보드 구성(컬럼·라벨)은 남는다.
 */
export function deleteProject(): Promise<void> {
  return apiRequest<void>("/api/settings/project", { method: "DELETE" });
}
