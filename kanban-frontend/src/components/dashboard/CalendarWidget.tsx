import { useEffect, useMemo, useState } from "react";
import { buildMonthGrid, getWeekdayLabels } from "../../utils/calendar";
import type { ScheduleDate } from "../../types/dashboard";
import { ApiError } from "../../lib/api";
import { getStoredUser } from "../../lib/auth";
import {
  CATEGORY_LABELS,
  SCHEDULE_COLORS,
  listMonthSchedules,
  toIsoDate,
  type Schedule,
} from "../../lib/schedules";
import ScheduleModal from "./ScheduleModal";

/** 날짜 칸 아래에 막대로 보여줄 최대 줄 수 — 넘치는 일정은 "+N"으로 표시 */
const MAX_LANES = 2;

/**
 * 겹치는 일정끼리 서로 다른 줄(lane)에 배치한다. 한 일정은 기간 내내 같은 줄을 쓰므로
 * 막대가 날짜를 넘어 이어져 보인다. 먼저 시작한 일정, 같은 날 시작이면 긴 일정이 윗줄.
 */
function assignLanes(schedules: Schedule[], monthStart: string): Map<number, number> {
  const effectiveStart = (s: Schedule) => (s.startDate < monthStart ? monthStart : s.startDate);
  const sorted = [...schedules].sort(
    (a, b) =>
      effectiveStart(a).localeCompare(effectiveStart(b)) ||
      b.endDate.localeCompare(a.endDate) ||
      a.id - b.id,
  );

  const laneEnds: string[] = []; // 각 줄에 마지막으로 놓인 일정의 종료일
  const lanes = new Map<number, number>();
  for (const s of sorted) {
    let lane = laneEnds.findIndex((end) => end < effectiveStart(s));
    if (lane === -1) lane = laneEnds.length;
    laneEnds[lane] = s.endDate;
    lanes.set(s.id, lane);
  }
  return lanes;
}

function todayDate(): ScheduleDate {
  const now = new Date();
  return { year: now.getFullYear(), monthIndex: now.getMonth(), date: now.getDate() };
}

function scheduleColor(schedule: Schedule): string {
  return schedule.color ?? SCHEDULE_COLORS[0];
}

/** "2026-09-24" → "9/24" */
function formatShortDate(isoDate: string): string {
  return `${Number(isoDate.slice(5, 7))}/${Number(isoDate.slice(8, 10))}`;
}

function formatPeriod(schedule: Schedule): string {
  const start = schedule.startTime?.slice(0, 5);
  const end = schedule.endTime?.slice(0, 5);

  if (schedule.startDate !== schedule.endDate) {
    const from = [formatShortDate(schedule.startDate), start].filter(Boolean).join(" ");
    const to = [formatShortDate(schedule.endDate), end].filter(Boolean).join(" ");
    return `${from} ~ ${to}`;
  }
  if (start && end) return `${start} - ${end}`;
  if (start) return start;
  if (end) return `~ ${end}`;
  return "하루 종일";
}

function sortByStart(a: Schedule, b: Schedule): number {
  return (
    a.startDate.localeCompare(b.startDate) ||
    (a.startTime ?? "").localeCompare(b.startTime ?? "") ||
    a.id - b.id
  );
}

type ModalState = { mode: "add" } | { mode: "edit"; schedule: Schedule } | null;

