// 컬럼의 [+ 새 작업 추가]를 누르면 뜨는 새 카드 창 — 카드 상세 창의 수정 화면과 같은 모양이고, 처음엔 모두 비어 있다.
// 화면 반영은 다른 카드 변경과 마찬가지로 board 스냅샷(/topic/projects/{id}/board)으로만 한다.

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import {
  MAX_CARD_DESCRIPTION_LENGTH,
  MAX_CARD_LABELS,
  alertCardError,
  createCard,
  normalizeCardTitle,
  toggleLabelId,
} from "../../lib/board";
import type { ColumnDef } from "../../lib/boardColumns";
import type { LabelDef } from "../../lib/labels";
import type { ProjectMemberDto } from "../../lib/projectMembers";
import { DetailRow } from "./CardDetailModal";
import { AssigneeSelect, DESCRIPTION_INPUT_CLASS, DueInputs, LabelPicker, TITLE_INPUT_CLASS } from "./CardForms";
import { toDueAt, useAutoGrow } from "./cardFormUtils";

interface NewCardModalProps {
  projectId: number;
  /** 카드를 추가할 컬럼. */
  column: ColumnDef;
  labels: LabelDef[];
  members: ProjectMemberDto[];
  onClose: () => void;
}

export default function NewCardModal({ projectId, column, labels, members, onClose }: NewCardModalProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [dueTime, setDueTime] = useState("");
  const [assigneeId, setAssigneeId] = useState<number | null>(null);
  const [labelIds, setLabelIds] = useState<string[]>([]);
  const [customLabel, setCustomLabel] = useState("");
  const [saving, setSaving] = useState(false);
  const titleRef = useAutoGrow(title);
  const descriptionRef = useAutoGrow(description);

  const dirty =
    title.trim() !== "" ||
    description.trim() !== "" ||
    dueDate !== "" ||
    assigneeId !== null ||
    labelIds.length > 0;

  const requestClose = () => {
    if (saving) return;
    if (!dirty || window.confirm("저장하지 않은 내용이 있어요. 버릴까요?")) onClose();
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") requestClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  });

  const save = async () => {
    const trimmedTitle = title.trim();
    if (!trimmedTitle || saving) return;
    // "기타" 라벨을 고르지 않았으면 직접 적은 글자는 버린다 (서버도 같은 기준).
    const hasEtc = labels.some((label) => label.isEtc && labelIds.includes(label.id));
    const dueAt = toDueAt(dueDate, dueTime);
    const trimmedDescription = description.trim();

    setSaving(true);
    try {
      // 설명까지 한 번에 보낸다 — 요청이 하나라 중간에 실패해서 카드가 반만 만들어지는 일이 없다.
      await createCard(
        projectId,
        trimmedTitle,
        column.id,
        labelIds,
        hasEtc ? customLabel.trim() || null : null,
        dueAt,
        assigneeId,
        trimmedDescription || null,
      );
      onClose();
    } catch (err) {
      alertCardError(err, "카드를 추가하지 못했어요.");
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
        aria-label={`'${column.name}'에 새 작업 추가`}
        className="relative flex max-h-[85vh] w-[520px] max-w-[90vw] flex-col gap-[18px] overflow-y-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]"
      >
        <div className="flex w-full items-start justify-between gap-3">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <span className="flex items-center gap-1.5 text-[12px] text-[#6b7280]">
              <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: column.color }} />
              {column.name}
            </span>
            <textarea
              ref={titleRef}
              autoFocus
              rows={1}
              value={title}
              onChange={(e) => setTitle(normalizeCardTitle(e.target.value))}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.nativeEvent.isComposing) {
                  e.preventDefault();
                  void save();
                }
              }}
              placeholder="카드 제목"
              aria-label="카드 제목"
              className={`${TITLE_INPUT_CLASS} text-[16px] font-bold`}
            />
          </div>
          <button
            type="button"
            aria-label="닫기"
            onClick={requestClose}
            className="shrink-0 text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <div className="flex w-full flex-col gap-3">
          <DetailRow label="담당자">
            <AssigneeSelect
              id={`new-card-assignee-${column.id}`}
              bare
              members={members}
              value={assigneeId}
              onChange={setAssigneeId}
            />
          </DetailRow>
          <DetailRow label="마감">
            <DueInputs
              id={`new-card-due-${column.id}`}
              bare
              date={dueDate}
              time={dueTime}
              onChange={(date, time) => {
                setDueDate(date);
                setDueTime(time);
              }}
            />
          </DetailRow>
          <DetailRow label="라벨">
            <div className="flex w-full flex-col gap-1">
              <LabelPicker
                labels={labels}
                selectedIds={labelIds}
                onToggle={(labelId) => setLabelIds((prev) => toggleLabelId(prev, labelId))}
                customLabel={customLabel}
                onCustomLabelChange={setCustomLabel}
                liveCustomLabel
              />
              <p className="text-[11px] text-[#9ca3af]">최대 {MAX_CARD_LABELS}개 · 다시 누르면 빠져요</p>
            </div>
          </DetailRow>
        </div>

        <div className="flex w-full flex-col gap-1.5">
          <p className="text-[13px] font-medium text-[#111827]">설명</p>
          <textarea
            ref={descriptionRef}
            rows={3}
            maxLength={MAX_CARD_DESCRIPTION_LENGTH}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="설명 추가"
            aria-label="카드 설명"
            className={`min-h-[72px] ${DESCRIPTION_INPUT_CLASS} text-[13px]`}
          />
        </div>

        <div className="flex w-full justify-end gap-1.5 border-t border-[#f0f0f2] pt-4">
          <button
            type="button"
            onClick={requestClose}
            disabled={saving}
            className="rounded-md px-3 py-1.5 text-[12.5px] text-[#6b7280] hover:bg-[#f0f0f2] disabled:opacity-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => void save()}
            disabled={!title.trim() || saving}
            className="rounded-md bg-[#6366f1] px-3 py-1.5 text-[12.5px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
          >
            {saving ? "저장 중…" : "저장"}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
