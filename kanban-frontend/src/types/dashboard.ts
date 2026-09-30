export type NavItemId =
  | "dashboard"
  | "kanban"
  | "timeline"
  | "members"
  | "files"
  | "settings";

export interface NavItem {
  id: NavItemId;
  label: string;
  icon: string;
}

export type PresenceStatus = "online" | "offline";

export interface TeamMember {
  id: string;
  name: string;
  initial: string;
  status: PresenceStatus;
}

/** 카드가 들어 있는 컬럼의 id (board_columns.id) — 기본 컬럼은 TODO / IN_PROGRESS / DONE. */
export type CardStatus = string;

export interface TaskCard {
  id: string;
  title: string;
  status: CardStatus;
  labelId: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
}

export interface KanbanColumn {
  id: CardStatus;
  title: string;
  tasks: TaskCard[];
}

export interface TimelineEventDto {
  id: number;
  type: "CARD_CREATED" | "CARD_MOVED";
  message: string;
  actorId: number;
  actorName: string;
  notified: boolean;
  createdAt: string;
}

export interface ProgressCardData {
  id: string;
  label: string;
  count: number;
  percent: number;
  badgeBg: string;
  badgeText: string;
  barColor: string;
}

export interface CalendarDay {
  date: number;
  inCurrentMonth: boolean;
  isToday?: boolean;
  hasEvent?: boolean;
}

export interface ScheduleDate {
  year: number;
  monthIndex: number;
  date: number;
}

export type ScheduleCategory = "회의" | "마감" | "이벤트" | "기타";

