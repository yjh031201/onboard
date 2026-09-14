import { useState } from "react";
import { Field, TextareaField } from "../ui/Field";
import Button from "../ui/Button";
import type { ScheduleCategory, ScheduleDate } from "../../types/dashboard";

const CATEGORIES: ScheduleCategory[] = ["회의", "마감", "이벤트", "기타"];
const COLORS = ["#6366f1", "#f59e0b", "#10b981", "#ef4444", "#a855f7"];
const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"];

function formatDate(date?: ScheduleDate): string {
  const d = date ? new Date(date.year, date.monthIndex, date.date) : new Date();
  const weekday = WEEKDAY_LABELS[d.getDay()];
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${weekday})`;
}

interface ScheduleModalProps {
  initialDate?: ScheduleDate;
  onClose: () => void;
  onSave?: (schedule: {
    title: string;
    category: ScheduleCategory;
    startTime: string;
    endTime: string;
    description: string;
    color: string;
  }) => void;
}

export default function ScheduleModal({ initialDate, onClose, onSave }: ScheduleModalProps) {
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState<ScheduleCategory>("회의");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [description, setDescription] = useState("");
  const [color, setColor] = useState(COLORS[0]);

  const handleSave = () => {
    onSave?.({ title, category, startTime, endTime, description, color });
    onClose();
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
          <p className="text-[18px] font-bold text-[#111827]">일정 추가</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <div className="inline-flex w-fit items-center gap-1.5 rounded-full bg-[#eeeefe] px-3 py-1.5">
          <span className="text-[12px]">📅</span>
          <span className="text-[12.5px] font-medium text-[#6366f1]">
            {formatDate(initialDate)}
          </span>
        </div>

        <Field
          label="제목"
          placeholder="일정 제목을 입력하세요"
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
                onClick={() => setCategory(c)}
                className={`rounded-full px-3.5 py-[7px] text-[12.5px] font-medium ${
                  category === c ? "bg-[#6366f1] text-white" : "bg-[#f3f4f6] text-[#6b7280]"
                }`}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        <div className="flex w-full gap-3.5">
          <Field
            label="시작 시간"
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
          />
          <Field
            label="종료 시간"
            type="time"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
          />
        </div>

        <TextareaField
          label="설명"
          placeholder="일정에 대한 설명을 입력하세요"
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <div className="flex w-full flex-col gap-2">
          <p className="text-[13px] font-medium text-[#111827]">참여자</p>
          <div className="flex items-start">
            {["양", "김", "이"].map((initial) => (
              <div
                key={initial}
                className="-mr-1.5 flex size-[30px] items-center justify-center rounded-full border-2 border-white bg-[#6366f1]"
              >
                <span className="text-[12px] font-bold text-white">{initial}</span>
              </div>
            ))}
            <button
              type="button"
              aria-label="참여자 추가"
              className="flex size-[30px] items-center justify-center rounded-full border-2 border-white bg-[#f3f4f6] text-[14px] font-bold text-[#6b7280]"
            >
              +
            </button>
          </div>
        </div>

        <div className="flex w-full flex-col gap-2">
          <p className="text-[13px] font-medium text-[#111827]">색상 태그</p>
          <div className="flex items-start gap-2.5">
            {COLORS.map((c) => (
              <button
                key={c}
                type="button"
                aria-label={`색상 ${c}`}
                onClick={() => setColor(c)}
                style={{ backgroundColor: c }}
                className={`size-6 rounded-full ${
                  color === c ? "border-2 border-[#111827]" : ""
                }`}
              />
            ))}
          </div>
        </div>

        <div className="flex w-full items-start justify-end gap-2.5">
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button variant="primary" onClick={handleSave}>
            저장
          </Button>
        </div>
      </div>
    </div>
  );
}
