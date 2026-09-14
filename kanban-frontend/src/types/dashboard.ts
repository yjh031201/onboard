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

export type PresenceStatus = "online" | "away" | "offline";

export interface TeamMember {
  id: string;
  name: string;
  initial: string;
  status: PresenceStatus;
}

export interface TaskCard {
  id: string;
  title: string;
  tagColor: string;
}

export interface KanbanColumn {
  id: string;
  title: string;
  tasks: TaskCard[];
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

