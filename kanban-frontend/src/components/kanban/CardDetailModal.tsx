// 카드를 눌렀을 때 뜨는 상세 창 — 칸반 페이지와 대시보드 칸반 위젯이 같이 쓴다.
// task는 부모가 최신 board 스냅샷에서 찾아 넘기므로, 다른 사람이 카드를 바꾸면 창 안 내용도 바로 바뀐다.
// 창 안에서 바로 고친 내용도 화면 반영은 board 스냅샷으로만 한다 (칸반의 다른 수정과 같은 경로).

import { useEffect, type ReactNode } from "react";
import { createPortal } from "react-dom";
import {
  MAX_CARD_LABELS,
  alertCardError,
  canManageCard,
  changeCardLabels,
  confirmAndDeleteCard,
  describeDue,
  toggleLabelId,
  updateCard,
} from "../../lib/board";
import type { ColumnDef } from "../../lib/boardColumns";
import type { LabelDef } from "../../lib/labels";
import type { ProjectMemberDto } from "../../lib/projectMembers";
import type { CardStatus, TaskCard } from "../../types/dashboard";
import {
  AssigneeSelect,
  ColumnPicker,
  DueEditor,
  InlineDescriptionEditor,
  InlineTitleEditor,
  LabelPicker,
} from "./CardForms";

interface CardDetailModalProps {
  projectId: number;
  task: TaskCard;
  columns: ColumnDef[];
  labels: LabelDef[];
  members: ProjectMemberDto[];
  onMove: (status: CardStatus) => void;
  onClose: () => void;
}

/** 서버로 보낼 카드 내용 중 바꿀 것만. 나머지는 지금 카드 값 그대로 보낸다. */
interface CardPatch {
  title?: string;
  description?: string;
  dueAt?: string | null;
  assigneeId?: number | null;
}

/** "2026-10-14T14:30:00" → "2026. 10. 14. 14:30" */
function formatDateTime(value: string): string {
  const date = new Date(value);
  return `${date.getFullYear()}. ${date.getMonth() + 1}. ${date.getDate()}. ${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes(),
  ).padStart(2, "0")}`;
}

function DetailRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex w-full items-start gap-3">
      <p className="w-[52px] shrink-0 pt-1 text-[12px] text-[#6b7280]">{label}</p>
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-1.5 pt-0.5 text-[13px] text-[#111827]">{children}</div>
    </div>
  );
}

