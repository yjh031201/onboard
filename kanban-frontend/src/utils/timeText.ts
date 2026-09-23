// 일정 모달에서 "오후 7시", "7시 반", "19:30" 같은 글자를 <input type="time"> 값(HH:mm)으로 바꾸는 유틸.

// "오후 7시 30분", "7시반", "오전 10 시" — 뒤에 "간"이 붙은 "3시간"은 시각이 아니라 제외
const HOUR_PATTERN = /(오전|오후)?\s*(\d{1,2})\s*시(?!간)(?:\s*(반|(\d{1,2})\s*분))?/;
// "19:30", "오후 7:30"
const COLON_PATTERN = /(오전|오후)?\s*(\d{1,2}):(\d{2})/;

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

/**
 * 오전/오후를 적으면 그대로, 안 적은 "N시"(1~11시)는 오후로 본다. 0시·12시·13~23시는 적힌 그대로.
 * "19:30" 같은 콜론 표기는 24시간제로 그대로 받는다.
 */
function toHour24(hour: number, meridiem: string | undefined, assumePmWhenBare: boolean): number | null {
  if (meridiem === "오전") {
    if (hour > 12) return null;
    return hour === 12 ? 0 : hour;
  }
  if (meridiem === "오후") {
    if (hour > 23) return null;
    return hour < 12 ? hour + 12 : hour;
  }
  if (hour === 24) return 0;
  if (hour > 23) return null;
  return assumePmWhenBare && hour >= 1 && hour <= 11 ? hour + 12 : hour;
}

/** 글 안에서 처음 나오는 시각을 HH:mm으로. 못 찾으면 null. */
export function parseTimeText(text: string): string | null {
  const hourMatch = HOUR_PATTERN.exec(text);
  const colonMatch = COLON_PATTERN.exec(text);
  const useColon = !!colonMatch && (!hourMatch || colonMatch.index < hourMatch.index);

  let hour: number | null;
  let minute: number;
  if (useColon && colonMatch) {
    minute = Number(colonMatch[3]);
    hour = toHour24(Number(colonMatch[2]), colonMatch[1], false);
  } else if (hourMatch) {
    minute = hourMatch[3] === "반" ? 30 : Number(hourMatch[4] ?? 0);
    hour = toHour24(Number(hourMatch[2]), hourMatch[1], true);
  } else {
    return null;
  }

  if (hour === null || minute > 59) return null;
  return `${pad(hour)}:${pad(minute)}`;
}

/** "19:30" → "오후 7시 30분", "00:00" → "오전 12시" */
export function formatTimeText(time: string): string {
  const [h, m] = time.split(":").map(Number);
  const meridiem = h < 12 ? "오전" : "오후";
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return m ? `${meridiem} ${hour12}시 ${m}분` : `${meridiem} ${hour12}시`;
}
