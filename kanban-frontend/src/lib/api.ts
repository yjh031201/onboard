// 백엔드와 통신하는 모든 API 호출의 기반이 되는 파일.
// 앞으로 칸반보드/팀/파일 등 새 기능을 만들 때는 이 파일의 apiRequest()를 사용하면
// 로그인 토큰이 자동으로 실리고, 토큰이 만료됐을 때도 자동으로 처리돼요.

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

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

/**
 * 로그인 없이 호출하는 API (회원가입, 로그인 등).
 * refresh token은 응답 body가 아니라 httpOnly 쿠키로 내려오므로 credentials: "include" 필수.
 */
export async function publicRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
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

let refreshing: Promise<string | null> | null = null;

/**
 * refresh 쿠키로 새 access token을 받아 저장하고 돌려준다. 실패하면 null.
 * 동시에 여러 요청이 401을 받아도 서버에는 한 번만 요청한다 — refresh token은 쓸 때마다 새것으로 바뀌어서,
 * 두 번 보내면 두 번째는 이미 바뀐 옛 토큰이라 거절되고 로그아웃돼 버린다.
 */
export function refreshAccessToken(): Promise<string | null> {
  refreshing ??= fetch(`${API_BASE_URL}/api/auth/refresh`, { method: "POST", credentials: "include" })
    .then(async (res) => {
      if (!res.ok) return null;
      const body = (await res.json()) as { accessToken: string; user: unknown };
      setSession(body.accessToken, body.user);
      return body.accessToken;
    })
    .catch(() => null)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

/**
 * 로그인 토큰이 필요한 모든 API 호출은 이 함수로.
 * - Authorization: Bearer <토큰> 헤더 자동 첨부
 * - 401(토큰 만료)이면 refresh 쿠키로 토큰을 새로 받아 한 번 다시 보낸다. 그것도 안 되면 세션 정리 후 로그인 페이지로
 * - body가 FormData(파일 업로드)면 Content-Type은 브라우저가 boundary와 함께 채우도록 비워둠
 */
export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await authorizedFetch(path, options);
  return toResult<T>(res);
}

/** 파일 다운로드처럼 JSON이 아닌 응답을 Blob으로 받을 때 사용. */
export async function apiBlob(path: string): Promise<Blob> {
  const res = await authorizedFetch(path);
  return res.blob();
}

async function authorizedFetch(path: string, options: RequestInit = {}, retried = false): Promise<Response> {
  const token = getToken();
  const isFormData = options.body instanceof FormData;

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (res.status === 401 && !retried) {
    // 다른 탭이 그 사이 이미 새 토큰을 받아 뒀으면 그걸 쓰고, 아니면 직접 새로 받는다.
    const current = getToken();
    const fresh = current && current !== token ? current : await refreshAccessToken();
    if (fresh) return authorizedFetch(path, options, true);
  }

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

  return res;
}
