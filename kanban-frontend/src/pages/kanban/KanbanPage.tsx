import { useState, type DragEvent } from "react";
import PageShell from "../../components/layout/PageShell";
import { useKanbanBoard } from "../../hooks/useKanbanBoard";
import { COLUMN_DEFS } from "../../lib/boardColumns";
import { createCard, moveCard } from "../../lib/board";
import type { CardStatus } from "../../types/dashboard";

export default function KanbanPage() {
  const { tasks, loading } = useKanbanBoard();
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [addingTo, setAddingTo] = useState<CardStatus | null>(null);
  const [newTitle, setNewTitle] = useState("");

  const handleDrop = (status: CardStatus, targetCardId: string | null) => {
    if (!draggingId) return;

    const columnTasks = tasks.filter((task) => task.status === status && task.id !== draggingId);
    const rawIndex = targetCardId ? columnTasks.findIndex((task) => task.id === targetCardId) : -1;
    const targetIndex = rawIndex === -1 ? columnTasks.length : rawIndex;

    setDraggingId(null);
    // 실제 반영은 서버 브로드캐스트(/topic/board)를 받아서 이루어진다 — 낙관적 업데이트 없이,
    // 다른 사람이 카드를 옮겼을 때와 동일한 경로로 내 화면도 갱신된다.
    moveCard(draggingId, status, targetIndex).catch(() => {
      // 실패해도 다음 board 스냅샷이 오면 다시 맞춰지므로 별도 롤백 처리는 하지 않는다.
    });
  };

  const submitNewCard = (status: CardStatus) => {
    const title = newTitle.trim();
    setAddingTo(null);
    setNewTitle("");
    if (!title) return;
    createCard(title, status).catch(() => {
      /* 실패 시에도 board 스냅샷과 어긋나지 않도록 별도 로컬 상태는 두지 않는다. */
    });
  };

  return (
    <PageShell title="칸반보드" subtitle="카드를 이동하며 작업 진행 상황을 관리하세요">
      <div className="flex h-full w-full flex-1 gap-5">
        {COLUMN_DEFS.map((column) => {
          const columnTasks = tasks.filter((task) => task.status === column.id);

          return (
            <div
              key={column.id}
              onDragOver={(e: DragEvent) => e.preventDefault()}
              onDrop={() => handleDrop(column.id, null)}
              className="flex h-full flex-1 flex-col gap-3 rounded-xl border border-[#ededef] bg-[#fafafa] px-4 py-4"
            >
              <div className="flex w-full items-center justify-between">
                <p className="text-[12.5px] font-medium tracking-[0.4px] text-[#111827]">
                  {column.title}
                </p>
                <p className="text-[12px] text-[#6b7280]">{columnTasks.length}</p>
              </div>

              <div className="flex flex-1 flex-col gap-2.5 overflow-auto">
                {loading && <p className="text-[12px] text-[#9ca3af]">불러오는 중...</p>}
                {columnTasks.map((task) => (
                  <div
                    key={task.id}
                    draggable
                    onDragStart={() => setDraggingId(task.id)}
                    onDragEnd={() => setDraggingId(null)}
                    onDragOver={(e: DragEvent) => {
                      e.preventDefault();
                      e.stopPropagation();
                    }}
                    onDrop={(e: DragEvent) => {
                      e.stopPropagation();
                      handleDrop(column.id, task.id);
                    }}
                    title={`${task.createdByName}님이 작성`}
                    className={`flex min-h-[52px] w-full cursor-grab items-center gap-2.5 rounded-lg border border-[#f0f0f2] bg-white p-3 transition-opacity active:cursor-grabbing ${
                      draggingId === task.id ? "opacity-40" : ""
                    }`}
                  >
                    <span className={`size-2 shrink-0 rounded-sm ${column.tagColor}`} />
                    <p className="flex-1 break-keep text-[13px] text-[#111827]">{task.title}</p>
                  </div>
                ))}
              </div>

              {addingTo === column.id ? (
                <input
                  autoFocus
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  onBlur={() => submitNewCard(column.id)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") submitNewCard(column.id);
                    if (e.key === "Escape") {
                      setAddingTo(null);
                      setNewTitle("");
                    }
                  }}
                  placeholder="카드 제목을 입력하고 Enter"
                  className="w-full rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-[13px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
                />
              ) : (
                <button
                  type="button"
                  onClick={() => setAddingTo(column.id)}
                  className="flex w-full items-center justify-center gap-1.5 rounded-lg py-2 text-[12.5px] font-medium text-[#6b7280] hover:bg-[#f0f0f2]"
                >
                  <span>+</span>
                  <span>새 작업 추가</span>
                </button>
              )}
            </div>
          );
        })}
      </div>
    </PageShell>
  );
}
