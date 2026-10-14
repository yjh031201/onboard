// 카드 입력 폼(CardForms)과 카드 상세 창이 같이 쓰는 도우미 — 컴포넌트 파일과 나눠 둬야 Fast Refresh가 잘 된다.

import { useLayoutEffect, useRef } from "react";

/**
 * field-sizing:content를 지원하지 않는 브라우저(Firefox·Safari)에서도 입력칸이 내용 높이만큼 자라게 한다.
 * 이게 없으면 긴 제목이 한 줄짜리 칸 안에서 스크롤되어 앞부분이 안 보인다.
 */
export function useAutoGrow(value: string) {
  const ref = useRef<HTMLTextAreaElement>(null);
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = "auto";
    // scrollHeight에는 테두리가 빠져 있어서 (border-box) 그만큼 더한다.
    el.style.height = `${el.scrollHeight + el.offsetHeight - el.clientHeight}px`;
  }, [value]);
  return ref;
}

/** 시간을 비워 두면 그날 끝으로 본다. */
const DEFAULT_DUE_TIME = "23:59";

/** 날짜·시간 입력값 → 서버로 보낼 "YYYY-MM-DDTHH:mm". 날짜가 없으면 마감 없음(null). */
export function toDueAt(date: string, time: string): string | null {
  return date ? `${date}T${time || DEFAULT_DUE_TIME}` : null;
}
