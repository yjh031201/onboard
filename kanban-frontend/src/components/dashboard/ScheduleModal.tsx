import { useRef, useState } from "react";
import { Field, TextareaField } from "../ui/Field";
import Button from "../ui/Button";
import { ApiError } from "../../lib/api";
import {
  CATEGORY_LABELS,
  SCHEDULE_COLORS,
  createSchedule,
  deleteSchedule,
  toIsoDate,
  updateSchedule,
  type Schedule,
  type ScheduleCategoryCode,
} from "../../lib/schedules";
import type { ScheduleDate } from "../../types/dashboard";
import { formatTimeText, parseTimeText } from "../../utils/timeText";

const CATEGORIES = Object.keys(CATEGORY_LABELS) as ScheduleCategoryCode[];

/** 서버는 HH:mm:ss로 내려줌 — <input type="time">은 HH:mm만 받음. */
function toTimeInput(time: string | null): string {
  return time ? time.slice(0, 5) : "";
}

interface ScheduleModalProps {
  /** 새 일정 추가 시 시작일/종료일 기본값 (캘린더에서 선택한 날짜). */
  date: ScheduleDate;
  /** 있으면 수정 모드, 없으면 새 일정 추가. */
  schedule?: Schedule;
  /** 수정 모드에서 삭제 버튼 노출 여부 (작성자 본인 또는 OWNER/ADMIN). */
  canEdit?: boolean;
  onClose: () => void;
  onSaved: (schedule: Schedule) => void;
  onDeleted?: (id: number) => void;
}

