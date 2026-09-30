import Button from "../ui/Button";
import { PALETTE_COLORS } from "../../lib/palette";

export interface NameColorDraft {
  name: string;
  color: string;
}

interface NameColorFormProps {
  draft: NameColorDraft;
  onChange: (draft: NameColorDraft) => void;
  onSave: () => void;
  onCancel: () => void;
  saving: boolean;
  error: string | null;
  submitText: string;
  placeholder: string;
  maxLength: number;
  /** false면 이름만 입력받는다 (보드 컬럼처럼 색을 고를 필요가 없을 때). */
  showColor?: boolean;
}

/** 설정 페이지의 라벨/컬럼 섹션이 공유하는 이름 (+ 색상) 입력 폼. */
export default function NameColorForm({
  draft,
  onChange,
  onSave,
  onCancel,
  saving,
  error,
  submitText,
  placeholder,
  maxLength,
  showColor = true,
}: NameColorFormProps) {
  return (
    <div className="flex w-full flex-col gap-3 rounded-lg border border-[#e5e7eb] bg-[#fafafa] p-3.5">
      <div className="flex w-full items-center gap-2.5">
        {showColor && (
          <span className="size-3.5 shrink-0 rounded-[4px]" style={{ backgroundColor: draft.color }} />
        )}
        <input
          autoFocus
          value={draft.name}
          maxLength={maxLength}
          onChange={(e) => onChange({ ...draft, name: e.target.value })}
          onKeyDown={(e) => {
            if (e.key === "Enter") onSave();
            if (e.key === "Escape") onCancel();
          }}
          placeholder={placeholder}
          className="min-w-0 flex-1 rounded-md border border-[#e5e7eb] bg-white px-3 py-2 text-[13px] text-[#111827] placeholder:text-[#9ca3af] focus:border-[#6366f1] focus:outline-none"
        />
      </div>
      {showColor && (
        <div className="flex flex-wrap items-center gap-2">
          {PALETTE_COLORS.map((color) => {
            const selected = draft.color.toLowerCase() === color;
            return (
              <button
                key={color}
                type="button"
                aria-label={`색상 ${color}`}
                aria-pressed={selected}
                onClick={() => onChange({ ...draft, color })}
                className={`size-6 rounded-md transition-transform ${
                  selected ? "scale-110 ring-2 ring-[#111827] ring-offset-2" : "hover:scale-110"
                }`}
                style={{ backgroundColor: color }}
              />
            );
          })}
        </div>
      )}
      {error && <p className="text-[12px] text-[#ef4444]">{error}</p>}
      <div className="flex items-center justify-end gap-2">
        <Button variant="ghost" onClick={onCancel} className="px-3 py-1.5">
          취소
        </Button>
        <Button onClick={onSave} disabled={saving} className="px-3 py-1.5 disabled:opacity-50">
          {saving ? "저장 중..." : submitText}
        </Button>
      </div>
    </div>
  );
}
