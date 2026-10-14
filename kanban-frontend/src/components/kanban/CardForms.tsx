// 카드 입력·수정 폼 — 칸반 페이지와 대시보드 칸반 위젯이 같이 쓴다.

import { useLayoutEffect, useRef, useState, type FocusEvent, type KeyboardEvent } from "react";
import {
  MAX_CARD_DESCRIPTION_LENGTH,
  MAX_CARD_LABELS,
  alertCardError,
  normalizeCardTitle,
  toggleLabelId,
  updateCard,
} from "../../lib/board";
import type { ColumnDef } from "../../lib/boardColumns";
import { MAX_LABEL_NAME_LENGTH, type LabelDef } from "../../lib/labels";
import type { ProjectMemberDto } from "../../lib/projectMembers";
import type { TaskCard } from "../../types/dashboard";

/**
 * 카드 제목 입력칸 — 한 줄 input 대신 textarea라서 긴 제목은 옆으로 밀리지 않고 줄이 늘어난다.
 * field-sizing:content로 내용 높이에 맞춰 자라고, Enter는 줄바꿈 대신 저장이다.
 */
const TITLE_INPUT_CLASS =
  "w-full resize-none overflow-hidden break-keep rounded-md border border-[#e5e7eb] bg-white px-3 py-2 text-[13px] text-[#111827] [field-sizing:content] [overflow-wrap:anywhere] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none";

/**
 * field-sizing:content를 지원하지 않는 브라우저(Firefox·Safari)에서도 입력칸이 내용 높이만큼 자라게 한다.
 * 이게 없으면 긴 제목이 한 줄짜리 칸 안에서 스크롤되어 앞부분이 안 보인다.
 */
function useAutoGrow(value: string) {
  const ref = useRef<HTMLTextAreaElement>(null);
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = "auto";
    // scrollHeight에는 테두리가 빠져 있어서 (border-box) 그만큼 더한다.
    el.style.height = `${el.scrollHeight + el.offsetHeight - el.clientHeight}px`;
  }, [value]);
  return ref;
}

/** 카드 설명 입력칸 — 제목과 달리 Enter는 줄바꿈이다. */
const DESCRIPTION_INPUT_CLASS =
  "min-h-[52px] w-full resize-none overflow-hidden rounded-md border border-[#e5e7eb] bg-white px-3 py-2 text-[12px] text-[#374151] [field-sizing:content] [overflow-wrap:anywhere] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none";

const DUE_INPUT_CLASS =
  "flex-1 rounded-md border border-[#e5e7eb] px-2 py-1 text-[12px] text-[#111827] focus:border-[#6366f1] focus:outline-none disabled:bg-[#f9fafb] disabled:text-[#9ca3af]";

/** 시간을 비워 두면 그날 끝으로 본다. */
const DEFAULT_DUE_TIME = "23:59";

/** 날짜·시간 입력값 → 서버로 보낼 "YYYY-MM-DDTHH:mm". 날짜가 없으면 마감 없음(null). */
function toDueAt(date: string, time: string): string | null {
  return date ? `${date}T${time || DEFAULT_DUE_TIME}` : null;
}

interface DueInputsProps {
  /** label·input 연결용 — 한 화면에 폼이 여러 개 떠도 겹치지 않게 한다. */
  id: string;
  date: string;
  time: string;
  onChange: (date: string, time: string) => void;
  /** true면 "마감" 글자를 숨긴다 — 카드 상세 창처럼 바깥에 이미 항목 이름이 있을 때. */
  bare?: boolean;
}

/**
 * 마감 날짜·시간 입력. datetime-local 한 칸은 좁은 컬럼(대시보드)에서 잘려 달력 버튼이 안 보여서,
 * 날짜와 시간을 나누고 자리가 모자라면 줄바꿈되게 한다.
 */
function DueInputs({ id, date, time, onChange, bare = false }: DueInputsProps) {
  return (
    <div className="flex w-full flex-wrap items-center gap-1.5">
      {!bare && (
        <label htmlFor={id} className="shrink-0 text-[11.5px] text-[#6b7280]">
          마감
        </label>
      )}
      <input
        id={id}
        type="date"
        aria-label={bare ? "마감 날짜" : undefined}
        value={date}
        onChange={(e) => onChange(e.target.value, e.target.value ? time : "")}
        className={`min-w-[118px] ${DUE_INPUT_CLASS}`}
      />
      <input
        type="time"
        aria-label="마감 시간"
        title={date ? undefined : "날짜를 먼저 골라 주세요"}
        value={time}
        disabled={!date}
        onChange={(e) => onChange(date, e.target.value)}
        className={`min-w-[96px] ${DUE_INPUT_CLASS}`}
      />
      {date && (
        <button
          type="button"
          onClick={() => onChange("", "")}
          className="shrink-0 text-[11.5px] text-[#9ca3af] hover:text-[#ef4444]"
        >
          지우기
        </button>
      )}
    </div>
  );
}

