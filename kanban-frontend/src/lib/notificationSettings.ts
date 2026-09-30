// 설정 페이지의 알림 on/off 토글 — 사용자별 브라우저에만 저장되는 개인 설정이라
// 서버 없이 localStorage로 관리한다. NotificationToasts가 이 값을 읽어서
// 꺼진 알림은 실제로 뜨지 않도록 걸러낸다.

const STORAGE_KEY = "kanban_notification_settings";

export interface NotificationSettings {
  cardMoveNotif: boolean;
  cardCreateNotif: boolean;
  labelChangeNotif: boolean;
  dueDateNotif: boolean;
}

const DEFAULT_SETTINGS: NotificationSettings = {
  cardMoveNotif: true,
  cardCreateNotif: true,
  labelChangeNotif: true,
  dueDateNotif: false,
};

export function getNotificationSettings(): NotificationSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    return { ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<NotificationSettings>) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function setNotificationSetting(key: keyof NotificationSettings, value: boolean): NotificationSettings {
  const next = { ...getNotificationSettings(), [key]: value };
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // localStorage를 못 쓰는 환경(프라이빗 모드 등)이면 이번 세션에서만 적용된다.
  }
  return next;
}
