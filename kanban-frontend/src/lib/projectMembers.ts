// 프로젝트별 멤버 목록/초대/권한변경/초대 수락·거절 API.
// 사용자를 찾는 검색 자체는 전역이라(이메일/휴대폰번호로 가입자 검색) ./user.ts의 searchMember를 그대로 쓰고,
// 여기는 "찾은 사용자를 이 프로젝트에 초대하기/권한 바꾸기"와 "내가 받은 초대 수락·거절"을 담당한다.
// 초대는 바로 멤버가 되는 게 아니라 PENDING으로 생기고, 초대받은 사람이 수락해야 실제 멤버가 된다
// (그 전까지는 listProjectMembers에도 안 뜨고, 그 프로젝트의 어떤 기능도 못 쓴다).

import { apiRequest } from "./api";

export interface ProjectMemberDto {
  userId: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  joinedAt: string;
}

export function listProjectMembers(projectId: number): Promise<ProjectMemberDto[]> {
  return apiRequest<ProjectMemberDto[]>(`/api/projects/${projectId}/members`);
}

/** 이미 가입된 사용자에게 이 프로젝트 초대를 보낸다. OWNER/ADMIN만 성공함 — 상대가 수락해야 실제 멤버가 된다. */
export function inviteMember(projectId: number, userId: number, role: string): Promise<ProjectMemberDto> {
  return apiRequest<ProjectMemberDto>(`/api/projects/${projectId}/members`, {
    method: "POST",
    body: JSON.stringify({ userId, role }),
  });
}

/** 멤버 권한 변경. OWNER/ADMIN만 성공함. */
export function updateMemberRole(projectId: number, userId: number, role: string): Promise<ProjectMemberDto> {
  return apiRequest<ProjectMemberDto>(`/api/projects/${projectId}/members/${userId}/role`, {
    method: "PATCH",
    body: JSON.stringify({ role }),
  });
}

/** 나에게 온(아직 수락/거절 안 한) 초대 하나. */
export interface InvitationDto {
  projectId: number;
  projectName: string;
  projectDescription: string | null;
  /** 수락하면 내가 이 프로젝트에서 갖게 될 role. */
  role: string;
  invitedAt: string;
}

/** 내가 받은 초대 전체 — 프로젝트를 가리지 않고 모아서 보여준다 (메인화면 알림 벨/초대 섹션). */
export function listMyInvitations(): Promise<InvitationDto[]> {
  return apiRequest<InvitationDto[]>("/api/invitations");
}

export function acceptInvitation(projectId: number): Promise<InvitationDto> {
  return apiRequest<InvitationDto>(`/api/invitations/${projectId}/accept`, { method: "POST" });
}

/** 거절하면 초대 자체가 지워진다 — 다시 받으려면 관리자가 새로 초대해야 한다. */
export function declineInvitation(projectId: number): Promise<void> {
  return apiRequest<void>(`/api/invitations/${projectId}/decline`, { method: "POST" });
}
