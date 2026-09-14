import type { ProgressCardData } from "../../types/dashboard";

const CARDS: ProgressCardData[] = [
  {
    id: "todo",
    label: "할 일",
    count: 8,
    percent: 20,
    badgeBg: "bg-[#e9ecf0]",
    badgeText: "text-[#94a3b8]",
    barColor: "bg-[#94a3b8]",
  },
  {
    id: "in-progress",
    label: "진행 중",
    count: 5,
    percent: 55,
    badgeBg: "bg-[#fff3e1]",
    badgeText: "text-[#f59e0b]",
    barColor: "bg-[#f59e0b]",
  },
  {
    id: "done",
    label: "완료",
    count: 12,
    percent: 100,
    badgeBg: "bg-[#dcf9ec]",
    badgeText: "text-[#10b981]",
    barColor: "bg-[#10b981]",
  },
];

export default function ProgressCards() {
  return (
    <div className="flex w-full items-start justify-between gap-6">
      {CARDS.map((card) => (
        <div
          key={card.id}
          className="flex h-[160px] w-full flex-col gap-3.5 rounded-[14px] border border-[#f0f0f2] bg-white p-[22px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]"
        >
          <div className="flex w-full items-center justify-between">
            <p className="text-[14px] font-medium text-[#6b7280]">{card.label}</p>
            <div
              className={`flex items-center justify-center rounded-full px-2.5 py-1 ${card.badgeBg}`}
            >
              <p className={`text-[11px] font-bold ${card.badgeText}`}>
                {card.percent}%
              </p>
            </div>
          </div>
          <p className="text-[32px] font-bold text-[#111827]">{card.count}</p>
          <div className="h-2 w-full overflow-hidden rounded-full bg-[#f0f0f2]">
            <div
              className={`h-2 rounded-full ${card.barColor}`}
              style={{ width: `${card.percent}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
