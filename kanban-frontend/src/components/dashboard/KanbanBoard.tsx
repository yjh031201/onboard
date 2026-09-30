import { useKanbanBoard } from "../../hooks/useKanbanBoard";
import { useBoardColumns } from "../../hooks/useBoardColumns";
import { canDeleteCard, confirmAndDeleteCard } from "../../lib/board";
import { findLabel } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";

export default function KanbanBoard() {
  const { tasks, loading } = useKanbanBoard();
  const { columns } = useBoardColumns();
  const labels = useLabels();

  return (
    <div className="flex h-full w-full flex-1 flex-col gap-3.5">
      <p className="text-[18px] font-bold text-[#111827]">칸반 보드</p>
      <div className="flex h-full w-full flex-1 gap-5 overflow-x-auto">
        {columns.map((column) => {
          const columnTasks = tasks.filter((task) => task.status === column.id);
          return (
            <div
              key={column.id}
              className="flex h-full min-w-[200px] flex-1 flex-col gap-2.5 rounded-xl border border-[#ededef] bg-[#fafafa] px-3.5 py-4"
            >
              <div className="flex w-full items-center justify-between">
                <p className="text-[11.5px] font-medium tracking-[0.46px] text-[#111827]">
                  {column.name}
                </p>
                <p className="text-[12px] text-[#6b7280]">{columnTasks.length}</p>
              </div>
              {loading && (
                <p className="text-[12px] text-[#9ca3af]">불러오는 중...</p>
              )}
              {columnTasks.map((task) => {
                const label = findLabel(labels, task.labelId);
                return (
                  <div
                    key={task.id}
                    className="flex h-[52px] w-full items-center gap-2.5 rounded-lg border border-[#f0f0f2] bg-white p-3"
                  >
                    {label ? (
                      <span className="size-2 shrink-0 rounded-sm" style={{ backgroundColor: label.color }} />
                    ) : (
                      <span className="size-2 shrink-0 rounded-sm" style={{ backgroundColor: column.color }} />
                    )}
                    <p className="flex-1 text-[13px] break-keep text-[#111827]">{task.title}</p>
                    {label && (
                      <span
                        className="shrink-0 rounded px-1.5 py-0.5 text-[11px] font-medium text-white"
                        style={{ backgroundColor: label.color }}
                      >
                        {label.name}
                      </span>
                    )}
                    {canDeleteCard(task) && (
                      <button
                        type="button"
                        title="카드 삭제"
                        aria-label={`'${task.title}' 카드 삭제`}
                        onClick={() => confirmAndDeleteCard(task)}
                        className="flex size-5 shrink-0 items-center justify-center rounded text-[15px] leading-none text-[#9ca3af] hover:bg-[#fee2e2] hover:text-[#ef4444]"
                      >
                        ×
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
