import PageShell from "../../components/layout/PageShell";
import { useTimelineFeed } from "../../hooks/useTimelineFeed";
import type { TimelineEventDto } from "../../types/dashboard";

const TYPE_ICON: Record<TimelineEventDto["type"], { icon: string; bg: string }> = {
  CARD_MOVED: { icon: "↔", bg: "#feeccf" },
  CARD_CREATED: { icon: "+", bg: "#eeeefe" },
};

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function groupLabel(date: Date): string {
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const monthDay = `${date.getMonth() + 1}월 ${date.getDate()}일`;

  if (isSameDay(date, today)) return `오늘 · ${monthDay}`;
  if (isSameDay(date, yesterday)) return `어제 · ${monthDay}`;
  return monthDay;
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString("ko-KR", { hour: "numeric", minute: "2-digit" });
}

interface TimelineGroup {
  label: string;
  entries: TimelineEventDto[];
}

function groupByDay(events: TimelineEventDto[]): TimelineGroup[] {
  const groups: TimelineGroup[] = [];

  for (const event of events) {
    const label = groupLabel(new Date(event.createdAt));
    const currentGroup = groups.at(-1);
    if (currentGroup?.label === label) {
      currentGroup.entries.push(event);
    } else {
      groups.push({ label, entries: [event] });
    }
  }

  return groups;
}

export default function TimelinePage() {
  const events = useTimelineFeed();
  const groups = groupByDay(events);

  return (
    <PageShell title="타임라인" subtitle="팀의 최근 활동을 시간순으로 확인하세요">
      <div className="flex w-full flex-1 flex-col gap-[30px] overflow-auto">
        {groups.length === 0 && (
          <p className="text-[13px] text-[#9ca3af]">아직 활동 기록이 없어요.</p>
        )}
        {groups.map((group) => (
          <div key={group.label} className="flex w-full flex-col gap-3.5">
            <div className="flex w-full items-center gap-3">
              <p className="text-[13px] font-bold text-[#111827] whitespace-nowrap">{group.label}</p>
              <div className="h-px flex-1 bg-[#f0f0f2]" />
            </div>
            <div className="flex w-full flex-col">
              {group.entries.map((entry, i) => {
                const { icon, bg } = TYPE_ICON[entry.type];
                return (
                  <div key={entry.id} className="flex w-full gap-3.5">
                    <div className="flex w-6 shrink-0 flex-col items-center gap-1.5 self-stretch">
                      <div
                        style={{ backgroundColor: bg }}
                        className="flex size-[22px] shrink-0 items-center justify-center rounded-full"
                      >
                        <span className="text-[10px] font-bold text-[#111827]">{icon}</span>
                      </div>
                      {i < group.entries.length - 1 && (
                        <div className="w-0.5 flex-1 bg-[#e7e8eb]" />
                      )}
                    </div>
                    <div className="flex flex-1 flex-col gap-1.5 pb-[18px]">
                      <p className="break-keep text-[13.5px] text-[#111827]">{entry.message}</p>
                      <div className="flex items-center gap-2">
                        <p className="text-[12px] text-[#6b7280]">{formatTime(new Date(entry.createdAt))}</p>
                        {entry.notified && (
                          <span className="inline-flex items-center rounded-full bg-[#eeeefe] px-2 py-[3px] text-[11px] font-medium text-[#6366f1]">
                            🔔 알림 전송
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  );
}
