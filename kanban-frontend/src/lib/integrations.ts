// GitHub/Slack/Google Drive "연동" REST API — 프로젝트마다 따로 연결한다.

import { apiRequest } from "./api";

export type IntegrationProvider = "GITHUB" | "SLACK" | "GOOGLE_DRIVE";

export interface IntegrationStatus {
  provider: IntegrationProvider;
  connected: boolean;
  accountLabel: string | null;
  detail: string | null;
  connectedAt: string | null;
}

export interface GithubIssue {
  number: number;
  title: string;
  state: string;
  htmlUrl: string;
  authorLogin: string;
  createdAt: string;
}

export interface DriveFile {
  id: string;
  name: string;
  webViewLink: string;
  mimeType: string;
  modifiedTime: string;
}

/** 이 프로젝트에 연결된 연동 상태 — 연동은 프로젝트마다 따로 저장된다. */
export function fetchIntegrations(projectId: number): Promise<IntegrationStatus[]> {
  return apiRequest<IntegrationStatus[]>(`/api/projects/${projectId}/integrations`);
}

export function disconnectIntegration(projectId: number, provider: IntegrationProvider): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}/integrations/${provider}`, { method: "DELETE" });
}

const RETURN_PATH_KEY = "integrationReturnPath";

function providerSlug(provider: IntegrationProvider): string {
  return provider === "GOOGLE_DRIVE" ? "drive" : provider.toLowerCase();
}

/**
 * 연결하기 버튼 핸들러에서 호출 — authorizeUrl을 받아서 전체 페이지 이동으로 OAuth 동의 화면에 보낸다.
 * 동의가 끝나면 서버가 이 프로젝트의 멤버 페이지(/projects/{id}/members)로 돌려보낸다.
 */
export async function startConnect(projectId: number, provider: IntegrationProvider): Promise<void> {
  const { authorizeUrl } = await apiRequest<{ authorizeUrl: string }>(
    `/api/projects/${projectId}/integrations/${providerSlug(provider)}/connect`,
  );
  // 예전 콜백 주소(/members)로 돌아오는 경우를 위한 대비 — IntegrationReturnPage가 이 경로로 넘겨준다.
  try {
    sessionStorage.setItem(RETURN_PATH_KEY, window.location.pathname);
  } catch {
    // 저장이 막힌 환경이면 그 경우에만 홈으로 돌아간다.
  }
  window.location.href = authorizeUrl;
}

/** startConnect가 기억해 둔 "연동을 시작한 페이지" 경로 — 없으면 null. */
export function getIntegrationReturnPath(): string | null {
  try {
    return sessionStorage.getItem(RETURN_PATH_KEY);
  } catch {
    return null;
  }
}

export function setGithubRepo(projectId: number, owner: string, repo: string): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}/integrations/github/repo`, {
    method: "PUT",
    body: JSON.stringify({ owner, repo }),
  });
}

export interface GithubRepo {
  name: string;
  isPrivate: boolean;
  description: string | null;
}

/** 소유자(사용자 또는 조직)의 저장소 목록, 최근에 고친 순. 연결한 계정 본인이면 비공개 저장소도 나온다. */
export function fetchGithubRepos(projectId: number, owner: string): Promise<GithubRepo[]> {
  return apiRequest<GithubRepo[]>(
    `/api/projects/${projectId}/integrations/github/repos?owner=${encodeURIComponent(owner)}`,
  );
}

export function fetchGithubIssues(projectId: number): Promise<GithubIssue[]> {
  return apiRequest<GithubIssue[]>(`/api/projects/${projectId}/integrations/github/issues`);
}

/** OAuth 없이 Slack Incoming Webhook 주소로 바로 연결한다 — 서버가 그 주소로 확인 메시지를 보내 검증한 뒤 저장. */
export function connectSlackWebhook(projectId: number, webhookUrl: string): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}/integrations/slack/webhook`, {
    method: "POST",
    body: JSON.stringify({ webhookUrl }),
  });
}

export function sendSlackTestMessage(projectId: number): Promise<void> {
  return apiRequest<void>(`/api/projects/${projectId}/integrations/slack/test`, { method: "POST" });
}

export function fetchDriveFiles(projectId: number): Promise<DriveFile[]> {
  return apiRequest<DriveFile[]>(`/api/projects/${projectId}/integrations/drive/files`);
}
