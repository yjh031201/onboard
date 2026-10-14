import { DUE_SOON_DAYS, EMPTY_CARD_FILTER, isFilterActive, type CardFilter } from "../../lib/cardFilter";
import type { LabelDef } from "../../lib/labels";

interface CardFilterBarProps {
  labels: LabelDef[];
  filter: CardFilter;
  onChange: (filter: CardFilter) => void;
  /** 필터를 통과한 카드 수 / 전체 카드 수. */
  shownCount: number;
  totalCount: number;
}

const CHIP_CLASS =
  "flex shrink-0 items-center gap-1 rounded-full border px-2.5 py-1 text-[12px] font-medium transition-colors";
const CHIP_OFF = "border-[#e5e7eb] bg-white text-[#374151] hover:bg-[#f9fafb]";
const CHIP_ON = "border-[#6366f1] bg-[#eeeefe] text-[#4f46e5]";

/** 칸반 보드 위의 검색·필터 줄 — 검색어, 라벨, 내 카드, 마감 임박. */
export default function CardFilterBar({ labels, filter, onChange, shownCount, totalCount }: CardFilterBarProps) {
  const active = isFilterActive(filter);

  const toggleLabel = (labelId: string) => {
    const labelIds = filter.labelIds.includes(labelId)
      ? filter.labelIds.filter((id) => id !== labelId)
      : [...filter.labelIds, labelId];
    onChange({ ...filter, labelIds });
  };

  return (
    <div className="flex w-full shrink-0 flex-wrap items-center gap-x-3 gap-y-2">
      <div className="flex h-9 w-[240px] shrink-0 items-center gap-2 rounded-lg border border-[#e5e7eb] bg-white px-3 focus-within:border-[#6366f1]">
        <span className="text-[13px] text-[#9ca3af]">🔍</span>
        <input
          type="search"
          value={filter.query}
          onChange={(e) => onChange({ ...filter, query: e.target.value })}
          placeholder="제목·설명·작성자 검색"
          aria-label="카드 검색"
          className="w-full min-w-0 bg-transparent text-[13px] text-[#111827] outline-none placeholder:text-[#9ca3af]"
        />
      </div>

      <div className="flex flex-wrap items-center gap-1.5">
        <button
          type="button"
          aria-pressed={filter.mineOnly}
          onClick={() => onChange({ ...filter, mineOnly: !filter.mineOnly })}
          className={`${CHIP_CLASS} ${filter.mineOnly ? CHIP_ON : CHIP_OFF}`}
        >
          내 카드
        </button>
        {labels.map((label) => {
          const selected = filter.labelIds.includes(label.id);
          return (
            <button
              key={label.id}
              type="button"
              aria-pressed={selected}
              onClick={() => toggleLabel(label.id)}
              // 선택되면 라벨 색으로 채운다 (카드에 붙은 라벨과 같은 모양).
              className={`${CHIP_CLASS} ${selected ? "text-white" : CHIP_OFF}`}
              style={selected ? { backgroundColor: label.color, borderColor: label.color } : undefined}
            >
              {!selected && <span className="size-2 rounded-full" style={{ backgroundColor: label.color }} />}
              {label.name}
            </button>
          );
        })}
        <button
          type="button"
          aria-pressed={filter.dueSoon}
          title={`마감이 지났거나 ${DUE_SOON_DAYS}일 안에 닥치는 카드`}
          onClick={() => onChange({ ...filter, dueSoon: !filter.dueSoon })}
          className={`${CHIP_CLASS} ${filter.dueSoon ? CHIP_ON : CHIP_OFF}`}
        >
          ⏰ 마감 임박
        </button>
      </div>

      {active && (
        <div className="ml-auto flex shrink-0 items-center gap-2.5">
          <p className="text-[12px] text-[#6b7280]">
            {totalCount}개 중 {shownCount}개
          </p>
          <button
            type="button"
            onClick={() => onChange(EMPTY_CARD_FILTER)}
            className="text-[12px] font-medium text-[#6366f1] hover:underline"
          >
            초기화
          </button>
        </div>
      )}
    </div>
  );
}