interface AssigneeSelectProps {
  /** label·select 연결용 — 한 화면에 폼이 여러 개 떠도 겹치지 않게 한다. */
  id: string;
  members: ProjectMemberDto[];
  /** 고른 멤버의 userId, null이면 담당자 없음. */
  value: number | null;
  onChange: (assigneeId: number | null) => void;
  /** 지금 담당자가 프로젝트를 나가 멤버 목록에 없을 때도 선택값으로 보여 주기 위한 이름. */
  currentName?: string | null;
  /** true면 "담당" 글자를 숨긴다 — 카드 상세 창처럼 바깥에 이미 항목 이름이 있을 때. */
  bare?: boolean;
}

/** 카드 담당자 고르기 — 프로젝트 멤버 중 한 명, 또는 담당자 없음. */
export function AssigneeSelect({ id, members, value, onChange, currentName, bare = false }: AssigneeSelectProps) {
  const missing = value !== null && !members.some((member) => member.userId === value);
  return (
    <div className="flex w-full items-center gap-1.5">
      {!bare && (
        <label htmlFor={id} className="shrink-0 text-[11.5px] text-[#6b7280]">
          담당
        </label>
      )}
      <select
        id={id}
        aria-label={bare ? "담당자" : undefined}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}
        className="min-w-0 flex-1 rounded-md border border-[#e5e7eb] bg-white px-2 py-1 text-[12px] text-[#111827] focus:border-[#6366f1] focus:outline-none"
      >
        <option value="">담당자 없음</option>
        {missing && <option value={value}>{currentName ?? "알 수 없음"}</option>}
        {members.map((member) => (
          <option key={member.userId} value={member.userId}>
            {member.name}
          </option>
        ))}
      </select>
    </div>
  );
}

interface NewCardFormProps {
  labels: LabelDef[];
  members: ProjectMemberDto[];
  /**
   * customLabel은 "기타" 라벨에 직접 적은 글자(없으면 null). dueAt은 "YYYY-MM-DDTHH:mm" 또는 null(마감 없음).
   * assigneeId는 담당자 userId, null이면 담당자 없음.
   */
  onSubmit: (
    title: string,
    labelIds: string[],
    customLabel: string | null,
    dueAt: string | null,
    assigneeId: number | null,
  ) => void;
  onCancel: () => void;
}

/** 새 카드 입력 — Enter를 누르거나 폼 밖을 누르면 추가되고, 제목이 비었거나 Esc면 취소된다. */
export function NewCardForm({ labels, members, onSubmit, onCancel }: NewCardFormProps) {
  const [title, setTitle] = useState("");
  const [labelIds, setLabelIds] = useState<string[]>([]);
  const [customLabel, setCustomLabel] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [dueTime, setDueTime] = useState("");
  const [assigneeId, setAssigneeId] = useState<number | null>(null);
  // Enter로 닫히면서 blur가 한 번 더 불려도 카드가 두 장 생기지 않게 한다.
  const closedRef = useRef(false);
  const titleRef = useAutoGrow(title);

  const close = (submit: boolean) => {
    if (closedRef.current) return;
    closedRef.current = true;
    const trimmed = title.trim();
    if (submit && trimmed) onSubmit(trimmed, labelIds, customLabel.trim() || null, toDueAt(dueDate, dueTime), assigneeId);
    else onCancel();
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === "Escape") close(false);
    if (e.key === "Enter" && !e.nativeEvent.isComposing && !(e.target instanceof HTMLButtonElement)) {
      e.preventDefault();
      close(true);
    }
  };

  // 포커스가 폼 안(라벨·마감 입력)으로 옮겨 간 것이면 아직 입력 중이다.
  const handleBlur = (e: FocusEvent<HTMLDivElement>) => {
    if (!e.currentTarget.contains(e.relatedTarget)) close(true);
  };

  return (
    <div
      // tabIndex: 폼의 빈 곳을 눌러도 포커스가 폼 안에 남아 카드가 추가되어 버리지 않는다.
      tabIndex={-1}
      onKeyDown={handleKeyDown}
      onBlur={handleBlur}
      className="flex w-full shrink-0 flex-col gap-2 rounded-lg border border-[#e5e7eb] bg-white p-2 focus:outline-none"
    >
      <textarea
        ref={titleRef}
        autoFocus
        rows={1}
        value={title}
        onChange={(e) => setTitle(normalizeCardTitle(e.target.value))}
        placeholder="카드 제목"
        className={TITLE_INPUT_CLASS}
      />
      <div className="flex w-full items-start gap-2">
        <p className="shrink-0 pt-0.5 text-[11.5px] text-[#6b7280]">라벨</p>
        <LabelPicker
          labels={labels}
          selectedIds={labelIds}
          onToggle={(labelId) => setLabelIds((prev) => toggleLabelId(prev, labelId))}
          customLabel={customLabel}
          onCustomLabelChange={setCustomLabel}
          liveCustomLabel
        />
      </div>
      <AssigneeSelect id="new-card-assignee" members={members} value={assigneeId} onChange={setAssigneeId} />
      <DueInputs
        id="new-card-due"
        date={dueDate}
        time={dueTime}
        onChange={(date, time) => {
          setDueDate(date);
          setDueTime(time);
        }}
      />
    </div>
  );
}

