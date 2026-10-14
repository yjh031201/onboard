// 로그인한 본인의 프로필/비밀번호 관리 API. 로그인/회원가입 자체는 ./auth.ts 참고.
// 팀원 목록/초대/권한 변경은 프로젝트별로 바뀌어서 ./projectMembers.ts로 옮겼다 — 여기는
// 본인 프로필과(가입자 검색만 전역이라 여기 남아 있는) searchMember만 담당한다.

import { apiRequest, clearSession, updateStoredUser } from "./api";
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

/** 회원 탈퇴 — 성공하면 계정이 사라지므로 로컬 세션도 같이 지운다. 소셜 로그인 전용 계정은 password 없이 호출. */
export function deleteAccount(password?: string): Promise<void> {
  return apiRequest<void>("/api/users/me", {
    method: "DELETE",
    body: JSON.stringify({ password: password ?? null }),
  }).then(() => clearSession());
}

/** 구성원 초대용 — 이메일 또는 휴대폰번호로 이미 가입된 사용자를 검색 (전역 검색, 프로젝트와 무관). */
export function searchMember(keyword: string): Promise<AuthUser> {
  return apiRequest<AuthUser>(`/api/users/search?keyword=${encodeURIComponent(keyword)}`);
}
