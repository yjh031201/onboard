// 설정 페이지의 알림 on/off 토글 — 사용자별 브라우저에만 저장되는 개인 설정이라
// 서버 없이 localStorage로 관리한다. NotificationToasts가 이 값을 읽어서
// 꺼진 알림은 실제로 뜨지 않도록 걸러낸다.
//
// 저장 키에 사용자 id와 프로젝트 id를 붙여서, 같은 브라우저에서 계정을 바꾸거나
// 다른 프로젝트로 가도 설정이 섞이지 않게 한다 ("이 프로젝트에서 받을 알림").
// 예전에 키 하나(LEGACY_KEY)로 저장했던 값은, 아직 따로 저장한 적 없는 프로젝트의 시작값으로 쓴다.

import { getStoredUser } from "./auth";

const STORAGE_PREFIX = "kanban_notification_settings";
const LEGACY_KEY = STORAGE_PREFIX;

export interface NotificationSettings {
  cardMoveNotif: boolean;
  cardCreateNotif: boolean;
  labelChangeNotif: boolean;
  dueDateNotif: boolean;
  commentNotif: boolean;
}

const DEFAULT_SETTINGS: NotificationSettings = {
  cardMoveNotif: true,
  cardCreateNotif: true,
  labelChangeNotif: true,
  dueDateNotif: false,
  commentNotif: true,
};

function storageKey(projectId: number): string {
  return `${STORAGE_PREFIX}:${getStoredUser()?.id ?? 0}:${projectId}`;
}

function read(key: string): Partial<NotificationSettings> | null {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as Partial<NotificationSettings>) : null;
  } catch {
    return null;
  }
}

export function getNotificationSettings(projectId: number): NotificationSettings {
  const saved = read(storageKey(projectId)) ?? read(LEGACY_KEY);
  return { ...DEFAULT_SETTINGS, ...saved };
}

export function setNotificationSetting(
  projectId: number,
  key: keyof NotificationSettings,
  value: boolean,
): NotificationSettings {
  const next = { ...getNotificationSettings(projectId), [key]: value };
  try {
    localStorage.setItem(storageKey(projectId), JSON.stringify(next));
  } catch {
    // localStorage를 못 쓰는 환경(프라이빗 모드 등)이면 저장되지 않는다.
  }
  return next;
}
