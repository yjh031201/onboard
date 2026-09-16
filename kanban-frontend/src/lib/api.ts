// 백엔드와 통신하는 모든 API 호출의 기반이 되는 파일.
// 앞으로 칸반보드/팀/파일 등 새 기능을 만들 때는 이 파일의 apiRequest()를 사용하면
// 로그인 토큰이 자동으로 실리고, 토큰이 만료됐을 때도 자동으로 처리돼요.

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "kanban_access_token";
const USER_KEY = "kanban_user";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(token: string, user: unknown) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

/** 토큰은 그대로 두고 캐시된 유저 정보만 갱신 (프로필 수정 후 사용). */
export function updateStoredUser(user: unknown) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getStoredUser<T = unknown>(): T | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

async function parseErrorMessage(res: Response): Promise<string> {
  const body = await res.json().catch(() => null);
  return body?.message ?? "요청 처리 중 오류가 발생했어요.";
}

async function toResult<T>(res: Response): Promise<T> {
  if (res.status === 204) {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

/** 로그인 없이 호출하는 API (회원가입, 로그인 등). */
export async function publicRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!res.ok) {
    throw new ApiError(res.status, await parseErrorMessage(res));
  }
  return toResult<T>(res);
}

/**
 * 로그인 토큰이 필요한 모든 API 호출은 이 함수로.
 * - Authorization: Bearer <토큰> 헤더 자동 첨부
 * - 401(토큰 만료/무효) 응답이면 세션 정리하고 로그인 페이지로 자동 이동
 */
export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (res.status === 401) {
    clearSession();
    if (typeof window !== "undefined" && window.location.pathname !== "/login") {
      window.location.href = "/login";
    }
    throw new ApiError(401, "로그인이 만료되었어요. 다시 로그인해주세요.");
  }

  if (!res.ok) {
    throw new ApiError(res.status, await parseErrorMessage(res));
  }

  return toResult<T>(res);
}