export default function ScheduleModal({
  date,
  schedule,
  canEdit = true,
  onClose,
  onSaved,
  onDeleted,
}: ScheduleModalProps) {
  const isEdit = !!schedule;
  const [title, setTitle] = useState(schedule?.title ?? "");
  const [category, setCategory] = useState<ScheduleCategoryCode>(schedule?.category ?? "MEETING");
  const defaultDate = toIsoDate(date.year, date.monthIndex, date.date);
  const [startDate, setStartDate] = useState(schedule?.startDate ?? defaultDate);
  const [endDate, setEndDate] = useState(schedule?.endDate ?? defaultDate);
  const [startTime, setStartTime] = useState(toTimeInput(schedule?.startTime ?? null));
  // 왼쪽 "시간" 칸에 직접 쓰는 글자 ("오후 7시" 등). 해석되면 오른쪽 시작 시간이 따라 바뀐다.
  const [timeText, setTimeText] = useState(startTime ? formatTimeText(startTime) : "");
  const [description, setDescription] = useState(schedule?.content ?? "");
  // 설명에서 마지막으로 읽어낸 시각 — 설명 속 시각이 바뀔 때만 시작 시간을 덮어쓰기 위함
  const lastDescriptionTime = useRef(parseTimeText(schedule?.content ?? ""));
  const [color, setColor] = useState(schedule?.color ?? SCHEDULE_COLORS[0]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    if (!title.trim()) {
      setError("제목을 입력해주세요.");
      return;
    }
    if (!startDate || !endDate) {
      setError("시작일과 종료일을 입력해주세요.");
      return;
    }
    if (endDate < startDate) {
      setError("종료일은 시작일보다 빠를 수 없어요.");
      return;
    }

    const input = {
      title: title.trim(),
      content: description,
      category,
      startDate,
      endDate,
      startTime: startTime || null,
      endTime: null,
      color,
    };

    setSubmitting(true);
    setError(null);
    try {
      const saved = isEdit ? await updateSchedule(schedule.id, input) : await createSchedule(input);
      onSaved(saved);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "일정을 저장하지 못했어요.");
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!schedule || !window.confirm(`'${schedule.title}' 일정을 삭제할까요?`)) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteSchedule(schedule.id);
      onDeleted?.(schedule.id);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "일정을 삭제하지 못했어요.");
      setSubmitting(false);
    }
  };

  const readOnly = isEdit && !canEdit;

  // 시작일을 종료일 뒤로 옮기면 종료일도 같이 따라가게 (하루짜리 일정 기본)
  const handleTimeTextChange = (value: string) => {
    setTimeText(value);
    const parsed = parseTimeText(value);
    if (parsed) setStartTime(parsed);
  };

  const handleStartTimeChange = (value: string) => {
    setStartTime(value);
    setTimeText(value ? formatTimeText(value) : "");
  };

  const handleDescriptionChange = (value: string) => {
    setDescription(value);
    const parsed = parseTimeText(value);
    if (parsed && parsed !== lastDescriptionTime.current) {
      handleStartTimeChange(parsed);
    }
    lastDescriptionTime.current = parsed;
  };

  const handleStartDateChange = (value: string) => {
    setStartDate(value);
    if (value && endDate < value) setEndDate(value);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button
        type="button"
        aria-label="닫기"
        onClick={onClose}
        className="absolute inset-0 bg-black/45"
      />
      <div className="relative flex w-[440px] max-w-[90vw] flex-col gap-[18px] rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[18px] font-bold text-[#111827]">
            {!isEdit ? "일정 추가" : readOnly ? "일정 보기" : "일정 수정"}
          </p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <div className="flex w-full gap-3.5">
          <Field
            label="시작일"
            type="date"
            required
            disabled={readOnly}
            value={startDate}
            onChange={(e) => handleStartDateChange(e.target.value)}
          />
          <Field
            label="종료일"
            type="date"
            required
            min={startDate}
            disabled={readOnly}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>

        <Field
          label="제목"
          placeholder="일정 제목을 입력하세요"
          maxLength={200}
          disabled={readOnly}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        <div className="flex w-full flex-col gap-2">
          <p className="text-[13px] font-medium text-[#111827]">카테고리</p>
          <div className="flex items-start gap-2">
            {CATEGORIES.map((c) => (
              <button
                key={c}
                type="button"
                disabled={readOnly}
                onClick={() => setCategory(c)}
                className={`rounded-full px-3.5 py-[7px] text-[12.5px] font-medium ${
                  category === c ? "bg-[#6366f1] text-white" : "bg-[#f3f4f6] text-[#6b7280]"
                }`}
              >
                {CATEGORY_LABELS[c]}
              </button>
            ))}
          </div>
        </div>

        <div className="flex w-full flex-col gap-1.5">
          <div className="flex w-full gap-3.5">
            <Field
              label="시간"
              placeholder="예: 오후 7시, 7시 반"
              disabled={readOnly}
              value={timeText}
              onChange={(e) => handleTimeTextChange(e.target.value)}
            />
            <Field
              label="시작 시간"
              type="time"
              disabled={readOnly}
              value={startTime}
              onChange={(e) => handleStartTimeChange(e.target.value)}
            />
          </div>
          {!readOnly && (
            <p className="text-[12px] text-[#9ca3af]">
              오전/오후 없이 "7시"라고 쓰면 오후로 들어가요. 설명에 쓴 시간도 자동으로 반영돼요.
            </p>
          )}
        </div>

        <TextareaField
          label="설명"
          placeholder="일정에 대한 설명을 입력하세요"
          rows={2}
          maxLength={5000}
          disabled={readOnly}
          value={description}
          onChange={(e) => handleDescriptionChange(e.target.value)}
        />

        <div className="flex w-full flex-col gap-2">
          <p className="text-[13px] font-medium text-[#111827]">색상 태그</p>
          <div className="flex items-start gap-2.5">
            {SCHEDULE_COLORS.map((c) => (
              <button
                key={c}
                type="button"
                aria-label={`색상 ${c}`}
                disabled={readOnly}
                onClick={() => setColor(c)}
                style={{ backgroundColor: c }}
                className={`size-6 rounded-full ${
                  color === c ? "border-2 border-[#111827]" : ""
                }`}
              />
            ))}
          </div>
        </div>

        {error && <p className="text-[13px] text-[#ef4444]">{error}</p>}

        <div className="flex w-full items-start justify-between gap-2.5">
          <div>
            {isEdit && canEdit && (
              <button
                type="button"
                disabled={submitting}
                onClick={handleDelete}
                className="rounded-lg px-4 py-2.5 text-[13px] font-medium text-[#ef4444] hover:bg-[#fef2f2]"
              >
                삭제
              </button>
            )}
          </div>
          <div className="flex gap-2.5">
            <Button variant="secondary" onClick={onClose}>
              {readOnly ? "닫기" : "취소"}
            </Button>
            {!readOnly && (
              <Button
                variant="primary"
                disabled={submitting}
                onClick={handleSave}
                className="disabled:opacity-60"
              >
                {submitting ? "저장 중..." : "저장"}
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
