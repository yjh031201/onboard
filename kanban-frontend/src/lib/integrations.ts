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

/** 연결하기 버튼 핸들러에서 호출 — authorizeUrl을 받아서 전체 페이지 이동으로 OAuth 동의 화면에 보낸다. */
export async function startConnect(provider: IntegrationProvider): Promise<void> {
  const { authorizeUrl } = await apiRequest<{ authorizeUrl: string }>(connectUrlPath(provider));
  window.location.href = authorizeUrl;
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

export function sendSlackTestMessage(): Promise<void> {
  return apiRequest<void>("/api/integrations/slack/test", { method: "POST" });
}

export function fetchDriveFiles(): Promise<DriveFile[]> {
  return apiRequest<DriveFile[]>("/api/integrations/drive/files");
}
