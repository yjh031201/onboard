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
  /** 구글/네이버로 가입한 계정은 전화번호를 안 받기 때문에 null일 수 있다. */
  phone: string | null;
  role: string;
  /** "게스트로 이용"으로 들어온 계정이면 true — 프로젝트를 새로 만들 수 없고 초대받아 참여만 가능. */
  isGuest: boolean;
  /** 소셜 로그인 전용 계정이면 false — 이 필드가 생기기 전에 로그인한 세션에는 없을 수 있다. */
  hasPassword?: boolean;
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

/** 서버에도 알려서 refresh 쿠키를 지운다 — 안 그러면 로그아웃해도 그 브라우저에서 로그인을 다시 연장할 수 있다. */
export function logout() {
  clearSession();
  // 화면 이동을 기다리게 하지 않는다. keepalive로 페이지를 떠나도 요청은 끝까지 간다.
  fetch(`${API_BASE_URL}/api/auth/logout`, { method: "POST", credentials: "include", keepalive: true }).catch(() => {
    // 서버에 못 알려도 이 브라우저의 로그인 정보는 이미 지웠다.
  });
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

/** 비밀번호 재설정 1단계 — 이메일로 6자리 인증 코드를 보낸다 (가입 안 된 이메일도 같은 응답). */
export function requestPasswordResetCode(email: string): Promise<void> {
  return publicRequest<void>("/api/auth/password-reset/request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

/** 비밀번호 재설정 2단계 — 메일로 받은 코드가 맞으면 새 비밀번호로 바뀐다. */
export function confirmPasswordReset(email: string, code: string, newPassword: string): Promise<void> {
  return publicRequest<void>("/api/auth/password-reset/confirm", {
    method: "POST",
    body: JSON.stringify({ email, code, newPassword }),
  });
}

/**
 * 구글/네이버 로그인 시작 — 전체 페이지 이동으로 백엔드 OAuth2 엔드포인트로 보낸다.
 * "google-guest"는 같은 구글 로그인 창을 쓰지만 처음 가입하는 계정만 게스트(isGuest=true)로
 * 만들어진다 — 서버(CustomOAuth2UserService)가 registrationId로 구분해서 처리.
 */
export function loginWithProvider(provider: "google" | "naver" | "google-guest") {
  window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
}

/** 랜딩페이지의 "게스트로 이용" 버튼 전용 진입점. */
export function loginAsGuest() {
  loginWithProvider("google-guest");
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
