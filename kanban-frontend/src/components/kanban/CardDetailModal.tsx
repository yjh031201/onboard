// 카드를 눌렀을 때 뜨는 상세 창 — 칸반 페이지와 대시보드 칸반 위젯이 같이 쓴다.
// task는 부모가 최신 board 스냅샷에서 찾아 넘기므로, 다른 사람이 카드를 바꾸면 창 안 내용도 바로 바뀐다.
// 평소에는 읽기 화면이고, [수정]을 누르면 입력칸으로 바뀌어 [저장]할 때 한꺼번에 서버로 보낸다.
// 저장한 내용도 화면 반영은 board 스냅샷으로만 한다 (칸반의 다른 수정과 같은 경로).

import { useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import {
  MAX_CARD_DESCRIPTION_LENGTH,
  MAX_CARD_LABELS,
  alertCardError,
  canManageCard,
  changeCardLabels,
  confirmAndDeleteCard,
  describeDue,
  normalizeCardTitle,
  toggleLabelId,
  updateCard,
} from "../../lib/board";
import type { ColumnDef } from "../../lib/boardColumns";
import { findCardLabels, type LabelDef } from "../../lib/labels";
import type { ProjectMemberDto } from "../../lib/projectMembers";
import type { CardStatus, TaskCard } from "../../types/dashboard";
import {
  AssigneeSelect,
  ColumnPicker,
  DESCRIPTION_INPUT_CLASS,
  DueInputs,
  LabelPicker,
  TITLE_INPUT_CLASS,
} from "./CardForms";
import { toDueAt, useAutoGrow } from "./cardFormUtils";

interface CardDetailModalProps {
  projectId: number;
  task: TaskCard;
  columns: ColumnDef[];
  labels: LabelDef[];
  members: ProjectMemberDto[];
  onMove: (status: CardStatus) => void;
  onClose: () => void;
}

/** [수정]을 누른 뒤 [저장] 전까지 고치고 있는 값. */
interface CardDraft {
  title: string;
  description: string;
  dueDate: string;
  dueTime: string;
  assigneeId: number | null;
  labelIds: string[];
  customLabel: string;
}

function toDraft(task: TaskCard): CardDraft {
  return {
    title: task.title,
    description: task.description ?? "",
    // "2026-10-02T18:00:00" → 날짜 "2026-10-02", 시간 "18:00"
    dueDate: task.dueAt?.slice(0, 10) ?? "",
    dueTime: task.dueAt?.slice(11, 16) ?? "",
    assigneeId: task.assigneeId,
    labelIds: task.labelIds,
    customLabel: task.customLabel ?? "",
  };
}

/**
 * 고친 값을 서버로 보낼 모양으로 바꾸고, 지금 카드와 달라진 게 있는지 본다.
 * 제목·설명·마감·담당자(updateCard)와 라벨(changeCardLabels)은 API가 따로라서 나눠서 본다.
 */
function diffDraft(task: TaskCard, draft: CardDraft, labels: LabelDef[]) {
  const card = {
    title: draft.title.trim(),
    description: draft.description.trim(),
    dueAt: toDueAt(draft.dueDate, draft.dueTime),
    assigneeId: draft.assigneeId,
  };
  // "기타" 라벨을 뺐으면 직접 적은 글자도 버린다 (서버도 같은 기준).
  const hasEtc = labels.some((label) => label.isEtc && draft.labelIds.includes(label.id));
  const label = { labelIds: draft.labelIds, customLabel: hasEtc ? draft.customLabel.trim() || null : null };

  const cardChanged =
    card.title !== task.title ||
    card.description !== (task.description ?? "") ||
    card.dueAt !== (task.dueAt?.slice(0, 16) ?? null) ||
    card.assigneeId !== task.assigneeId;
  const labelChanged =
    label.labelIds.length !== task.labelIds.length ||
    label.labelIds.some((id, i) => id !== task.labelIds[i]) ||
    label.customLabel !== (hasEtc ? (task.customLabel ?? null) : null);

  return { card, label, cardChanged, labelChanged };
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
  const cardLabels = findCardLabels(labels, task);

  // null이면 읽기 화면, 값이 있으면 수정 중.
  const [draft, setDraft] = useState<CardDraft | null>(null);
  const [saving, setSaving] = useState(false);
  const titleRef = useAutoGrow(draft?.title ?? "");
  const descriptionRef = useAutoGrow(draft?.description ?? "");

  const diff = draft ? diffDraft(task, draft, labels) : null;
  const dirty = diff !== null && (diff.cardChanged || diff.labelChanged);

  const editDraft = (patch: Partial<CardDraft>) => setDraft((prev) => (prev ? { ...prev, ...patch } : prev));

  /** 고치던 게 있으면 버려도 되는지 묻는다. */
  const confirmDiscard = () => !dirty || window.confirm("저장하지 않은 내용이 있어요. 버릴까요?");

  const cancelEdit = () => {
    if (confirmDiscard()) setDraft(null);
  };

  const requestClose = () => {
    if (confirmDiscard()) onClose();
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== "Escape") return;
      // 수정 중이면 창을 닫지 않고 수정만 취소한다.
      if (draft) cancelEdit();
      else onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  });

  const save = async () => {
    if (!diff || !diff.card.title || saving) return;
    if (!dirty) {
      setDraft(null);
      return;
    }
    setSaving(true);
    try {
      // 같은 카드에 대한 요청이라 하나씩 보낸다.
      if (canManage && diff.cardChanged) {
        const { title, description, dueAt, assigneeId } = diff.card;
        await updateCard(projectId, task.id, title, description, dueAt, assigneeId);
      }
      if (diff.labelChanged) {
        await changeCardLabels(projectId, task.id, diff.label.labelIds, diff.label.customLabel);
      }
      setDraft(null);
    } catch (err) {
      alertCardError(err, "카드를 저장하지 못했어요.");
    } finally {
      setSaving(false);
    }
  };

  // body에 띄워서, 카드 목록의 스크롤·드래그 영역 안에 갇히지 않게 한다.
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={requestClose} className="absolute inset-0 bg-black/45" />
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
            {draft && canManage ? (
              <textarea
                ref={titleRef}
                id="card-detail-title"
                autoFocus
                rows={1}
                value={draft.title}
                onChange={(e) => editDraft({ title: normalizeCardTitle(e.target.value) })}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                    e.preventDefault();
                    void save();
                  }
                }}
                aria-label="카드 제목"
                className={`${TITLE_INPUT_CLASS} text-[16px] font-bold`}
              />
            ) : (
              <p
                id="card-detail-title"
                className="break-keep text-[18px] font-bold text-[#111827] [overflow-wrap:anywhere]"
              >
                {task.title}
              </p>
            )}
          </div>
          <div className="flex shrink-0 items-center gap-2">
            {!draft && (
              <button
                type="button"
                onClick={() => setDraft(toDraft(task))}
                className="rounded-md border border-[#e5e7eb] px-2.5 py-1 text-[12px] font-medium text-[#374151] hover:border-[#6366f1] hover:bg-[#eeeefe]"
              >
                수정
              </button>
            )}
            <button
              type="button"
              aria-label="닫기"
              onClick={requestClose}
              className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
            >
              ✕
            </button>
          </div>
        </div>

        <div className="flex w-full flex-col gap-3">
          <DetailRow label="담당자">
            {draft && canManage ? (
              <AssigneeSelect
                id={`detail-assignee-${task.id}`}
                bare
                members={members}
                value={draft.assigneeId}
                currentName={task.assigneeName}
                onChange={(assigneeId) => editDraft({ assigneeId })}
              />
            ) : (
              (task.assigneeName ?? <span className="text-[#9ca3af]">담당자 없음</span>)
            )}
          </DetailRow>
          <DetailRow label="마감">
            {draft && canManage ? (
              <DueInputs
                id={`detail-due-${task.id}`}
                bare
                date={draft.dueDate}
                time={draft.dueTime}
                onChange={(dueDate, dueTime) => editDraft({ dueDate, dueTime })}
              />
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
            {draft ? (
              <div className="flex w-full flex-col gap-1">
                <LabelPicker
                  labels={labels}
                  selectedIds={draft.labelIds}
                  onToggle={(labelId) => editDraft({ labelIds: toggleLabelId(draft.labelIds, labelId) })}
                  customLabel={draft.customLabel}
                  onCustomLabelChange={(customLabel) => editDraft({ customLabel })}
                  liveCustomLabel
                />
                <p className="text-[11px] text-[#9ca3af]">최대 {MAX_CARD_LABELS}개 · 다시 누르면 빠져요</p>
              </div>
            ) : cardLabels.length > 0 ? (
              cardLabels.map((label) => (
                <span
                  key={label.id}
                  className="rounded-full px-2 py-0.5 text-[11.5px] font-medium text-white"
                  style={{ backgroundColor: label.color }}
                >
                  {label.name}
                </span>
              ))
            ) : (
              <span className="text-[#9ca3af]">라벨 없음</span>
            )}
          </DetailRow>
        </div>

        <div className="flex w-full flex-col gap-1.5">
          <p className="text-[13px] font-medium text-[#111827]">설명</p>
          {draft && canManage ? (
            <textarea
              ref={descriptionRef}
              rows={3}
              maxLength={MAX_CARD_DESCRIPTION_LENGTH}
              value={draft.description}
              onChange={(e) => editDraft({ description: e.target.value })}
              placeholder="설명 추가"
              aria-label="카드 설명"
              className={`min-h-[72px] ${DESCRIPTION_INPUT_CLASS} text-[13px]`}
            />
          ) : task.description ? (
            <p className="break-keep whitespace-pre-wrap text-[13px] text-[#374151] [overflow-wrap:anywhere]">
              {task.description}
            </p>
          ) : (
            <p className="text-[13px] text-[#9ca3af]">설명 없음</p>
          )}
        </div>

        {draft ? (
          <div className="flex w-full flex-col gap-2 border-t border-[#f0f0f2] pt-4">
            {!canManage && (
              <p className="text-[11px] text-[#9ca3af]">제목·설명·마감·담당자는 작성자나 관리자만 고칠 수 있어요</p>
            )}
            <div className="flex w-full justify-end gap-1.5">
              <button
                type="button"
                onClick={cancelEdit}
                disabled={saving}
                className="rounded-md px-3 py-1.5 text-[12.5px] text-[#6b7280] hover:bg-[#f0f0f2] disabled:opacity-50"
              >
                취소
              </button>
              <button
                type="button"
                onClick={() => void save()}
                disabled={!draft.title.trim() || saving}
                className="rounded-md bg-[#6366f1] px-3 py-1.5 text-[12.5px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
              >
                {saving ? "저장 중…" : "저장"}
              </button>
            </div>
          </div>
        ) : (
          <>
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
          </>
        )}
      </div>
    </div>,
    document.body,
  );
}
