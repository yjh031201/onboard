import { useState } from "react";
import Button from "../ui/Button";
import { ApiError } from "../../lib/api";
import { updateMemberRole } from "../../lib/user";
import type { AuthUser } from "../../lib/auth";
import RoleCheckboxes, { type Role } from "./RoleCheckboxes";

interface RolePopupProps {
  member: AuthUser;
  onClose: () => void;
  onUpdated: (member: AuthUser) => void;
}

export default function RolePopup({ member, onClose, onUpdated }: RolePopupProps) {
  const [role, setRole] = useState<Role>(member.role as Role);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSave = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const updated = await updateMemberRole(member.id, role);
      onUpdated(updated);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "권한 변경에 실패했어요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[360px] max-w-[90vw] flex-col gap-[18px] rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">{member.name}님의 권한</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <RoleCheckboxes value={role} onChange={setRole} />

        {error && (
          <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
            <p className="text-[13px] text-[#ef4444]">{error}</p>
          </div>
        )}

        <div className="flex w-full justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button type="button" variant="primary" onClick={handleSave} disabled={submitting} className="disabled:opacity-60">
            {submitting ? "저장 중..." : "저장"}
          </Button>
        </div>
      </div>
    </div>
  );
}
