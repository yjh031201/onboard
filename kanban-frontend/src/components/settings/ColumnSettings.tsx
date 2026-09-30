import { useState, type DragEvent } from "react";
import Section, { Divider } from "../ui/Section";
import Button from "../ui/Button";
import NameColorForm, { type NameColorDraft } from "./NameColorForm";
import { ApiError } from "../../lib/api";
import { createColumn, deleteColumn, reorderColumns, updateColumn, type ColumnDef } from "../../lib/boardColumns";
import { useBoardColumns } from "../../hooks/useBoardColumns";

/**
 * 컬럼은 색을 고르지 않는다 — 새 컬럼은 이 중립 회색으로 저장되고(라벨 없는 카드의 점 색),
 * 이름을 바꿀 때는 기존 색을 그대로 보낸다.
 */
const DEFAULT_COLUMN_COLOR = "#94a3b8";

/** "new"면 추가 폼, 컬럼 id면 그 컬럼의 편집 폼, null이면 폼 없음. */
type EditingTarget = "new" | string | null;

/**
 * 설정 페이지 "보드 컬럼" 섹션 — 팀원 누구나 추가/이름 변경/삭제/순서 변경을 할 수 있다.
 * 순서는 ⠿ 손잡이로 끌어다 놓거나 ▲▼ 버튼으로 바꾼다.
 * 목록 반영은 서버가 보내는 /topic/columns 브로드캐스트로 이루어지고,
 * 칸반보드/대시보드도 같은 브로드캐스트를 받아 바로 컬럼이 바뀐다.
 */
export default function ColumnSettings() {
  const { columns, loading } = useBoardColumns();
  const [editing, setEditing] = useState<EditingTarget>(null);
  const [draft, setDraft] = useState<NameColorDraft>({ name: "", color: DEFAULT_COLUMN_COLOR });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dropTargetId, setDropTargetId] = useState<string | null>(null);

  const startEdit = (column: ColumnDef) => {
    setEditing(column.id);
    setDraft({ name: column.name, color: column.color });
    setError(null);
  };

  const startAdd = () => {
    setEditing("new");
    setDraft({ name: "", color: DEFAULT_COLUMN_COLOR });
    setError(null);
  };

  const cancel = () => {
    setEditing(null);
    setError(null);
  };

  const save = async () => {
    const name = draft.name.trim();
    if (!name) {
      setError("컬럼 이름을 입력해주세요.");
      return;
    }

    setSaving(true);
    try {
      if (editing === "new") {
        await createColumn(name, draft.color);
      } else if (editing) {
        await updateColumn(editing, name, draft.color);
      }
      setEditing(null);
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "컬럼을 저장하지 못했어요.");
    } finally {
      setSaving(false);
    }
  };

  const remove = (column: ColumnDef) => {
    if (!window.confirm(`'${column.name}' 컬럼을 삭제할까요?`)) return;
    // 카드가 남아 있으면 서버가 거절하고 이유를 돌려준다.
    deleteColumn(column.id).catch((err) => {
      window.alert(err instanceof ApiError ? err.message : "컬럼을 삭제하지 못했어요.");
    });
  };

  /** from 위치의 컬럼을 to 위치로 옮긴 순서를 서버에 보낸다. 화면 반영은 /topic/columns 브로드캐스트로 이루어진다. */
  const moveColumn = (from: number, to: number) => {
    if (from === to || to < 0 || to >= columns.length) return;
    const ids = columns.map((column) => column.id);
    const [moved] = ids.splice(from, 1);
    ids.splice(to, 0, moved);
    reorderColumns(ids).catch((err) => {
      window.alert(err instanceof ApiError ? err.message : "컬럼 순서를 바꾸지 못했어요.");
    });
  };

  const handleDrop = (targetIndex: number) => {
    const from = columns.findIndex((column) => column.id === draggingId);
    setDraggingId(null);
    setDropTargetId(null);
    if (from !== -1) moveColumn(from, targetIndex);
  };

  const form = (
    <NameColorForm
      draft={draft}
      onChange={setDraft}
      onSave={save}
      onCancel={cancel}
      saving={saving}
      error={error}
      submitText={editing === "new" ? "추가" : "저장"}
      placeholder="컬럼 이름 (예: 검토 중)"
      maxLength={30}
      showColor={false}
    />
  );

  return (
    <Section title="보드 컬럼" description="칸반보드에 표시할 컬럼을 관리하세요">
      {loading && <p className="text-[13px] text-[#9ca3af]">불러오는 중...</p>}
      {columns.map((column, i) => (
        <div key={column.id} className="flex w-full flex-col gap-[18px]">
          {i > 0 && <Divider />}
          {editing === column.id ? (
            form
          ) : (
            <div
              // 편집 폼이 열려 있을 때는 끌기를 막는다 (입력 중 텍스트 선택과 충돌).
              draggable={editing === null}
              onDragStart={() => setDraggingId(column.id)}
              onDragEnd={() => {
                setDraggingId(null);
                setDropTargetId(null);
              }}
              onDragOver={(e: DragEvent) => {
                if (!draggingId) return;
                e.preventDefault();
                setDropTargetId(column.id);
              }}
              onDrop={() => handleDrop(i)}
              className={`flex w-full items-center justify-between rounded-md transition-colors ${
                draggingId === column.id ? "opacity-40" : ""
              } ${dropTargetId === column.id && draggingId !== column.id ? "bg-[#eeeefe]" : ""}`}
            >
              <div className="flex items-center gap-2.5">
                <span
                  title="끌어서 순서 변경"
                  className={`select-none text-[13px] font-bold text-[#9ca3af] ${
                    editing === null ? "cursor-grab active:cursor-grabbing" : ""
                  }`}
                >
                  ⠿
                </span>
                <p className="text-[13.5px] font-medium text-[#111827]">{column.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <div className="flex items-center">
                  <button
                    type="button"
                    title="위로"
                    aria-label={`'${column.name}' 컬럼 위로`}
                    disabled={i === 0}
                    onClick={() => moveColumn(i, i - 1)}
                    className="rounded px-1 text-[#6b7280] hover:bg-[#f0f0f2] disabled:opacity-30 disabled:hover:bg-transparent"
                  >
                    ▲
                  </button>
                  <button
                    type="button"
                    title="아래로"
                    aria-label={`'${column.name}' 컬럼 아래로`}
                    disabled={i === columns.length - 1}
                    onClick={() => moveColumn(i, i + 1)}
                    className="rounded px-1 text-[#6b7280] hover:bg-[#f0f0f2] disabled:opacity-30 disabled:hover:bg-transparent"
                  >
                    ▼
                  </button>
                </div>
                <button type="button" onClick={() => startEdit(column)} className="text-[#6366f1] hover:underline">
                  이름 변경
                </button>
                <button type="button" onClick={() => remove(column)} className="text-[#9ca3af] hover:text-[#ef4444]">
                  삭제
                </button>
              </div>
            </div>
          )}
        </div>
      ))}
      {editing === "new" ? (
        <>
          {columns.length > 0 && <Divider />}
          {form}
        </>
      ) : (
        <Button variant="secondary" onClick={startAdd}>
          + 컬럼 추가
        </Button>
      )}
    </Section>
  );
}
