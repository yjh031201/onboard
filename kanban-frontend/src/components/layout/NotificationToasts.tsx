import { useEffect, useState } from "react";
import { subscribeTopic } from "../../lib/realtime";
import type { TimelineEventDto } from "../../types/dashboard";

interface ToastItem extends TimelineEventDto {
  toastId: number;
}

let toastSeq = 0;
const TOAST_DURATION_MS = 4000;

/** 서버가 알림 대상(notified: true)으로 표시한 타임라인 이벤트를 실시간 토스트로 띄운다. */
export default function NotificationToasts() {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    return subscribeTopic<TimelineEventDto>("/topic/timeline", (event) => {
      if (!event.notified) return;

      const toastId = ++toastSeq;
      setToasts((prev) => [...prev, { ...event, toastId }]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((toast) => toast.toastId !== toastId));
      }, TOAST_DURATION_MS);
    });
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed right-6 top-6 z-50 flex w-[320px] flex-col gap-2.5">
      {toasts.map((toast) => (
        <div
          key={toast.toastId}
          className="flex items-start gap-2.5 rounded-xl border border-[#f0f0f2] bg-white p-3.5 shadow-[0px_8px_24px_0px_rgba(0,0,0,0.12)]"
        >
          <span className="mt-0.5 shrink-0 text-[14px]">🔔</span>
          <p className="flex-1 break-keep text-[13px] text-[#111827]">{toast.message}</p>
        </div>
      ))}
    </div>
  );
}
