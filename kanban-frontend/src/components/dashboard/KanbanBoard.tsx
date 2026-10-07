import { useState, type DragEvent, type MouseEvent } from "react";
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
  toggleLabelId,
} from "../../lib/board";
import { getStoredUser } from "../../lib/auth";
import type { ColumnDef } from "../../lib/boardColumns";
import { EMPTY_CARD_FILTER, isFilterActive, matchesFilter } from "../../lib/cardFilter";
import { findLabels } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";
import type { CardStatus, TaskCard } from "../../types/dashboard";
import CardFilterBar from "../kanban/CardFilterBar";
import { CardDescription, CardEditForm, ColumnPicker, LabelPicker, NewCardForm } from "../kanban/CardForms";

interface KanbanBoardProps {
  projectId: number;
  columns: ColumnDef[];
  tasks: TaskCard[];
  loading: boolean;
}

export default function KanbanBoard({ projectId, columns, tasks, loading }: KanbanBoardProps) {
  const labels = useLabels(projectId);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [addingTo, setAddingTo] = useState<CardStatus | null>(null);
  const [labelEditingId, setLabelEditingId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [movingId, setMovingId] = useState<string | null>(null);
  const [filter, setFilter] = useState(EMPTY_CARD_FILTER);

  // 설정에서 지워진 라벨이 필터에 남아 있으면, 버튼은 사라졌는데 카드만 계속 걸러지므로 뺀다.
  const activeFilter = { ...filter, labelIds: filter.labelIds.filter((id) => labels.some((label) => label.id === id)) };
  const filtering = isFilterActive(activeFilter);
  const myId = getStoredUser()?.id;
  // 화면에 보여 줄 카드. 드래그·이동 위치 계산은 걸러지지 않은 tasks 기준으로 한다 (서버의 순서와 맞아야 한다).
  const visibleTasks = filtering ? tasks.filter((task) => matchesFilter(task, activeFilter, myId)) : tasks;

  const handleDrop = (status: CardStatus, targetCardId: string | null) => {
    if (!draggingId) return;

    const columnTasks = tasks.filter((task) => task.status === status && task.id !== draggingId);
    const rawIndex = targetCardId ? columnTasks.findIndex((task) => task.id === targetCardId) : -1;
    const targetIndex = rawIndex === -1 ? columnTasks.length : rawIndex;

    setDraggingId(null);
    // 칸반 페이지와 마찬가지로 낙관적 업데이트 없이, 서버 브로드캐스트(/topic/projects/{id}/board)를 받아 화면이 갱신된다.
    // 화면은 바뀐 적이 없으니 롤백은 필요 없고, 왜 안 옮겨졌는지(보관된 프로젝트 등)만 알려 준다.
    moveCard(projectId, draggingId, status, targetIndex).catch((err) => alertCardError(err, "카드를 옮기지 못했어요."));
  };

  const submitNewCard = (status: CardStatus, title: string, labelIds: string[], dueAt: string | null) => {
    setAddingTo(null);
    createCard(projectId, title, status, labelIds, dueAt).catch((err) => alertCardError(err, "카드를 추가하지 못했어요."));
  };

  const toggleLabelEditing = (task: TaskCard) => {
    setLabelEditingId(labelEditingId === task.id ? null : task.id);
  };

  const toggleCardLabel = (task: TaskCard, labelId: string) => {
    const next = toggleLabelId(task.labelIds, labelId);
    if (next === task.labelIds) return; // 이미 최대 개수
    changeCardLabels(projectId, task.id, next).catch((err) => alertCardError(err, "라벨을 바꾸지 못했어요."));
  };

  // 카드를 누르면 그 카드 안에 "이동할 컬럼" 목록이 열린다 — 드래그하지 않고도 눌러서 옮길 수 있다.
  const toggleMoving = (e: MouseEvent<HTMLDivElement>, task: TaskCard) => {
    // 카드 안의 버튼(라벨·수정·삭제 등)을 누른 것은 카드를 고른 것으로 치지 않는다.
    if ((e.target as HTMLElement).closest("button")) return;
    setMovingId(movingId === task.id ? null : task.id);
  };

  const moveCardToColumn = (task: TaskCard, status: CardStatus) => {
    setMovingId(null);
    // 옮겨 간 컬럼의 맨 아래에 붙인다. 화면 반영은 드래그와 마찬가지로 board 스냅샷으로 한다.
    const position = tasks.filter((other) => other.status === status).length;
    moveCard(projectId, task.id, status, position).catch((err) => alertCardError(err, "카드를 옮기지 못했어요."));
  };

  const clearCardLabels = (task: TaskCard) => {
    setLabelEditingId(null);
    changeCardLabels(projectId, task.id, []).catch((err) => alertCardError(err, "라벨을 바꾸지 못했어요."));
  };

  return (
    // min-w-0: flex 자식은 기본으로 내용 너비 밑으로 줄어들지 않아서, 컬럼이 많으면
    // 아래 overflow-x-auto가 동작하지 않고 화면 밖으로 밀려난다.
    <div className="flex h-full w-full min-w-0 flex-1 flex-col gap-3.5">
      <p className="text-[18px] font-bold text-[#111827]">칸반 보드</p>
      <CardFilterBar
        labels={labels}
        filter={activeFilter}
        onChange={setFilter}
        shownCount={visibleTasks.length}
        totalCount={tasks.length}
      />
      <div className="flex h-full w-full flex-1 gap-5 overflow-x-auto">
        {columns.map((column) => {
          const columnTasks = visibleTasks.filter((task) => task.status === column.id);
          const columnTotal = tasks.filter((task) => task.status === column.id).length;
          return (
            <div
              key={column.id}
              onDragOver={(e: DragEvent) => e.preventDefault()}
              onDrop={() => handleDrop(column.id, null)}
              className="flex h-full min-w-[200px] flex-1 flex-col gap-2.5 rounded-xl border border-[#ededef] bg-[#fafafa] px-3.5 py-4"
            >
              <div className="flex w-full items-center justify-between">
                <p className="text-[11.5px] font-medium tracking-[0.46px] text-[#111827]">
                  {column.name}
                </p>
                <p className="text-[12px] text-[#6b7280]">
                  {filtering ? `${columnTasks.length}/${columnTotal}` : columnTotal}
                </p>
              </div>
              {/*
                카드가 컬럼 높이를 넘으면 이 안에서만 스크롤된다. scrollbar-gutter:stable로 스크롤바 자리를 미리 잡아 둬서
                스크롤바가 생겼다 없어져도 카드 폭이 그대로다 (폭이 줄면 다른 카드 제목까지 줄바꿈된다).
                -mr-2.5는 그 자리만큼 오른쪽 여백이 넓어 보이지 않게 당긴 것.
              */}
              <div className="-mr-2.5 flex min-h-0 flex-col gap-2.5 overflow-y-auto [scrollbar-gutter:stable] [scrollbar-width:thin] empty:hidden">
                {loading && (
                  <p className="text-[12px] text-[#9ca3af]">불러오는 중...</p>
                )}
                {columnTasks.map((task) => {
                  if (editingId === task.id) {
                    return (
                      <CardEditForm key={task.id} projectId={projectId} task={task} onDone={() => setEditingId(null)} />
                    );
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
                      onClick={(e) => toggleMoving(e, task)}
                      onDragOver={(e: DragEvent) => {
                        e.preventDefault();
                        e.stopPropagation();
                      }}
                      onDrop={(e: DragEvent) => {
                        e.stopPropagation();
                        handleDrop(column.id, task.id);
                      }}
                      title={`${task.createdByName}님이 작성`}
                      // shrink-0: 컬럼이 꽉 차도 카드가 눌려 내용이 잘리지 않게 한다 (대신 목록이 스크롤된다).
                      className={`group flex min-h-[52px] w-full shrink-0 cursor-grab flex-col justify-center gap-1.5 rounded-lg border bg-white p-3 transition-opacity active:cursor-grabbing ${
                        draggingId === task.id ? "opacity-40" : ""
                      } ${movingId === task.id ? "border-[#6366f1]" : "border-[#f0f0f2]"}`}
                    >
                      <div className="flex w-full flex-wrap items-start gap-x-2 gap-y-1.5">
                        <span
                          // mt: 제목이 여러 줄이어도 색 점은 첫 줄 가운데에 맞춘다.
                          className="mt-[6px] size-2 shrink-0 rounded-sm"
                          style={{ backgroundColor: cardLabels[0]?.color ?? column.color }}
                        />
                        <p className={CARD_TITLE_CLASS}>{task.title}</p>
                        {/* 라벨·버튼은 제목 오른쪽에 붙인다. 제목이 길어 자리가 모자라면 오른쪽 정렬로 다음 줄에 내려간다. */}
                        <div className="ml-auto flex max-w-full flex-wrap items-center justify-end gap-1">
                          {cardLabels.map((label) => (
                            <button
                              key={label.id}
                              type="button"
                              title="라벨 변경"
                              onClick={() => toggleLabelEditing(task)}
                              className="max-w-full truncate rounded-full px-2 py-0.5 text-[11px] font-medium text-white hover:opacity-80"
                              style={{ backgroundColor: label.color }}
                            >
                              {label.name}
                            </button>
                          ))}
                          {cardLabels.length === 0 && (
                            <button
                              type="button"
                              title="라벨 추가"
                              onClick={() => toggleLabelEditing(task)}
                              className={`rounded-full px-2 py-0.5 text-[11px] text-[#9ca3af] hover:bg-[#f0f0f2] ${
                                labelEditingId === task.id ? "" : "opacity-0 group-hover:opacity-100"
                              }`}
                            >
                              + 라벨
                            </button>
                          )}
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
                              onClick={() => confirmAndDeleteCard(projectId, task)}
                              className="flex size-5 shrink-0 items-center justify-center rounded text-[15px] leading-none text-[#9ca3af] hover:bg-[#fee2e2] hover:text-[#ef4444]"
                            >
                              ×
                            </button>
                          )}
                        </div>
                      </div>

                      {task.description && (
                        <CardDescription text={task.description} expanded={movingId === task.id} />
                      )}

                      {/* 작성자는 항상 왼쪽에, 마감은 있을 때만 오른쪽에 보여 준다. */}
                      <div className="flex w-full items-center justify-between gap-2 pl-[18px]">
                        <span className="min-w-0 truncate text-[11px] text-[#9ca3af]">{task.createdByName}</span>
                        {due && (
                          <span
                            className={`shrink-0 text-[11px] ${due.overdue ? "font-medium text-[#ef4444]" : "text-[#6b7280]"}`}
                            title={due.overdue ? "마감이 지났어요" : "마감"}
                          >
                            ⏰ {due.text}
                          </span>
                        )}
                      </div>

                      {movingId === task.id && (
                        <div className="flex w-full items-start gap-2 border-t border-[#f0f0f2] pt-2">
                          <ColumnPicker
                            columns={columns}
                            currentId={column.id}
                            onPick={(columnId) => moveCardToColumn(task, columnId)}
                          />
                        </div>
                      )}

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
                <NewCardForm
                  labels={labels}
                  onSubmit={(title, labelIds, dueAt) => submitNewCard(column.id, title, labelIds, dueAt)}
                  onCancel={() => setAddingTo(null)}
                />
              ) : (
                <button
                  type="button"
                  onClick={() => setAddingTo(column.id)}
                  className="flex w-full shrink-0 items-center justify-center gap-1.5 rounded-lg py-2 text-[12.5px] font-medium text-[#6b7280] hover:bg-[#f0f0f2]"
                >
                  <span>+</span>
                  <span>새 작업 추가</span>
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
