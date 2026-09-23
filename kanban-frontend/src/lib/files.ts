// 파일 페이지(프로젝트 파일 업로드/목록/다운로드/삭제) 관련 API.

import { apiBlob, apiRequest } from "./api";

/** 백엔드 spring.servlet.multipart.max-file-size와 맞출 것. */
export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

export interface ProjectFile {
  id: number;
  fileName: string;
  fileSize: number;
  contentType: string | null;
  uploadedBy: number;
  uploaderName: string | null;
  createdAt: string;
}

export function listFiles(): Promise<ProjectFile[]> {
  return apiRequest<ProjectFile[]>("/api/files");
}

export function uploadFile(file: File): Promise<ProjectFile> {
  const form = new FormData();
  form.append("file", file);
  return apiRequest<ProjectFile>("/api/files", { method: "POST", body: form });
}

/** 인증 헤더가 필요해서 <a href>로는 못 받음 — Blob으로 받아 임시 링크로 저장시킨다. */
export async function downloadFile(file: ProjectFile): Promise<void> {
  const blob = await apiBlob(`/api/files/${file.id}/download`);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = file.fileName;
  link.click();
  URL.revokeObjectURL(url);
}

/** 업로더 본인 또는 OWNER/ADMIN만 성공함 (서버에서 검증). */
export function deleteFile(id: number): Promise<void> {
  return apiRequest<void>(`/api/files/${id}`, { method: "DELETE" });
}
