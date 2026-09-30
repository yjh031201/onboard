import { useState } from "react";
import Section, { Divider } from "../ui/Section";
import Button from "../ui/Button";
import NameColorForm, { type NameColorDraft } from "./NameColorForm";
import { PALETTE_COLORS } from "../../lib/palette";
import { ApiError } from "../../lib/api";
import { createLabel, deleteLabel, updateLabel, type LabelDef } from "../../lib/labels";
import { useLabels } from "../../hooks/useLabels";

/** "new"면 추가 폼, 라벨 id면 그 라벨의 편집 폼, null이면 폼 없음. */
type EditingTarget = "new" | string | null;

/**
 * 설정 페이지 "라벨" 섹션 — 팀원 누구나 추가/편집/삭제할 수 있다.
 * 목록 반영은 서버가 보내는 /topic/labels 브로드캐스트로 이루어진다 (useLabels).
 */
export default function LabelSettings() {
  const labels = useLabels();
  const [editing, setEditing] = useState<EditingTarget>(null);
  const [draft, setDraft] = useState<NameColorDraft>({ name: "", color: PALETTE_COLORS[0] });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const startEdit = (label: LabelDef) => {
    setEditing(label.id);
    setDraft({ name: label.name, color: label.color });
    setError(null);
  };

  const startAdd = () => {
    setEditing("new");
    setDraft({ name: "", color: PALETTE_COLORS[labels.length % PALETTE_COLORS.length] });
    setError(null);
  };

  const cancel = () => {
    setEditing(null);
    setError(null);
  };

  const save = async () => {
    const name = draft.name.trim();
    if (!name) {
      setError("라벨 이름을 입력해주세요.");
      return;
    }

    setSaving(true);
    try {
      if (editing === "new") {
        await createLabel(name, draft.color);
      } else if (editing) {
        await updateLabel(editing, name, draft.color);
      }
      setEditing(null);
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "라벨을 저장하지 못했어요.");
    } finally {
      setSaving(false);
    }
  };

  const remove = (label: LabelDef) => {
    if (!window.confirm(`'${label.name}' 라벨을 삭제할까요?\n이 라벨이 붙어 있던 카드는 라벨 없음으로 바뀌어요.`)) return;
    deleteLabel(label.id).catch((err) => {
      window.alert(err instanceof ApiError ? err.message : "라벨을 삭제하지 못했어요.");
    });
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
      placeholder="라벨 이름 (예: 회의)"
      maxLength={20}
    />
  );

  return (
    <Section title="라벨" description="카드에 붙일 라벨 색상과 이름을 관리하세요">
      {labels.length === 0 && editing !== "new" && (
        <p className="text-[13px] text-[#9ca3af]">아직 라벨이 없어요.</p>
      )}
      {labels.map((label, i) => (
        <div key={label.id} className="flex w-full flex-col gap-[18px]">
          {i > 0 && <Divider />}
          {editing === label.id ? (
            form
          ) : (
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="size-3.5 rounded-[4px]" style={{ backgroundColor: label.color }} />
                <p className="text-[13.5px] font-medium text-[#111827]">{label.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <button type="button" onClick={() => startEdit(label)} className="text-[#6366f1] hover:underline">
                  편집
                </button>
                <button type="button" onClick={() => remove(label)} className="text-[#9ca3af] hover:text-[#ef4444]">
                  삭제
                </button>
              </div>
            </div>
          )}
        </div>
      ))}
      {editing === "new" ? (
        <>
          {labels.length > 0 && <Divider />}
          {form}
        </>
      ) : (
        <Button variant="secondary" onClick={startAdd}>
          + 라벨 추가
        </Button>
      )}
    </Section>
  );
}
