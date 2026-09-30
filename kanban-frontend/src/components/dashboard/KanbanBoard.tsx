import { CARD_TITLE_CLASS, canManageCard, confirmAndDeleteCard, describeDue } from "../../lib/board";
import type { ColumnDef } from "../../lib/boardColumns";
import { findLabels } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";
import type { TaskCard } from "../../types/dashboard";

interface KanbanBoardProps {
  columns: ColumnDef[];
  tasks: TaskCard[];
  loading: boolean;
}

export default function KanbanBoard({ columns, tasks, loading }: KanbanBoardProps) {
  const labels = useLabels();

  return (
    // min-w-0: flex 자식은 기본으로 내용 너비 밑으로 줄어들지 않아서, 컬럼이 많으면
    // 아래 overflow-x-auto가 동작하지 않고 화면 밖으로 밀려난다.
    <div className="flex h-full w-full min-w-0 flex-1 flex-col gap-3.5">
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
                const cardLabels = findLabels(labels, task.labelIds);
                const due = task.dueAt ? describeDue(task.dueAt) : null;
                return (
                  <div
                    key={task.id}
                    className="flex min-h-[52px] w-full flex-col justify-center gap-1.5 rounded-lg border border-[#f0f0f2] bg-white p-3"
                  >
                    <div className="flex w-full items-center gap-2.5">
                      <span
                        className="size-2 shrink-0 rounded-sm"
                        style={{ backgroundColor: cardLabels[0]?.color ?? column.color }}
                      />
                      <p className={CARD_TITLE_CLASS}>{task.title}</p>
                      {canManageCard(task) && (
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
                    {(cardLabels.length > 0 || due) && (
                      <div className="flex w-full flex-wrap items-center gap-1.5 pl-[18px]">
                        {cardLabels.map((label) => (
                          <span
                            key={label.id}
                            className="rounded px-1.5 py-0.5 text-[11px] font-medium text-white"
                            style={{ backgroundColor: label.color }}
                          >
                            {label.name}
                          </span>
                        ))}
                        {due && (
                          <span
                            className={`ml-auto text-[11px] ${due.overdue ? "font-medium text-[#ef4444]" : "text-[#6b7280]"}`}
                          >
                            ⏰ {due.text}
                          </span>
                        )}
                      </div>
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
