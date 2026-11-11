// WebSocket(STOMP) 실시간 연결 — 서버가 /topic/board, /topic/presence, /topic/timeline로
// 보내는 이벤트를 구독한다. 실제 카드 이동/생성 같은 변경은 여전히 REST(api.ts)로 요청하고,
// 이 연결은 "다른 사람이 방금 한 변경"을 나에게 push 받는 용도로만 쓴다.
//
// 백엔드는 Redis pub-sub을 backplane으로 써서 여러 인스턴스에 붙은 클라이언트에게도
// 동일하게 이벤트를 전파하지만, 프론트에서는 그냥 STOMP 토픽 하나만 구독하면 된다.

import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE_URL, getToken, refreshAccessToken } from "./api";

type Handler<T> = (payload: T) => void;

interface TopicEntry {
  subscription: StompSubscription | null;
  handlers: Set<Handler<unknown>>;
}

const topics = new Map<string, TopicEntry>();
let client: Client | null = null;
/** 이번 로그인에서 한 번이라도 연결됐는지 — 그 뒤의 연결은 "재연결"이다. */
let connectedBefore = false;
const reconnectListeners = new Set<() => void>();

/**
 * 끊겼다가 다시 연결됐을 때 부를 함수를 등록한다 (서버 재시작·네트워크 끊김 등).
 * 끊긴 동안 온 이벤트는 받지 못했으므로, 처음에 REST로 불러온 데이터를 다시 불러오는 데 쓴다.
 */
export function onRealtimeReconnect(listener: () => void): () => void {
  reconnectListeners.add(listener);
  return () => {
    reconnectListeners.delete(listener);
  };
}

function getOrCreateEntry(destination: string): TopicEntry {
  let entry = topics.get(destination);
  if (!entry) {
    entry = { subscription: null, handlers: new Set() };
    topics.set(destination, entry);
  }
  return entry;
}

function subscribeAllPendingTopics(activeClient: Client) {
  for (const [destination, entry] of topics) {
    if (entry.handlers.size > 0 && !entry.subscription) {
      entry.subscription = activeClient.subscribe(destination, (message: IMessage) => {
        const payload = JSON.parse(message.body) as unknown;
        entry.handlers.forEach((handler) => handler(payload));
      });
    }
  }
}

function clearStaleSubscriptions() {
  for (const entry of topics.values()) {
    entry.subscription = null;
  }
}

/** JWT의 만료 시각(exp)이 1분 안이거나 읽을 수 없으면 true. 서명 검증은 서버가 하고, 여기선 미리 갱신할지만 본다. */
function isExpiringSoon(token: string): boolean {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload)) as { exp?: number };
    return !exp || exp * 1000 - Date.now() < 60_000;
  } catch {
    return true;
  }
}

function ensureClient(): Client {
  if (client) return client;

  client = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    reconnectDelay: 3000,
    // 연결·재연결할 때마다 그때의 토큰을 싣는다. 서버 재시작 등으로 다시 붙을 때 토큰이 만료돼 있으면
    // 먼저 새로 받는다 — 안 그러면 만료된 토큰으로 CONNECT가 계속 거절된다.
    beforeConnect: async (stompClient) => {
      let token = getToken();
      if (!token || isExpiringSoon(token)) {
        token = (await refreshAccessToken()) ?? token;
      }
      stompClient.connectHeaders = { Authorization: `Bearer ${token ?? ""}` };
    },
    onConnect: () => {
      subscribeAllPendingTopics(client!);
      if (connectedBefore) reconnectListeners.forEach((listener) => listener());
      connectedBefore = true;
    },
    onWebSocketClose: clearStaleSubscriptions,
    onStompError: (frame) => {
      console.error("STOMP error", frame.headers["message"], frame.body);
    },
  });

  return client;
}

/** 로그인 후 한 번 호출하면 되고, 이미 연결돼 있으면 아무 일도 하지 않는다. */
export function connectRealtime() {
  const activeClient = ensureClient();
  if (!activeClient.active) {
    activeClient.activate();
  }
}

export function disconnectRealtime() {
  client?.deactivate();
  clearStaleSubscriptions();
  connectedBefore = false; // 다음 로그인의 첫 연결은 재연결이 아니다.
}

/**
 * destination(`/topic/board` 등)을 구독한다. 같은 destination을 여러 컴포넌트가
 * 동시에 구독해도 실제 STOMP 구독은 하나만 유지된다. 반환값을 useEffect의 cleanup에서
 * 호출하면 구독이 해제된다.
 */
export function subscribeTopic<T>(destination: string, handler: Handler<T>): () => void {
  const entry = getOrCreateEntry(destination);
  entry.handlers.add(handler as Handler<unknown>);

  if (client?.connected && !entry.subscription) {
    entry.subscription = client.subscribe(destination, (message: IMessage) => {
      const payload = JSON.parse(message.body) as unknown;
      entry.handlers.forEach((h) => h(payload));
    });
  }

  return () => {
    entry.handlers.delete(handler as Handler<unknown>);
    if (entry.handlers.size === 0 && entry.subscription) {
      entry.subscription.unsubscribe();
      entry.subscription = null;
    }
  };
}
