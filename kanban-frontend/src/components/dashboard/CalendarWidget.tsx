import { useMemo, useState } from "react";
import { buildMonthGrid, getWeekdayLabels } from "../../utils/calendar";
import type { ScheduleDate } from "../../types/dashboard";

// Mock "today" + event data matching the current design — swap for real
// data / `new Date()` once a backend is wired up.
const MOCK_TODAY = { year: 2026, monthIndex: 8, date: 9 }; // Sept 9, 2026
const MOCK_EVENT_DAYS = [12, 18, 25];

interface CalendarWidgetProps {
  onAddSchedule: (date?: ScheduleDate) => void;
}

export default function CalendarWidget({ onAddSchedule }: CalendarWidgetProps) {
  const [viewYear, setViewYear] = useState(MOCK_TODAY.year);
  const [viewMonth, setViewMonth] = useState(MOCK_TODAY.monthIndex);

  const weeks = useMemo(
    () =>
      buildMonthGrid(viewYear, viewMonth, {
        eventDates: viewMonth === MOCK_TODAY.monthIndex ? MOCK_EVENT_DAYS : [],
        today: MOCK_TODAY,
      }),
    [viewYear, viewMonth],
  );

  const goToMonth = (delta: number) => {
    const next = new Date(viewYear, viewMonth + delta, 1);
    setViewYear(next.getFullYear());
    setViewMonth(next.getMonth());
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
            {week.map((day, dayIdx) => (
              <button
                key={dayIdx}
                type="button"
                disabled={!day.inCurrentMonth}
                onClick={() =>
                  onAddSchedule({ year: viewYear, monthIndex: viewMonth, date: day.date })
                }
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
                    className={`text-[12px] ${
                      day.inCurrentMonth
                        ? "font-medium text-[#111827]"
                        : "font-normal text-[#d1d2d6]"
                    }`}
                  >
                    {day.date}
                  </span>
                )}
                <span
                  className={`size-1 rounded-sm ${
                    day.hasEvent ? "bg-[#6366f1]" : "bg-transparent"
                  }`}
                />
              </button>
            ))}
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => onAddSchedule()}
        className="flex w-full items-center justify-center gap-1.5 rounded-lg bg-[#eeeefe] py-2.5 text-[13px] font-bold text-[#6366f1] hover:bg-[#e4e4fd]"
      >
        <span>+</span>
        <span>일정 추가</span>
      </button>
    </div>
  );
}
