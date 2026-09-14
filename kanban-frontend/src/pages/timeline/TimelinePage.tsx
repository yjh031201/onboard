import PageShell from "../../components/layout/PageShell";

interface TimelineEntry {
  id: string;
  icon: string;
  iconBg: string;
  text: string;
  time: string;
  notified?: boolean;
}

interface TimelineGroup {
  id: string;
  label: string;
  entries: TimelineEntry[];
}

const GROUPS: TimelineGroup[] = [
  {
    id: "today",
    label: "오늘 · 9월 9일",
    entries: [
      {
        id: "1",
        icon: "↔",
        iconBg: "#feeccf",
        text: "김민수님이 '칸반보드 드래그 기능' 카드를 진행 중 → 완료로 이동했습니다",
        time: "오후 3:24",
        notified: true,
      },
      {
        id: "2",
        icon: "💬",
        iconBg: "#daeefe",
        text: "이서연님이 '로그인 API 개발' 카드에 댓글을 남겼습니다",
        time: "오전 9:47",
        notified: true,
      },
      {
        id: "3",
        icon: "+",
        iconBg: "#eeeefe",
        text: "양종호님이 '배포 스크립트 작성' 카드를 할 일에 추가했습니다",
        time: "오전 9:10",
      },
    ],
  },
  {
    id: "yesterday",
    label: "어제 · 9월 8일",
    entries: [
      {
        id: "4",
        icon: "✎",
        iconBg: "#ececee",
        text: "박지훈님이 '와이어프레임 설계' 카드의 마감일을 변경했습니다",
        time: "오후 6:32",
        notified: true,
      },
      {
        id: "5",
        icon: "👤",
        iconBg: "#dcf9ec",
        text: "양종호님이 이서연님을 팀에 초대했습니다",
        time: "오후 2:05",
      },
      {
        id: "6",
        icon: "↔",
        iconBg: "#feeccf",
        text: "김민수님이 'DB 스키마 설계' 카드를 진행 중 → 완료로 이동했습니다",
        time: "오전 11:20",
        notified: true,
      },
    ],
  },
];

export default function TimelinePage() {
  return (
    <PageShell title="타임라인" subtitle="팀의 최근 활동을 시간순으로 확인하세요">
      <div className="flex w-full flex-1 flex-col gap-[30px] overflow-auto">
        {GROUPS.map((group) => (
          <div key={group.id} className="flex w-full flex-col gap-3.5">
            <div className="flex w-full items-center gap-3">
              <p className="text-[13px] font-bold text-[#111827] whitespace-nowrap">{group.label}</p>
              <div className="h-px flex-1 bg-[#f0f0f2]" />
            </div>
            <div className="flex w-full flex-col">
              {group.entries.map((entry, i) => (
                <div key={entry.id} className="flex w-full gap-3.5">
                  <div className="flex w-6 shrink-0 flex-col items-center gap-1.5 self-stretch">
                    <div
                      style={{ backgroundColor: entry.iconBg }}
                      className="flex size-[22px] shrink-0 items-center justify-center rounded-full"
                    >
                      <span className="text-[10px] font-bold text-[#111827]">{entry.icon}</span>
                    </div>
                    {i < group.entries.length - 1 && (
                      <div className="w-0.5 flex-1 bg-[#e7e8eb]" />
                    )}
                  </div>
                  <div className="flex flex-1 flex-col gap-1.5 pb-[18px]">
                    <p className="break-keep text-[13.5px] text-[#111827]">{entry.text}</p>
                    <div className="flex items-center gap-2">
                      <p className="text-[12px] text-[#6b7280]">{entry.time}</p>
                      {entry.notified && (
                        <span className="inline-flex items-center rounded-full bg-[#eeeefe] px-2 py-[3px] text-[11px] font-medium text-[#6366f1]">
                          🔔 알림 전송
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  );
}