interface CardDescriptionProps {
  text: string;
  /** 카드를 눌러 펼친 상태면 전부 보여 준다. */
  expanded: boolean;
}

/** 카드에 보이는 설명 — 평소에는 두 줄까지만 보이고, 카드를 누르면 전부 보인다. 줄바꿈은 그대로 살린다. */
export function CardDescription({ text, expanded }: CardDescriptionProps) {
  return (
    <p
      className={`w-full pl-[18px] text-[12px] break-keep whitespace-pre-wrap text-[#6b7280] [overflow-wrap:anywhere] ${
        expanded ? "" : "line-clamp-2"
      }`}
    >
      {text}
    </p>
  );
}

/** 카드 제목·설명·마감·담당자 수정 — 작성자 또는 관리자만 연다 (서버에서도 검사). */
export function CardEditForm({
  projectId,
  task,
  members,
  onDone,
}: {
  projectId: number;
  task: TaskCard;
  members: ProjectMemberDto[];
  onDone: () => void;
}) {
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description ?? "");
  // "2026-10-02T18:00:00" → 날짜 "2026-10-02", 시간 "18:00"
  const [dueDate, setDueDate] = useState(task.dueAt?.slice(0, 10) ?? "");
  const [dueTime, setDueTime] = useState(task.dueAt?.slice(11, 16) ?? "");
  const [assigneeId, setAssigneeId] = useState(task.assigneeId);
  const [saving, setSaving] = useState(false);
  const titleRef = useAutoGrow(title);
  const descriptionRef = useAutoGrow(description);

  const save = () => {
    const trimmed = title.trim();
    if (!trimmed || saving) return;
    setSaving(true);
    updateCard(projectId, task.id, trimmed, description.trim(), toDueAt(dueDate, dueTime), assigneeId)
      .then(onDone)
      .catch((err) => {
        setSaving(false);
        alertCardError(err, "카드를 수정하지 못했어요.");
      });
  };

  return (
    <div className="flex w-full shrink-0 flex-col gap-2 rounded-lg border border-[#6366f1] bg-white p-2.5">
      <textarea
        ref={titleRef}
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
      <textarea
        ref={descriptionRef}
        rows={2}
        maxLength={MAX_CARD_DESCRIPTION_LENGTH}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Escape") onDone();
        }}
        placeholder="설명 추가 (선택)"
        aria-label="카드 설명"
        className={DESCRIPTION_INPUT_CLASS}
      />
      <AssigneeSelect
        id={`assignee-${task.id}`}
        members={members}
        value={assigneeId}
        onChange={setAssigneeId}
        currentName={task.assigneeName}
      />
      <DueInputs
        id={`due-${task.id}`}
        date={dueDate}
        time={dueTime}
        onChange={(date, time) => {
          setDueDate(date);
          setDueTime(time);
        }}
      />
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

/**
 * 바깥(서버 스냅샷) 값으로 시작하는 입력값 — 바깥 값이 바뀌면(다른 사람이 고쳤거나 내 저장이 반영되면) 입력값도 맞춘다.
 * 카드 상세 창의 바로 고치기 입력칸들이 같이 쓴다.
 */
