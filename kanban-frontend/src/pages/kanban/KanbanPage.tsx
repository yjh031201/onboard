import { useState, type DragEvent } from "react";
import PageShell from "../../components/layout/PageShell";
import { useKanbanBoard } from "../../hooks/useKanbanBoard";
import { useBoardColumns } from "../../hooks/useBoardColumns";
import { canDeleteCard, changeCardLabel, confirmAndDeleteCard, createCard, moveCard } from "../../lib/board";
import { findLabel, type LabelDef } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";
import type { CardStatus } from "../../types/dashboard";

export default function KanbanPage() {
  const { tasks, loading } = useKanbanBoard();
  const { columns } = useBoardColumns();
  const labels = useLabels();
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [addingTo, setAddingTo] = useState<CardStatus | null>(null);
  const [newTitle, setNewTitle] = useState("");
  const [newLabelId, setNewLabelId] = useState<string | null>(null);
  const [labelEditingId, setLabelEditingId] = useState<string | null>(null);

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

  const cancelNewCard = () => {
    setAddingTo(null);
    setNewTitle("");
    setNewLabelId(null);
  };

  const submitNewCard = (status: CardStatus) => {
    const title = newTitle.trim();
    const labelId = newLabelId;
    cancelNewCard();
    if (!title) return;
    createCard(title, status, labelId).catch(() => {
      /* 실패 시에도 board 스냅샷과 어긋나지 않도록 별도 로컬 상태는 두지 않는다. */
    });
  };

  const updateCardLabel = (cardId: string, labelId: string | null) => {
    setLabelEditingId(null);
    changeCardLabel(cardId, labelId).catch(() => {
      /* 이동과 마찬가지로 화면 반영은 board 스냅샷을 통해서만 한다. */
    });
  };

  return (
    <PageShell title="칸반보드" subtitle="카드를 이동하며 작업 진행 상황을 관리하세요">
      <div className="flex h-full w-full flex-1 gap-5 overflow-x-auto">
        {columns.map((column) => {
          const columnTasks = tasks.filter((task) => task.status === column.id);

          return (
            <div
              key={column.id}
              onDragOver={(e: DragEvent) => e.preventDefault()}
              onDrop={() => handleDrop(column.id, null)}
              className="flex h-full min-w-[240px] flex-1 flex-col gap-3 rounded-xl border border-[#ededef] bg-[#fafafa] px-4 py-4"
            >
              <div className="flex w-full items-center justify-between">
                <p className="text-[12.5px] font-medium tracking-[0.4px] text-[#111827]">
                  {column.name}
                </p>
                <p className="text-[12px] text-[#6b7280]">{columnTasks.length}</p>
              </div>

              <div className="flex flex-1 flex-col gap-2.5 overflow-auto">
                {loading && <p className="text-[12px] text-[#9ca3af]">불러오는 중...</p>}
                {columnTasks.map((task) => {
                  const label = findLabel(labels, task.labelId);
                  return (
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
                      className={`group flex min-h-[52px] w-full cursor-grab flex-col justify-center gap-2 rounded-lg border border-[#f0f0f2] bg-white p-3 transition-opacity active:cursor-grabbing ${
                        draggingId === task.id ? "opacity-40" : ""
                      }`}
                    >
                      <div className="flex w-full items-center gap-2.5">
                        {label ? (
                          <span
                            className="size-2 shrink-0 rounded-sm"
                            style={{ backgroundColor: label.color }}
                          />
                        ) : (
                          <span className="size-2 shrink-0 rounded-sm" style={{ backgroundColor: column.color }} />
                        )}
                        <p className="flex-1 break-keep text-[13px] text-[#111827]">{task.title}</p>
                        <button
                          type="button"
                          title="라벨 변경"
                          onClick={() => setLabelEditingId(labelEditingId === task.id ? null : task.id)}
                          className={
                            label
                              ? "shrink-0 rounded px-1.5 py-0.5 text-[11px] font-medium text-white hover:opacity-80"
                              : `shrink-0 rounded px-1.5 py-0.5 text-[11px] text-[#9ca3af] hover:bg-[#f0f0f2] ${
                                  labelEditingId === task.id ? "" : "opacity-0 group-hover:opacity-100"
                                }`
                          }
                          style={label ? { backgroundColor: label.color } : undefined}
                        >
                          {label ? label.name : "+ 라벨"}
                        </button>
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
                      {labelEditingId === task.id && (
                        <div className="flex w-full items-center justify-end gap-2 border-t border-[#f0f0f2] pt-2">
                          <LabelPicker
                            labels={labels}
                            selectedId={task.labelId}
                            onSelect={(labelId) => updateCardLabel(task.id, labelId)}
                          />
                          {label && (
                            <button
                              type="button"
                              onClick={() => updateCardLabel(task.id, null)}
                              className="text-[11.5px] text-[#9ca3af] hover:text-[#ef4444]"
                            >
                              라벨 삭제
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {addingTo === column.id ? (
                <div className="flex w-full flex-col gap-2 rounded-lg border border-[#e5e7eb] bg-white p-2">
                  <input
                    autoFocus
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    onBlur={() => submitNewCard(column.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") submitNewCard(column.id);
                      if (e.key === "Escape") cancelNewCard();
                    }}
                    placeholder="카드 제목을 입력하고 Enter"
                    className="w-full rounded-md border border-[#e5e7eb] bg-white px-3 py-2 text-[13px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
                  />
                  <div className="flex w-full items-center gap-2">
                    <p className="shrink-0 text-[11.5px] text-[#6b7280]">라벨</p>
                    <LabelPicker labels={labels} selectedId={newLabelId} onSelect={setNewLabelId} />
                  </div>
                </div>
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

interface LabelPickerProps {
  labels: LabelDef[];
  selectedId: string | null;
  /** 이미 선택된 색을 다시 누르면 null(라벨 없음)로 넘어온다. */
  onSelect: (labelId: string | null) => void;
}

/** 설정 페이지 라벨 색상 버튼 목록 — 새 카드 입력칸 옆과 기존 카드의 라벨 변경에서 같이 쓴다. */
function LabelPicker({ labels, selectedId, onSelect }: LabelPickerProps) {
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {labels.map((label) => {
        const selected = selectedId === label.id;
        return (
          <button
            key={label.id}
            type="button"
            aria-pressed={selected}
            // mousedown 기본 동작을 막아 입력창 포커스를 유지한다 — 새 카드 입력칸은 blur되면 카드가 바로 생성되기 때문.
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => onSelect(selected ? null : label.id)}
            // 선택되면 라벨 색으로 채우고, 아니면 테두리와 색 점만 보여준다.
            className={`flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11.5px] font-medium transition-colors ${
              selected ? "text-white" : "bg-white text-[#374151] hover:bg-[#f9fafb]"
            }`}
            style={selected ? { backgroundColor: label.color, borderColor: label.color } : { borderColor: "#e5e7eb" }}
          >
            {!selected && <span className="size-2 rounded-full" style={{ backgroundColor: label.color }} />}
            {label.name}
          </button>
        );
      })}
    </div>
  );
}
