import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Field, TextareaField } from "../ui/Field";
import Button from "../ui/Button";
import { ApiError } from "../../lib/api";
import { createProject } from "../../lib/projects";

interface CreateProjectModalProps {
  onClose: () => void;
}

/**
 * "+ 새 프로젝트" 모달 — 이름/설명을 받아 프로젝트를 만들고, 만든 사람이 바로 OWNER가 된다.
 * 생성에 성공하면 그 프로젝트의 팀원 페이지로 이동해서 바로 멤버를 초대할 수 있게 한다.
 */
export default function CreateProjectModal({ onClose }: CreateProjectModalProps) {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError("프로젝트 이름을 입력해주세요.");
      return;
    }
    setError(null);
    setSaving(true);
    try {
      const project = await createProject(trimmed, description.trim());
      navigate(`/projects/${project.id}/members`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "프로젝트를 만들지 못했어요.");
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[420px] max-w-[90vw] flex-col gap-[18px] rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">새 프로젝트 만들기</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex w-full flex-col gap-[14px]">
          <Field
            label="프로젝트 이름"
            placeholder="예: 마케팅팀 캠페인"
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />
          <TextareaField
            label="설명 (선택)"
            rows={3}
            maxLength={2000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          {error && (
            <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
              <p className="text-[13px] text-[#ef4444]">{error}</p>
            </div>
          )}

          <div className="flex w-full justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" variant="primary" disabled={saving} className="disabled:opacity-60">
              {saving ? "만드는 중..." : "만들기"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
