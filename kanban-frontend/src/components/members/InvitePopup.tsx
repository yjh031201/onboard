import { useState, type FormEvent } from "react";
import { Field } from "../ui/Field";
import Button from "../ui/Button";
import { Divider } from "../ui/Section";
import { ApiError } from "../../lib/api";
import { searchMember, updateMemberRole } from "../../lib/user";
import type { AuthUser } from "../../lib/auth";
import RoleCheckboxes, { type Role } from "./RoleCheckboxes";

interface InvitePopupProps {
  onClose: () => void;
  onAdded: (member: AuthUser) => void;
}

/** 이미 가입된 사용자를 이메일/휴대폰번호로 검색해서 찾은 뒤, 권한을 골라 구성원으로 추가. */
export default function InvitePopup({ onClose, onAdded }: InvitePopupProps) {
  const [keyword, setKeyword] = useState("");
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [found, setFound] = useState<AuthUser | null>(null);

  const [role, setRole] = useState<Role>("MEMBER");
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  const handleSearch = async (e: FormEvent) => {
    e.preventDefault();
    setSearchError(null);
    setFound(null);
    setSearching(true);
    try {
      const member = await searchMember(keyword.trim());
      setFound(member);
      setRole((member.role as Role) ?? "MEMBER");
    } catch (err) {
      setSearchError(err instanceof ApiError ? err.message : "사용자 검색에 실패했어요.");
    } finally {
      setSearching(false);
    }
  };

  const handleAdd = async () => {
    if (!found) return;
    setAddError(null);
    setAdding(true);
    try {
      const updated = await updateMemberRole(found.id, role);
      onAdded(updated);
      onClose();
    } catch (err) {
      setAddError(err instanceof ApiError ? err.message : "구성원 추가에 실패했어요.");
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[420px] max-w-[90vw] flex-col gap-[18px] rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">구성원 초대</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSearch} className="flex w-full flex-col gap-[14px]">
          <Field
            label="이메일 또는 휴대폰 번호"
            placeholder="example@email.com 또는 010-1234-5678"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            required
          />

          {searchError && (
            <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
              <p className="text-[13px] text-[#ef4444]">{searchError}</p>
            </div>
          )}

          <div className="flex w-full justify-end">
            <Button type="submit" variant="secondary" disabled={searching} className="disabled:opacity-60">
              {searching ? "검색 중..." : "검색"}
            </Button>
          </div>
        </form>

        {found && (
          <>
            <Divider />
            <div className="flex w-full flex-col gap-[14px]">
              <div className="flex flex-col gap-0.5">
                <p className="text-[13.5px] font-medium text-[#111827]">{found.name}</p>
                <p className="text-[12px] text-[#6b7280]">{found.email}</p>
              </div>

              <p className="text-[13px] font-medium text-[#111827]">권한 선택</p>
              <RoleCheckboxes value={role} onChange={setRole} />

              {addError && (
                <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
                  <p className="text-[13px] text-[#ef4444]">{addError}</p>
                </div>
              )}

              <div className="flex w-full justify-end">
                <Button
                  type="button"
                  variant="primary"
                  onClick={handleAdd}
                  disabled={adding}
                  className="disabled:opacity-60"
                >
                  {adding ? "추가 중..." : "구성원으로 추가"}
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
