import type { CalendarDay } from "../types/dashboard";

const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

export function getWeekdayLabels(): string[] {
  return WEEKDAY_LABELS;
}

/**
 * Builds a 6-row (or fewer, trimmed) week grid for the given month,
 * including the leading/trailing days from adjacent months so every
 * week row has 7 cells (Sun-Sat).
 */
export function buildMonthGrid(
  year: number,
  monthIndex: number, // 0-11
  options: {
    eventDates?: number[]; // day-of-month numbers within this month that have an event
    today?: { year: number; monthIndex: number; date: number };
  } = {},
): CalendarDay[][] {
  const { eventDates = [], today } = options;

  const firstOfMonth = new Date(year, monthIndex, 1);
  const startWeekday = firstOfMonth.getDay(); // 0 = Sun
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const daysInPrevMonth = new Date(year, monthIndex, 0).getDate();

  const cells: CalendarDay[] = [];

  // Leading days from previous month
  for (let i = startWeekday - 1; i >= 0; i--) {
    cells.push({
      date: daysInPrevMonth - i,
      inCurrentMonth: false,
    });
  }

  // Days in current month
  for (let d = 1; d <= daysInMonth; d++) {
    const isToday =
      !!today &&
      today.year === year &&
      today.monthIndex === monthIndex &&
      today.date === d;
    cells.push({
      date: d,
      inCurrentMonth: true,
      isToday,
      hasEvent: eventDates.includes(d),
    });
  }

  // Trailing days from next month to complete the last week row
  let nextDay = 1;
  while (cells.length % 7 !== 0) {
    cells.push({ date: nextDay, inCurrentMonth: false });
    nextDay++;
  }

  const weeks: CalendarDay[][] = [];
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7));
  }
  return weeks;
}
