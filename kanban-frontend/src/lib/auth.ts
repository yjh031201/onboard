// 로그인/회원가입 관련 API. 공통 fetch 로직은 ./api.ts 참고.

import {
  API_BASE_URL,
  ApiError,
  apiRequest,
  clearSession,
  getStoredUser as getStoredUserRaw,
  getToken,
  isLoggedIn,
  publicRequest,
  setSession,
} from "./api";

export { ApiError, getToken, isLoggedIn };

export interface AuthUser {
  id: number;
  email: string;
  name: string;
  phone: string;
  role: string;
}

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return publicRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  }).then((auth) => {
    setSession(auth.accessToken, auth.user);
    return auth;
  });
}

export function signup(
  name: string,
  email: string,
  password: string,
  phone: string,
): Promise<AuthResponse> {
  return publicRequest<AuthResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify({ name, email, password, phone }),
  }).then((auth) => {
    setSession(auth.accessToken, auth.user);
    return auth;
  });
}

export function logout() {
  clearSession();
}

export interface FindIdResponse {
  maskedEmail: string;
}

/** 이름+휴대폰번호로 본인 확인 후 마스킹된 이메일(아이디)을 돌려줌. */
export function findId(name: string, phone: string): Promise<FindIdResponse> {
  return publicRequest<FindIdResponse>("/api/auth/find-id", {
    method: "POST",
    body: JSON.stringify({ name, phone }),
  });
}

/** 이름+이메일 일치 확인 후 새 비밀번호로 즉시 교체 (이메일 발송 없는 간소화된 플로우). */
export function resetPassword(name: string, email: string, newPassword: string): Promise<void> {
  return publicRequest<void>("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify({ name, email, newPassword }),
  });
}

/** 구글/네이버 로그인 시작 — 전체 페이지 이동으로 백엔드 OAuth2 엔드포인트로 보낸다. */
export function loginWithProvider(provider: "google" | "naver") {
  window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
}

/**
 * OAuthCallbackPage에서 사용. 백엔드가 리다이렉트 쿼리스트링으로 실어 보낸 1회용 교환 코드를
 * 실제 access token으로 바꾼다 (refresh token은 응답 body가 아니라 httpOnly 쿠키로 내려옴).
 */
export async function completeOAuthLogin(code: string): Promise<AuthUser> {
  const auth = await publicRequest<AuthResponse>("/api/auth/oauth/exchange", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
  setSession(auth.accessToken, auth.user);
  return auth.user;
}

export function getStoredUser(): AuthUser | null {
  return getStoredUserRaw<AuthUser>();
}

/** 서버에 토큰으로 내 정보 재확인할 때 사용 (선택적으로 쓰면 됨). */
export function fetchMe(): Promise<AuthUser> {
  return apiRequest<AuthUser>("/api/auth/me");
}