export default function CardDetailModal({
  projectId,
  task,
  columns,
  labels,
  members,
  onMove,
  onClose,
}: CardDetailModalProps) {
  const column = columns.find((c) => c.id === task.status);
  // 제목·설명·마감·담당자는 작성자 또는 관리자만, 라벨·상태는 멤버 누구나 바꿀 수 있다 (서버와 같은 기준).
  const canManage = canManageCard(task);
  const due = task.dueAt ? describeDue(task.dueAt) : null;

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const save = (patch: CardPatch) => {
    updateCard(
      projectId,
      task.id,
      patch.title ?? task.title,
      patch.description ?? task.description ?? "",
      "dueAt" in patch ? (patch.dueAt ?? null) : task.dueAt,
      "assigneeId" in patch ? (patch.assigneeId ?? null) : task.assigneeId,
    ).catch((err) => alertCardError(err, "카드를 수정하지 못했어요."));
  };

  const toggleLabel = (labelId: string) => {
    const next = toggleLabelId(task.labelIds, labelId);
    if (next === task.labelIds) return; // 이미 최대 개수
    changeCardLabels(projectId, task.id, next, task.customLabel).catch((err) =>
      alertCardError(err, "라벨을 바꾸지 못했어요."),
    );
  };

  const changeCustomLabel = (text: string) => {
    changeCardLabels(projectId, task.id, task.labelIds, text || null).catch((err) =>
      alertCardError(err, "라벨을 바꾸지 못했어요."),
    );
  };

  // body에 띄워서, 카드 목록의 스크롤·드래그 영역 안에 갇히지 않게 한다.
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="card-detail-title"
        className="relative flex max-h-[85vh] w-[520px] max-w-[90vw] flex-col gap-[18px] overflow-y-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]"
      >
        <div className="flex w-full items-start justify-between gap-3">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            {column && (
              <span className="flex items-center gap-1.5 text-[12px] text-[#6b7280]">
                <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: column.color }} />
                {column.name}
              </span>
            )}
            {canManage ? (
              <InlineTitleEditor value={task.title} onSave={(title) => save({ title })} />
            ) : (
              <p
                id="card-detail-title"
                className="break-keep text-[18px] font-bold text-[#111827] [overflow-wrap:anywhere]"
              >
                {task.title}
              </p>
            )}
          </div>
          <button
            type="button"
            aria-label="닫기"
            onClick={onClose}
            className="shrink-0 text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <div className="flex w-full flex-col gap-3">
          <DetailRow label="담당자">
            {canManage ? (
              <AssigneeSelect
                id={`detail-assignee-${task.id}`}
                bare
                members={members}
                value={task.assigneeId}
                currentName={task.assigneeName}
                onChange={(assigneeId) => save({ assigneeId })}
              />
            ) : (
              (task.assigneeName ?? <span className="text-[#9ca3af]">담당자 없음</span>)
            )}
          </DetailRow>
          <DetailRow label="마감">
            {canManage ? (
              <DueEditor id={`detail-due-${task.id}`} dueAt={task.dueAt} onSave={(dueAt) => save({ dueAt })} />
            ) : due ? (
              <span className={due.overdue ? "font-medium text-[#ef4444]" : ""}>
                {due.text}
                {due.overdue && " (지남)"}
              </span>
            ) : (
              <span className="text-[#9ca3af]">마감 없음</span>
            )}
          </DetailRow>
          <DetailRow label="라벨">
            <div className="flex w-full flex-col gap-1">
              <LabelPicker
                labels={labels}
                selectedIds={task.labelIds}
                onToggle={toggleLabel}
                customLabel={task.customLabel ?? ""}
                onCustomLabelChange={changeCustomLabel}
              />
              <p className="text-[11px] text-[#9ca3af]">최대 {MAX_CARD_LABELS}개 · 다시 누르면 빠져요</p>
            </div>
          </DetailRow>
        </div>

        <div className="flex w-full flex-col gap-1.5">
          <p className="text-[13px] font-medium text-[#111827]">설명</p>
          {canManage ? (
            <InlineDescriptionEditor
              value={task.description ?? ""}
              onSave={(description) => save({ description })}
            />
          ) : task.description ? (
            <p className="break-keep whitespace-pre-wrap text-[13px] text-[#374151] [overflow-wrap:anywhere]">
              {task.description}
            </p>
          ) : (
            <p className="text-[13px] text-[#9ca3af]">설명이 없어요</p>
          )}
        </div>

        <div className="flex w-full items-start gap-2 border-t border-[#f0f0f2] pt-4">
          <ColumnPicker columns={columns} currentId={task.status} onPick={onMove} />
        </div>

        <div className="flex w-full items-center justify-between gap-3">
          <p className="text-[11.5px] text-[#9ca3af]">
            {task.createdByName}님이 {formatDateTime(task.createdAt)}에 만들었어요
          </p>
          {canManage && (
            <button
              type="button"
              // 삭제되면 board 스냅샷에서 카드가 사라지고, 부모가 창을 닫는다.
              onClick={() => confirmAndDeleteCard(projectId, task)}
              className="shrink-0 rounded-md px-2.5 py-1 text-[12px] text-[#9ca3af] hover:bg-[#fee2e2] hover:text-[#ef4444]"
            >
              카드 삭제
            </button>
          )}
        </div>
        {!canManage && (
          <p className="-mt-2 text-[11px] text-[#9ca3af]">제목·설명·마감·담당자는 작성자나 관리자만 고칠 수 있어요</p>
        )}
      </div>
    </div>,
    document.body,
  );
}
