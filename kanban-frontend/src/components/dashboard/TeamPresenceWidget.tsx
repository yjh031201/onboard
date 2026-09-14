import type { PresenceStatus, TeamMember } from "../../types/dashboard";

const MEMBERS: TeamMember[] = [
  { id: "1", name: "양종호", initial: "양", status: "online" },
  { id: "2", name: "김민수", initial: "김", status: "online" },
  { id: "3", name: "이서연", initial: "이", status: "away" },
  { id: "4", name: "박지훈", initial: "박", status: "offline" },
];

const STATUS_LABEL: Record<PresenceStatus, string> = {
  online: "온라인",
  away: "자리비움",
  offline: "오프라인",
};

const STATUS_DOT_COLOR: Record<PresenceStatus, string> = {
  online: "bg-[#16a34a]",
  away: "bg-[#f59e0b]",
  offline: "bg-[#b3b6bc]",
};

export default function TeamPresenceWidget() {
  const onlineCount = MEMBERS.filter((m) => m.status === "online").length;

  return (
    <div className="flex w-full flex-1 flex-col gap-4 rounded-[14px] border border-[#f0f0f2] bg-white p-[22px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]">
      <div className="flex w-full flex-col gap-[3px]">
        <p className="text-[14.5px] font-bold text-[#111827]">팀원 현황</p>
        <p className="text-[12.5px] text-[#6b7280]">
          {MEMBERS.length}명 중 {onlineCount}명 접속 중
        </p>
      </div>

      {MEMBERS.map((member) => (
        <div key={member.id} className="flex w-full items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex size-[30px] items-center justify-center rounded-full bg-[#6366f1]">
              <span className="text-[12px] font-bold text-white">
                {member.initial}
              </span>
            </div>
            <p className="text-[13.5px] font-medium text-[#111827]">{member.name}</p>
          </div>
          <div className="flex items-center gap-1.5">
            <span className={`size-[7px] rounded-sm ${STATUS_DOT_COLOR[member.status]}`} />
            <p className="text-[12px] text-[#6b7280]">{STATUS_LABEL[member.status]}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
