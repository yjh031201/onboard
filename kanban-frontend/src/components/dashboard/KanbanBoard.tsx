import { useKanbanBoard } from "../../hooks/useKanbanBoard";
import { COLUMN_DEFS } from "../../lib/boardColumns";

export default function KanbanBoard() {
  const { tasks, loading } = useKanbanBoard();

  return (
    <div className="flex h-full w-full flex-1 flex-col gap-3.5">
      <p className="text-[18px] font-bold text-[#111827]">칸반 보드</p>
      <div className="flex h-full w-full flex-1 gap-5">
        {COLUMN_DEFS.map((column) => {
          const columnTasks = tasks.filter((task) => task.status === column.id);
          return (
            <div
              key={column.id}
              className="flex h-full flex-1 flex-col gap-2.5 rounded-xl border border-[#ededef] bg-[#fafafa] px-3.5 py-4"
            >
              <div className="flex w-full items-center justify-between">
                <p className="text-[11.5px] font-medium tracking-[0.46px] text-[#111827]">
                  {column.title}
                </p>
                <p className="text-[12px] text-[#6b7280]">{columnTasks.length}</p>
              </div>
              {loading && (
                <p className="text-[12px] text-[#9ca3af]">불러오는 중...</p>
              )}
              {columnTasks.map((task) => (
                <div
                  key={task.id}
                  className="flex h-[52px] w-full items-center gap-2.5 rounded-lg border border-[#f0f0f2] bg-white p-3"
                >
                  <span className={`size-2 shrink-0 rounded-sm ${column.tagColor}`} />
                  <p className="flex-1 text-[13px] break-keep text-[#111827]">{task.title}</p>
                </div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}
