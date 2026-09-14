import { useState } from "react";
import PageShell from "../components/layout/PageShell";
import CalendarWidget from "../components/dashboard/CalendarWidget";
import KanbanBoard from "../components/dashboard/KanbanBoard";
import ProgressCards from "../components/dashboard/ProgressCards";
import TeamPresenceWidget from "../components/dashboard/TeamPresenceWidget";
import ScheduleModal from "../components/dashboard/ScheduleModal";
import type { ScheduleDate } from "../types/dashboard";

export default function Dashboard() {
  const [modalOpen, setModalOpen] = useState(false);
  const [modalDate, setModalDate] = useState<ScheduleDate | undefined>();

  const handleAddSchedule = (date?: ScheduleDate) => {
    setModalDate(date);
    setModalOpen(true);
  };

  return (
    <PageShell title="대시보드" subtitle="프로젝트 진행 현황을 한눈에 확인하세요">
      <ProgressCards />

      <div className="flex w-full flex-1 gap-6">
        <div className="flex h-full w-[340px] shrink-0 flex-col gap-5">
          <CalendarWidget onAddSchedule={handleAddSchedule} />
          <TeamPresenceWidget />
        </div>
        <KanbanBoard />
      </div>

      {modalOpen && (
        <ScheduleModal initialDate={modalDate} onClose={() => setModalOpen(false)} />
      )}
    </PageShell>
  );
}
