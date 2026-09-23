// 로그인한 본인의 프로필/비밀번호 관리 API. 로그인/회원가입 자체는 ./auth.ts 참고.

import { apiRequest, updateStoredUser } from "./api";
import type { AuthUser } from "./auth";

export function updateProfile(name: string, phone: string): Promise<AuthUser> {
  return apiRequest<AuthUser>("/api/users/me", {
    method: "PATCH",
    body: JSON.stringify({ name, phone }),
  }).then((user) => {
    updateStoredUser(user);
    return user;
  });
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiRequest<void>("/api/users/me/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

// 팀원 페이지(구성원 목록/초대/권한 변경) 관련 API.
// 이 워크스페이스는 단일 팀 구조라, "팀원 목록" = 가입된 전체 사용자.

/** 팀원 목록 전체 조회. */
export function listMembers(): Promise<AuthUser[]> {
  return apiRequest<AuthUser[]>("/api/users");
}

/** 구성원 초대용 — 이메일 또는 휴대폰번호로 이미 가입된 사용자를 검색. */
export function searchMember(keyword: string): Promise<AuthUser> {
  return apiRequest<AuthUser>(`/api/users/search?keyword=${encodeURIComponent(keyword)}`);
}

/** 팀원 권한 변경 (OWNER/ADMIN만 성공함 — 서버에서 검증). */
export function updateMemberRole(userId: number, role: string): Promise<AuthUser> {
  return apiRequest<AuthUser>(`/api/users/${userId}/role`, {
    method: "PATCH",
    body: JSON.stringify({ role }),
  });
}
