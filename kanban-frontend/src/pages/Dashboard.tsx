import { useState } from "react";
import PageShell from "../components/layout/PageShell";
import CalendarWidget from "../components/dashboard/CalendarWidget";
import KanbanBoard from "../components/dashboard/KanbanBoard";
import ProgressCards from "../components/dashboard/ProgressCards";
import TeamPresenceWidget from "../components/dashboard/TeamPresenceWidget";
import { useBoardColumns } from "../hooks/useBoardColumns";
import { useKanbanBoard } from "../hooks/useKanbanBoard";

// 달력·팀원 현황 패널을 접어 두면 칸반이 넓어진다 — 개인 화면 설정이라 브라우저에만 저장.
const PANEL_STORAGE_KEY = "kanban_dashboard_panel_collapsed";

function readPanelCollapsed(): boolean {
  try {
    return localStorage.getItem(PANEL_STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

function writePanelCollapsed(collapsed: boolean): void {
  try {
    localStorage.setItem(PANEL_STORAGE_KEY, String(collapsed));
  } catch {
    // localStorage를 못 쓰는 환경이면 이번 방문 동안만 유지된다.
  }
}

export default function Dashboard() {
  // 진행 현황 카드·칸반·캘린더가 같은 데이터를 보도록 한 번만 불러와서 나눠 준다.
  const { tasks, loading } = useKanbanBoard();
  const { columns } = useBoardColumns();
  const [panelCollapsed, setPanelCollapsed] = useState(readPanelCollapsed);

  const togglePanel = () => {
    setPanelCollapsed((prev) => {
      writePanelCollapsed(!prev);
      return !prev;
    });
  };

  return (
    <PageShell title="대시보드" subtitle="프로젝트 진행 현황을 한눈에 확인하세요">
      <ProgressCards columns={columns} tasks={tasks} />

      <div className="flex w-full flex-1 gap-6">
        {panelCollapsed ? (
          <button
            type="button"
            onClick={togglePanel}
            aria-label="달력·팀원 현황 펼치기"
            title="달력·팀원 현황 펼치기"
            className="flex w-10 shrink-0 flex-col items-center gap-3 self-start rounded-[14px] border border-[#f0f0f2] bg-white py-4 text-[#6b7280] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)] hover:text-[#6366f1]"
          >
            <span className="text-[13px]">»</span>
            <span className="text-[15px]">📅</span>
            <span className="text-[15px]">👥</span>
          </button>
        ) : (
          <div className="flex h-full w-[340px] shrink-0 flex-col gap-3">
            <button
              type="button"
              onClick={togglePanel}
              className="self-end rounded-md px-2 py-0.5 text-[12px] text-[#6b7280] hover:bg-[#f0f0f2] hover:text-[#6366f1]"
            >
              « 접기
            </button>
            <CalendarWidget cards={tasks} />
            <TeamPresenceWidget />
          </div>
        )}
        <KanbanBoard columns={columns} tasks={tasks} loading={loading} />
      </div>
    </PageShell>
  );
}