export default function CalendarWidget() {
  const [today] = useState(todayDate);
  const currentUser = getStoredUser();

  const [viewYear, setViewYear] = useState(today.year);
  const [viewMonth, setViewMonth] = useState(today.monthIndex);
  const [selectedDay, setSelectedDay] = useState(today.date);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ModalState>(null);

  useEffect(() => {
    let cancelled = false;
    listMonthSchedules(viewYear, viewMonth)
      .then((data) => {
        if (cancelled) return;
        setSchedules(data);
        setError(null);
      })
      .catch((err) => {
        if (cancelled) return;
        setSchedules([]);
        setError(err instanceof ApiError ? err.message : "일정을 불러오지 못했어요.");
      });
    return () => {
      cancelled = true;
    };
  }, [viewYear, viewMonth]);

  const weeks = useMemo(() => buildMonthGrid(viewYear, viewMonth, { today }), [viewYear, viewMonth, today]);

  const monthStart = toIsoDate(viewYear, viewMonth, 1);
  const monthEnd = toIsoDate(viewYear, viewMonth, new Date(viewYear, viewMonth + 1, 0).getDate());

  const lanes = useMemo(() => assignLanes(schedules, monthStart), [schedules, monthStart]);

  /** 해당 날짜가 기간 안에 들어가는 일정들 (시작일 순) */
  const schedulesOn = (isoDate: string) =>
    schedules.filter((s) => s.startDate <= isoDate && isoDate <= s.endDate);

  const selectedIso = toIsoDate(viewYear, viewMonth, selectedDay);
  const selectedSchedules = schedulesOn(selectedIso);
  const selectedDate: ScheduleDate = { year: viewYear, monthIndex: viewMonth, date: selectedDay };

  // 작성자 본인이거나 OWNER/ADMIN이면 수정/삭제 가능 (서버에서도 동일하게 검증됨).
  const canEdit = (schedule: Schedule) =>
    schedule.createdBy === currentUser?.id ||
    currentUser?.role === "OWNER" ||
    currentUser?.role === "ADMIN";

  const goToMonth = (delta: number) => {
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
    const isCurrentMonth =
      next.getFullYear() === today.year && next.getMonth() === today.monthIndex;
    setSelectedDay(isCurrentMonth ? today.date : 1);
  };

  const handleSaved = (saved: Schedule) => {
    setSchedules((prev) => {
      const others = prev.filter((s) => s.id !== saved.id);
      // 기간을 다른 달로 옮겼으면 지금 보고 있는 달 목록에서는 빠진다.
      const inView = saved.startDate <= monthEnd && saved.endDate >= monthStart;
      return (inView ? [...others, saved] : others).sort(sortByStart);
    });
  };

  const handleDeleted = (id: number) => {
    setSchedules((prev) => prev.filter((s) => s.id !== id));
  };

  return (
    <div className="flex w-full flex-col gap-3.5 rounded-[14px] border border-[#f0f0f2] bg-white p-[22px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]">
      <div className="flex w-full items-center justify-between">
        <p className="text-[14.5px] font-bold text-[#111827]">
          {viewYear}년 {viewMonth + 1}월
        </p>
        <div className="flex gap-1.5">
          <button
            type="button"
            aria-label="이전 달"
            onClick={() => goToMonth(-1)}
            className="flex size-6 items-center justify-center rounded-md bg-[#f3f4f6] text-[13px] font-bold text-[#6b7280] hover:bg-[#e9ecf0]"
          >
            ‹
          </button>
          <button
            type="button"
            aria-label="다음 달"
            onClick={() => goToMonth(1)}
            className="flex size-6 items-center justify-center rounded-md bg-[#f3f4f6] text-[13px] font-bold text-[#6b7280] hover:bg-[#e9ecf0]"
          >
            ›
          </button>
        </div>
      </div>

      <div className="flex w-full items-start">
        {getWeekdayLabels().map((label) => (
          <div key={label} className="flex flex-1 items-start justify-center">
            <p className="text-[11px] font-medium text-[#6b7280]">{label}</p>
          </div>
        ))}
      </div>

      <div className="flex w-full flex-col gap-1">
        {weeks.map((week, weekIdx) => (
          <div key={weekIdx} className="flex w-full items-start">
            {week.map((day, dayIdx) => {
              const iso = day.inCurrentMonth ? toIsoDate(viewYear, viewMonth, day.date) : null;
              const daySchedules = iso ? schedulesOn(iso) : [];
              const hiddenCount = daySchedules.filter((s) => (lanes.get(s.id) ?? 0) >= MAX_LANES).length;
              const isSelected = day.inCurrentMonth && day.date === selectedDay;

              return (
                <button
                  key={dayIdx}
                  type="button"
                  disabled={!day.inCurrentMonth}
                  onClick={() => setSelectedDay(day.date)}
                  className={`flex flex-1 flex-col items-center justify-center gap-0.5 py-[3px] ${
                    day.inCurrentMonth ? "cursor-pointer" : "cursor-default"
                  }`}
                >
                  {day.isToday ? (
                    <span className="flex size-[26px] items-center justify-center rounded-full bg-[#6366f1] text-[12px] font-bold text-white">
                      {day.date}
                    </span>
                  ) : (
                    <span
                      className={`flex size-[26px] items-center justify-center rounded-full text-[12px] ${
                        isSelected ? "bg-[#eeeefe] font-bold text-[#6366f1]" : ""
                      } ${
                        day.inCurrentMonth
                          ? "font-medium text-[#111827]"
                          : "font-normal text-[#d1d2d6]"
                      }`}
                    >
                      {day.date}
                    </span>
                  )}
                  <span className="flex w-full flex-col gap-[2px]">
                    {Array.from({ length: MAX_LANES }, (_, lane) => {
                      const s = daySchedules.find((item) => lanes.get(item.id) === lane);
                      if (!s) return <span key={lane} className="h-1" />;
                      // 기간의 처음/끝(또는 주·월 경계)에서만 둥글게, 중간은 옆 칸과 붙여서 이어진 막대로
                      const startsHere = s.startDate === iso || dayIdx === 0 || day.date === 1;
                      const endsHere = s.endDate === iso || dayIdx === 6 || iso === monthEnd;
                      return (
                        <span
                          key={lane}
                          title={s.title}
                          className={`h-1 ${startsHere ? "rounded-l-full" : ""} ${
                            endsHere ? "rounded-r-full" : ""
                          }`}
                          style={{
                            marginLeft: startsHere ? 3 : 0,
                            marginRight: endsHere ? 3 : 0,
                            backgroundColor: scheduleColor(s),
                          }}
                        />
                      );
                    })}
                  </span>
                  {hiddenCount > 0 && (
                    <span className="text-[9px] leading-none font-medium text-[#6b7280]">
                      +{hiddenCount}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        ))}
      </div>

      {error && <p className="text-[12px] text-[#ef4444]">{error}</p>}

      <div className="flex w-full flex-col gap-1.5">
        <p className="text-[12px] font-medium text-[#6b7280]">
          {viewMonth + 1}월 {selectedDay}일 일정
        </p>
        {selectedSchedules.length === 0 ? (
          <p className="text-[12px] text-[#9ca3af]">등록된 일정이 없어요.</p>
        ) : (
          selectedSchedules.map((schedule) => (
            <button
              key={schedule.id}
              type="button"
              onClick={() => setModal({ mode: "edit", schedule })}
              className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-[#f9fafb]"
            >
              <span
                className="size-2 shrink-0 rounded-full"
                style={{ backgroundColor: scheduleColor(schedule) }}
              />
              <span className="min-w-0 flex-1 truncate text-[12.5px] font-medium text-[#111827]">
                {schedule.title}
              </span>
              <span className="shrink-0 text-[11px] text-[#6b7280]">
                {CATEGORY_LABELS[schedule.category]} · {formatPeriod(schedule)}
              </span>
            </button>
          ))
        )}
      </div>

      <button
        type="button"
        onClick={() => setModal({ mode: "add" })}
        className="flex w-full items-center justify-center gap-1.5 rounded-lg bg-[#eeeefe] py-2.5 text-[13px] font-bold text-[#6366f1] hover:bg-[#e4e4fd]"
      >
        <span>+</span>
        <span>일정 추가</span>
      </button>

      {modal && (
        <ScheduleModal
          date={selectedDate}
          schedule={modal.mode === "edit" ? modal.schedule : undefined}
          canEdit={modal.mode === "edit" ? canEdit(modal.schedule) : true}
          onClose={() => setModal(null)}
          onSaved={handleSaved}
          onDeleted={handleDeleted}
        />
      )}
    </div>
  );
}
