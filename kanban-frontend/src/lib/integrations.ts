// GitHub/Slack/Google Drive "연동" REST API.

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

export function fetchIntegrations(): Promise<IntegrationStatus[]> {
  return apiRequest<IntegrationStatus[]>("/api/integrations");
}

export function disconnectIntegration(provider: IntegrationProvider): Promise<void> {
  return apiRequest<void>(`/api/integrations/${provider}`, { method: "DELETE" });
}

function connectUrlPath(provider: IntegrationProvider): string {
  const slug = provider === "GOOGLE_DRIVE" ? "drive" : provider.toLowerCase();
  return `/api/integrations/${slug}/connect`;
}

const RETURN_PATH_KEY = "integrationReturnPath";

/** 연결하기 버튼 핸들러에서 호출 — authorizeUrl을 받아서 전체 페이지 이동으로 OAuth 동의 화면에 보낸다. */
export async function startConnect(provider: IntegrationProvider): Promise<void> {
  const { authorizeUrl } = await apiRequest<{ authorizeUrl: string }>(connectUrlPath(provider));
  // 백엔드 콜백은 어느 프로젝트에서 시작했는지 모르고 /members로만 돌려보낸다 —
  // 돌아올 팀원 페이지 경로(/projects/{id}/members)를 여기서 기억해 둔다 (IntegrationReturnPage가 읽음).
  try {
    sessionStorage.setItem(RETURN_PATH_KEY, window.location.pathname);
  } catch {
    // 저장이 막힌 환경이면 연동 후 홈으로 돌아간다.
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

export function setGithubRepo(owner: string, repo: string): Promise<void> {
  return apiRequest<void>("/api/integrations/github/repo", {
    method: "PUT",
    body: JSON.stringify({ owner, repo }),
  });
}

export function fetchGithubIssues(): Promise<GithubIssue[]> {
  return apiRequest<GithubIssue[]>("/api/integrations/github/issues");
}

/** OAuth 없이 Slack Incoming Webhook 주소로 바로 연결한다 — 서버가 그 주소로 확인 메시지를 보내 검증한 뒤 저장. */
export function connectSlackWebhook(webhookUrl: string): Promise<void> {
  return apiRequest<void>("/api/integrations/slack/webhook", {
    method: "POST",
    body: JSON.stringify({ webhookUrl }),
  });
}

export function sendSlackTestMessage(): Promise<void> {
  return apiRequest<void>("/api/integrations/slack/test", { method: "POST" });
}

export function fetchDriveFiles(): Promise<DriveFile[]> {
  return apiRequest<DriveFile[]>("/api/integrations/drive/files");
}