function useSyncedDraft<T>(value: T) {
  const [draft, setDraft] = useState(value);
  const [syncedValue, setSyncedValue] = useState(value);
  if (syncedValue !== value) {
    setSyncedValue(value);
    setDraft(value);
  }
  return [draft, setDraft] as const;
}

/** 바로 고치기 입력칸에서 Esc — 고치던 것을 되돌린다. 상세 창까지 닫히지 않게 이벤트를 여기서 멈춘다. */
function cancelOnEscape(e: KeyboardEvent<HTMLTextAreaElement>, revert: () => void) {
  if (e.key !== "Escape") return false;
  e.stopPropagation();
  revert();
  return true;
}

/** 카드 상세 창의 제목 — 누르면 바로 고치고, Enter를 누르거나 칸을 벗어나면 저장된다. 비우면 원래 제목으로 돌아간다. */
export function InlineTitleEditor({ value, onSave }: { value: string; onSave: (title: string) => void }) {
  const [draft, setDraft] = useSyncedDraft(value);
  const ref = useAutoGrow(draft);

  const commit = () => {
    const trimmed = draft.trim();
    if (trimmed && trimmed !== value) onSave(trimmed);
    else setDraft(value);
  };

  return (
    <textarea
      ref={ref}
      id="card-detail-title"
      rows={1}
      value={draft}
      onChange={(e) => setDraft(normalizeCardTitle(e.target.value))}
      onBlur={commit}
      onKeyDown={(e) => {
        if (cancelOnEscape(e, () => setDraft(value))) return;
        if (e.key === "Enter" && !e.nativeEvent.isComposing) {
          e.preventDefault();
          e.currentTarget.blur();
        }
      }}
      aria-label="카드 제목"
      className="w-full resize-none overflow-hidden break-keep rounded-md border border-transparent px-1.5 py-1 -mx-1.5 text-[18px] font-bold text-[#111827] [field-sizing:content] [overflow-wrap:anywhere] hover:border-[#e5e7eb] focus:border-[#6366f1] focus:outline-none"
    />
  );
}

/** 카드 상세 창의 설명 — Enter는 줄바꿈이고, 칸을 벗어나면 저장된다. 비우면 설명이 지워진다. */
export function InlineDescriptionEditor({ value, onSave }: { value: string; onSave: (description: string) => void }) {
  const [draft, setDraft] = useSyncedDraft(value);
  const ref = useAutoGrow(draft);

  return (
    <textarea
      ref={ref}
      rows={3}
      maxLength={MAX_CARD_DESCRIPTION_LENGTH}
      value={draft}
      onChange={(e) => setDraft(e.target.value)}
      onBlur={() => {
        if (draft.trim() !== value) onSave(draft.trim());
      }}
      onKeyDown={(e) => {
        if (cancelOnEscape(e, () => setDraft(value))) e.currentTarget.blur();
      }}
      placeholder="설명 추가"
      aria-label="카드 설명"
      className={`min-h-[72px] ${DESCRIPTION_INPUT_CLASS} text-[13px]`}
    />
  );
}

/** 카드 상세 창의 마감 — 날짜나 시간을 고르면 바로 저장된다. */
export function DueEditor({
  id,
  dueAt,
  onSave,
}: {
  id: string;
  /** 서버 값 "YYYY-MM-DDTHH:mm:ss", 없으면 null. */
  dueAt: string | null;
  onSave: (dueAt: string | null) => void;
}) {
  // "2026-10-02T18:00:00" → 날짜 "2026-10-02", 시간 "18:00"
  const [date, setDate] = useSyncedDraft(dueAt?.slice(0, 10) ?? "");
  const [time, setTime] = useSyncedDraft(dueAt?.slice(11, 16) ?? "");

  return (
    <DueInputs
      id={id}
      bare
      date={date}
      time={time}
      onChange={(nextDate, nextTime) => {
        setDate(nextDate);
        setTime(nextTime);
        const next = toDueAt(nextDate, nextTime);
        if (next !== (dueAt?.slice(0, 16) ?? null)) onSave(next);
      }}
    />
  );
}

interface ColumnPickerProps {
  columns: ColumnDef[];
  /** 카드가 지금 있는 컬럼 — 옮길 곳 목록에서는 뺀다. */
  currentId: string;
  onPick: (columnId: string) => void;
}

