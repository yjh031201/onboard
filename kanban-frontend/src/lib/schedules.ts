// 대시보드 캘린더(일정 조회/등록/수정/삭제) 관련 API.

import { apiRequest } from "./api";
import type { ScheduleCategory } from "../types/dashboard";

/** 백엔드 ScheduleCategory enum 값. 화면에는 CATEGORY_LABELS의 한글로 보여준다. */
export type ScheduleCategoryCode = "MEETING" | "DEADLINE" | "EVENT" | "OTHER";

export const CATEGORY_LABELS: Record<ScheduleCategoryCode, ScheduleCategory> = {
  MEETING: "회의",
  DEADLINE: "마감",
  EVENT: "이벤트",
  OTHER: "기타",
};

/** 일정 모달의 색상 태그 선택지. 첫 번째가 기본값. */
export const SCHEDULE_COLORS = ["#6366f1", "#f59e0b", "#10b981", "#ef4444", "#a855f7"];

export interface Schedule {
  id: number;
  title: string;
  content: string | null;
  category: ScheduleCategoryCode;
  scheduleDate: string; // YYYY-MM-DD
  startTime: string | null; // HH:mm:ss
  endTime: string | null;
  color: string | null;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleInput {
  title: string;
  content: string;
  category: ScheduleCategoryCode;
  scheduleDate: string;
  startTime: string | null; // HH:mm
  endTime: string | null;
  color: string;
}

/** monthIndex는 JS Date와 같은 0-11, 서버는 1-12를 받음. */
export function listMonthSchedules(year: number, monthIndex: number): Promise<Schedule[]> {
  return apiRequest<Schedule[]>(`/api/schedules?year=${year}&month=${monthIndex + 1}`);
}

export function createSchedule(input: ScheduleInput): Promise<Schedule> {
  return apiRequest<Schedule>("/api/schedules", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

/** 작성자 본인 또는 OWNER/ADMIN만 성공함 (서버에서 검증). */
export function updateSchedule(id: number, input: ScheduleInput): Promise<Schedule> {
  return apiRequest<Schedule>(`/api/schedules/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteSchedule(id: number): Promise<void> {
  return apiRequest<void>(`/api/schedules/${id}`, { method: "DELETE" });
}
