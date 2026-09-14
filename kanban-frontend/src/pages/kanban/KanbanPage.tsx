import PageShell from "../../components/layout/PageShell";
import type { KanbanColumn } from "../../types/dashboard";

const COLUMNS: KanbanColumn[] = [
  {
    id: "todo",
    title: "할 일",
    tasks: [
      { id: "t1", title: "요구사항 정의서 작성", tagColor: "bg-[#94a3b8]" },
      { id: "t2", title: "와이어프레임 설계", tagColor: "bg-[#94a3b8]" },
      { id: "t7", title: "배포 스크립트 작성", tagColor: "bg-[#94a3b8]" },
    ],
  },
  {
    id: "in-progress",
    title: "진행 중",
    tasks: [
      { id: "t3", title: "로그인 API 개발", tagColor: "bg-[#f59e0b]" },
      { id: "t4", title: "칸반보드 드래그 기능", tagColor: "bg-[#f59e0b]" },
    ],
  },
  {
    id: "done",
    title: "완료",
    tasks: [
      { id: "t5", title: "DB 스키마 설계", tagColor: "bg-[#10b981]" },
      { id: "t6", title: "프로젝트 킥오프", tagColor: "bg-[#10b981]" },
    ],
  },
];

export default function KanbanPage() {
  return (
    <PageShell title="칸반보드" subtitle="카드를 이동하며 작업 진행 상황을 관리하세요">
      <div className="flex h-full w-full flex-1 gap-5">
        {COLUMNS.map((column) => (
          <div
            key={column.id}
            className="flex h-full flex-1 flex-col gap-3 rounded-xl border border-[#ededef] bg-[#fafafa] px-4 py-4"
          >
            <div className="flex w-full items-center justify-between">
              <p className="text-[12.5px] font-medium tracking-[0.4px] text-[#111827]">
                {column.title}
              </p>
              <p className="text-[12px] text-[#6b7280]">{column.tasks.length}</p>
            </div>
            <div className="flex flex-1 flex-col gap-2.5 overflow-auto">
              {column.tasks.map((task) => (
                <div
                  key={task.id}
                  className="flex min-h-[52px] w-full items-center gap-2.5 rounded-lg border border-[#f0f0f2] bg-white p-3"
                >
                  <span className={`size-2 shrink-0 rounded-sm ${task.tagColor}`} />
                  <p className="flex-1 break-keep text-[13px] text-[#111827]">{task.title}</p>
                </div>
              ))}
            </div>
            <button
              type="button"
              className="flex w-full items-center justify-center gap-1.5 rounded-lg py-2 text-[12.5px] font-medium text-[#6b7280] hover:bg-[#f0f0f2]"
            >
              <span>+</span>
              <span>새 작업 추가</span>
            </button>
          </div>
        ))}
      </div>
    </PageShell>
  );
}
