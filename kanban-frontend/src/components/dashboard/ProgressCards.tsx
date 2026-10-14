import type { ColumnDef } from "../../lib/boardColumns";
import type { TaskCard } from "../../types/dashboard";

interface ProgressCardsProps {
  columns: ColumnDef[];
  tasks: TaskCard[];
}

// 보드 컬럼마다 카드 하나 — 숫자는 해당 컬럼의 카드 수, 비율은 전체 카드 중 그 컬럼의 비중.
// 컬럼은 설정에서 추가/삭제되므로 개수에 맞춰 줄바꿈되는 그리드로 그린다.
export default function ProgressCards({ columns, tasks }: ProgressCardsProps) {
  const total = tasks.length;

  return (
    <div className="grid w-full grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-6">
      {columns.map((column) => {
        const count = tasks.filter((task) => task.status === column.id).length;
        const percent = total === 0 ? 0 : Math.round((count / total) * 100);

        return (
          <div
            key={column.id}
            className="flex h-[160px] w-full flex-col gap-3.5 rounded-[14px] border border-[#f0f0f2] bg-white p-[22px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]"
          >
            <div className="flex w-full items-center justify-between">
              <p className="text-[14px] font-medium text-[#6b7280]">{column.name}</p>
              <div
                className="flex items-center justify-center rounded-full px-2.5 py-1"
                style={{ backgroundColor: `${column.color}1f` }}
              >
                <p className="text-[11px] font-bold" style={{ color: column.color }}>
                  {percent}%
                </p>
              </div>
            </div>
            <p className="text-[32px] font-bold text-[#111827]">{count}</p>
            <div className="h-2 w-full overflow-hidden rounded-full bg-[#f0f0f2]">
              <div
                className="h-2 rounded-full"
                style={{ width: `${percent}%`, backgroundColor: column.color }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