/** 카드 상세 창에 뜨는 "상태"(옮길 컬럼) 버튼 목록 — 드래그 대신 눌러서 옮긴다. */
export function ColumnPicker({ columns, currentId, onPick }: ColumnPickerProps) {
  const targets = columns.filter((column) => column.id !== currentId);
  if (targets.length === 0) {
    return <p className="text-[11.5px] text-[#9ca3af]">옮길 수 있는 다른 컬럼이 없어요</p>;
  }

  return (
    <>
      <p className="shrink-0 pt-0.5 text-[11.5px] text-[#6b7280]">상태</p>
      <div className="flex min-w-0 flex-wrap items-center gap-1.5">
        {targets.map((column) => (
          <button
            key={column.id}
            type="button"
            title={`'${column.name}'(으)로 이동`}
            onClick={() => onPick(column.id)}
            className="flex max-w-full items-center gap-1 rounded-full border border-[#e5e7eb] bg-white px-2 py-0.5 text-[11.5px] font-medium text-[#374151] transition-colors hover:border-[#6366f1] hover:bg-[#eeeefe]"
          >
            <span className="size-2 shrink-0 rounded-full" style={{ backgroundColor: column.color }} />
            <span className="truncate">{column.name}</span>
          </button>
        ))}
      </div>
    </>
  );
}

interface LabelPickerProps {
  labels: LabelDef[];
  selectedIds: string[];
  /** 선택된 라벨을 다시 누르면 빠지고, 최대 개수를 넘으면 눌리지 않는다. */
  onToggle: (labelId: string) => void;
  /** "기타" 라벨에 직접 적은 글자 (없으면 ""). */
  customLabel: string;
  onCustomLabelChange: (text: string) => void;
  /** true면 글자를 칠 때마다, 아니면 Enter를 누르거나 입력칸을 벗어날 때 onCustomLabelChange를 부른다. */
  liveCustomLabel?: boolean;
}

interface CustomLabelInputProps {
  value: string;
  live: boolean;
  onChange: (text: string) => void;
}

/** "기타" 라벨을 골랐을 때 뜨는 직접 입력칸 — 적은 글자가 카드에 "기타" 대신 보인다. */
function CustomLabelInput({ value, live, onChange }: CustomLabelInputProps) {
  const [draft, setDraft] = useState(value);
  // 다른 사람이 바꿨거나 내 저장이 반영되면 입력칸도 그 값으로 맞춘다.
  const [syncedValue, setSyncedValue] = useState(value);
  if (syncedValue !== value) {
    setSyncedValue(value);
    setDraft(value);
  }

  const commit = () => {
    const text = draft.trim();
    if (!live && text !== value) onChange(text);
  };

  return (
    <input
      autoFocus
      type="text"
      maxLength={MAX_LABEL_NAME_LENGTH}
      value={live ? value : draft}
      onChange={(e) => (live ? onChange(e.target.value) : setDraft(e.target.value))}
      onBlur={commit}
      onKeyDown={(e) => {
        if (!live && e.key === "Enter" && !e.nativeEvent.isComposing) e.currentTarget.blur();
      }}
      // 카드 안에서 눌러도 카드를 고른 것(컬럼 이동 목록 열기)으로 치지 않는다.
      onClick={(e) => e.stopPropagation()}
      placeholder="기타 라벨 직접 입력"
      aria-label="기타 라벨 직접 입력"
      className="w-full rounded-md border border-[#e5e7eb] bg-white px-2 py-1 text-[12px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
    />
  );
}

/** 설정 페이지 라벨 버튼 목록 — 새 카드 입력칸 아래와 기존 카드의 라벨 변경에서 같이 쓴다. */
export function LabelPicker({
  labels,
  selectedIds,
  onToggle,
  customLabel,
  onCustomLabelChange,
  liveCustomLabel = false,
}: LabelPickerProps) {
  const full = selectedIds.length >= MAX_CARD_LABELS;

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-1.5">
      <div className="flex flex-wrap items-center gap-1.5">
        {labels.map((label) => {
          const selected = selectedIds.includes(label.id);
          const disabled = !selected && full;
          const hint = label.isEtc ? "고르면 라벨 글자를 직접 적을 수 있어요" : undefined;
          return (
            <button
              key={label.id}
              type="button"
              aria-pressed={selected}
              disabled={disabled}
              title={disabled ? `라벨은 최대 ${MAX_CARD_LABELS}개까지 붙일 수 있어요` : hint}
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
      {labels.some((label) => selectedIds.includes(label.id) && label.isEtc) && (
        <CustomLabelInput value={customLabel} live={liveCustomLabel} onChange={onCustomLabelChange} />
      )}
    </div>
  );
}
