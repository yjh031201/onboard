import { useState, type DragEvent } from "react";
import PageShell from "../../components/layout/PageShell";
import { useKanbanBoard } from "../../hooks/useKanbanBoard";
import { useBoardColumns } from "../../hooks/useBoardColumns";
import {
  CARD_TITLE_CLASS,
  MAX_CARD_LABELS,
  alertCardError,
  canManageCard,
  changeCardLabels,
  confirmAndDeleteCard,
  createCard,
  describeDue,
  moveCard,
  normalizeCardTitle,
  toggleLabelId,
  updateCard,
} from "../../lib/board";
import { findLabels, type LabelDef } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";
import type { CardStatus, TaskCard } from "../../types/dashboard";

export default function KanbanPage() {
  const { tasks, loading } = useKanbanBoard();
  const { columns } = useBoardColumns();
  const labels = useLabels();
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [addingTo, setAddingTo] = useState<CardStatus | null>(null);
  const [newTitle, setNewTitle] = useState("");
  const [newLabelIds, setNewLabelIds] = useState<string[]>([]);
  const [labelEditingId, setLabelEditingId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);

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
    setNewLabelIds([]);
  };

  const submitNewCard = (status: CardStatus) => {
    const title = newTitle.trim();
    const labelIds = newLabelIds;
    cancelNewCard();
    if (!title) return;
    createCard(title, status, labelIds).catch((err) => alertCardError(err, "카드를 추가하지 못했어요."));
  };

  const toggleCardLabel = (task: TaskCard, labelId: string) => {
    const next = toggleLabelId(task.labelIds, labelId);
    if (next === task.labelIds) return; // 이미 최대 개수
    // 이동과 마찬가지로 화면 반영은 board 스냅샷을 통해서만 한다.
    changeCardLabels(task.id, next).catch((err) => alertCardError(err, "라벨을 바꾸지 못했어요."));
  };

  const clearCardLabels = (task: TaskCard) => {
    setLabelEditingId(null);
    changeCardLabels(task.id, []).catch((err) => alertCardError(err, "라벨을 바꾸지 못했어요."));
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
                  if (editingId === task.id) {
                    return <CardEditForm key={task.id} task={task} onDone={() => setEditingId(null)} />;
                  }

                  const cardLabels = findLabels(labels, task.labelIds);
                  const canManage = canManageCard(task);
                  const due = task.dueAt ? describeDue(task.dueAt) : null;
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
                        <span
                          className="size-2 shrink-0 rounded-sm"
                          style={{ backgroundColor: cardLabels[0]?.color ?? column.color }}
                        />
                        <p className={CARD_TITLE_CLASS}>{task.title}</p>
                        {canManage && (
                          <button
                            type="button"
                            title="카드 수정"
                            aria-label={`'${task.title}' 카드 수정`}
                            onClick={() => setEditingId(task.id)}
                            className="flex size-5 shrink-0 items-center justify-center rounded text-[12px] leading-none text-[#9ca3af] opacity-0 group-hover:opacity-100 hover:bg-[#f0f0f2] hover:text-[#6366f1]"
                          >
                            ✎
                          </button>
                        )}
                        {canManage && (
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

                      <div className="flex w-full flex-wrap items-center gap-1.5 pl-[18px]">
                        {cardLabels.map((label) => (
                          <button
                            key={label.id}
                            type="button"
                            title="라벨 변경"
                            onClick={() => setLabelEditingId(labelEditingId === task.id ? null : task.id)}
                            className="rounded px-1.5 py-0.5 text-[11px] font-medium text-white hover:opacity-80"
                            style={{ backgroundColor: label.color }}
                          >
                            {label.name}
                          </button>
                        ))}
                        {cardLabels.length === 0 && (
                          <button
                            type="button"
                            title="라벨 추가"
                            onClick={() => setLabelEditingId(labelEditingId === task.id ? null : task.id)}
                            className={`rounded px-1.5 py-0.5 text-[11px] text-[#9ca3af] hover:bg-[#f0f0f2] ${
                              labelEditingId === task.id ? "" : "opacity-0 group-hover:opacity-100"
                            }`}
                          >
                            + 라벨
                          </button>
                        )}
                        {due && (
                          <span
                            className={`ml-auto text-[11px] ${due.overdue ? "font-medium text-[#ef4444]" : "text-[#6b7280]"}`}
                            title={due.overdue ? "마감이 지났어요" : "마감"}
                          >
                            ⏰ {due.text}
                          </span>
                        )}
                      </div>

                      {labelEditingId === task.id && (
                        <div className="flex w-full flex-col gap-1.5 border-t border-[#f0f0f2] pt-2">
                          <LabelPicker
                            labels={labels}
                            selectedIds={task.labelIds}
                            onToggle={(labelId) => toggleCardLabel(task, labelId)}
                          />
                          <div className="flex w-full items-center justify-between">
                            <p className="text-[11px] text-[#9ca3af]">최대 {MAX_CARD_LABELS}개</p>
                            {cardLabels.length > 0 && (
                              <button
                                type="button"
                                onClick={() => clearCardLabels(task)}
                                className="text-[11.5px] text-[#9ca3af] hover:text-[#ef4444]"
                              >
                                라벨 모두 삭제
                              </button>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {addingTo === column.id ? (
                <div className="flex w-full flex-col gap-2 rounded-lg border border-[#e5e7eb] bg-white p-2">
                  <textarea
                    autoFocus
                    rows={1}
                    value={newTitle}
                    onChange={(e) => setNewTitle(normalizeCardTitle(e.target.value))}
                    onBlur={() => submitNewCard(column.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                        e.preventDefault();
                        submitNewCard(column.id);
                      }
                      if (e.key === "Escape") cancelNewCard();
                    }}
                    placeholder="카드 제목을 입력하고 Enter"
                    className={TITLE_INPUT_CLASS}
                  />
                  <div className="flex w-full items-start gap-2">
                    <p className="shrink-0 pt-0.5 text-[11.5px] text-[#6b7280]">라벨</p>
                    <LabelPicker
                      labels={labels}
                      selectedIds={newLabelIds}
                      onToggle={(labelId) => setNewLabelIds((prev) => toggleLabelId(prev, labelId))}
                    />
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

/**
 * 카드 제목 입력칸 — 한 줄 input 대신 textarea라서 긴 제목은 옆으로 밀리지 않고 줄이 늘어난다.
 * field-sizing:content로 내용 높이에 맞춰 자라고, Enter는 줄바꿈 대신 저장이다.
 */
const TITLE_INPUT_CLASS =
  "w-full resize-none rounded-md border border-[#e5e7eb] bg-white px-3 py-2 text-[13px] text-[#111827] [field-sizing:content] [overflow-wrap:anywhere] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none";

/** "2026-10-02T18:00:00" → datetime-local 입력값 "2026-10-02T18:00" */
function toInputValue(dueAt: string | null): string {
  return dueAt ? dueAt.slice(0, 16) : "";
}

/** 카드 제목·마감 수정 — 작성자 또는 관리자만 연다 (서버에서도 검사). */
function CardEditForm({ task, onDone }: { task: TaskCard; onDone: () => void }) {
  const [title, setTitle] = useState(task.title);
  const [dueAt, setDueAt] = useState(toInputValue(task.dueAt));
  const [saving, setSaving] = useState(false);

  const save = () => {
    const trimmed = title.trim();
    if (!trimmed || saving) return;
    setSaving(true);
    updateCard(task.id, trimmed, dueAt || null)
      .then(onDone)
      .catch((err) => {
        setSaving(false);
        alertCardError(err, "카드를 수정하지 못했어요.");
      });
  };

  return (
    <div className="flex w-full flex-col gap-2 rounded-lg border border-[#6366f1] bg-white p-2.5">
      <textarea
        autoFocus
        rows={1}
        value={title}
        onChange={(e) => setTitle(normalizeCardTitle(e.target.value))}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.nativeEvent.isComposing) {
            e.preventDefault();
            save();
          }
          if (e.key === "Escape") onDone();
        }}
        aria-label="카드 제목"
        className={TITLE_INPUT_CLASS}
      />
      <div className="flex w-full items-center gap-2">
        <label htmlFor={`due-${task.id}`} className="shrink-0 text-[11.5px] text-[#6b7280]">
          마감
        </label>
        <input
          id={`due-${task.id}`}
          type="datetime-local"
          value={dueAt}
          onChange={(e) => setDueAt(e.target.value)}
          className="min-w-0 flex-1 rounded-md border border-[#e5e7eb] px-2 py-1 text-[12px] text-[#111827] focus:border-[#6366f1] focus:outline-none"
        />
        {dueAt && (
          <button
            type="button"
            onClick={() => setDueAt("")}
            className="shrink-0 text-[11.5px] text-[#9ca3af] hover:text-[#ef4444]"
          >
            지우기
          </button>
        )}
      </div>
      <div className="flex w-full justify-end gap-1.5">
        <button
          type="button"
          onClick={onDone}
          className="rounded-md px-2.5 py-1 text-[12px] text-[#6b7280] hover:bg-[#f0f0f2]"
        >
          취소
        </button>
        <button
          type="button"
          onClick={save}
          disabled={!title.trim() || saving}
          className="rounded-md bg-[#6366f1] px-2.5 py-1 text-[12px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
        >
          저장
        </button>
      </div>
    </div>
  );
}

interface LabelPickerProps {
  labels: LabelDef[];
  selectedIds: string[];
  /** 선택된 라벨을 다시 누르면 빠지고, 최대 개수를 넘으면 눌리지 않는다. */
  onToggle: (labelId: string) => void;
}

/** 설정 페이지 라벨 버튼 목록 — 새 카드 입력칸 아래와 기존 카드의 라벨 변경에서 같이 쓴다. */
function LabelPicker({ labels, selectedIds, onToggle }: LabelPickerProps) {
  const full = selectedIds.length >= MAX_CARD_LABELS;

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {labels.map((label) => {
        const selected = selectedIds.includes(label.id);
        const disabled = !selected && full;
        return (
          <button
            key={label.id}
            type="button"
            aria-pressed={selected}
            disabled={disabled}
            title={disabled ? `라벨은 최대 ${MAX_CARD_LABELS}개까지 붙일 수 있어요` : undefined}
            // mousedown 기본 동작을 막아 입력창 포커스를 유지한다 — 새 카드 입력칸은 blur되면 카드가 바로 생성되기 때문.
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => onToggle(label.id)}
            // 선택되면 라벨 색으로 채우고, 아니면 테두리와 색 점만 보여준다.
            className={`flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11.5px] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
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
