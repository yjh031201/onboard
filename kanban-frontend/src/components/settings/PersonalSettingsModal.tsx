import { useState, type FormEvent } from "react";
import { Field } from "../ui/Field";
import Button from "../ui/Button";
import { Divider } from "../ui/Section";
import { ApiError } from "../../lib/api";
import { changePassword, updateProfile } from "../../lib/user";
import type { AuthUser } from "../../lib/auth";

interface PersonalSettingsModalProps {
  user: AuthUser;
  onClose: () => void;
  onUpdated: (user: AuthUser) => void;
}

export default function PersonalSettingsModal({ user, onClose, onUpdated }: PersonalSettingsModalProps) {
  const [name, setName] = useState(user.name);
  const [phone, setPhone] = useState(user.phone);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [profileSubmitting, setProfileSubmitting] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  const handleSaveProfile = async (e: FormEvent) => {
    e.preventDefault();
    setProfileError(null);
    setProfileSuccess(false);
    setProfileSubmitting(true);
    try {
      const updated = await updateProfile(name, phone);
      onUpdated(updated);
      setProfileSuccess(true);
    } catch (err) {
      setProfileError(err instanceof ApiError ? err.message : "프로필 저장에 실패했어요.");
    } finally {
      setProfileSubmitting(false);
    }
  };

  const handleChangePassword = async (e: FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(false);

    if (newPassword !== newPasswordConfirm) {
      setPasswordError("새 비밀번호가 일치하지 않아요.");
      return;
    }

    setPasswordSubmitting(true);
    try {
      await changePassword(currentPassword, newPassword);
      setPasswordSuccess(true);
      setCurrentPassword("");
      setNewPassword("");
      setNewPasswordConfirm("");
    } catch (err) {
      setPasswordError(err instanceof ApiError ? err.message : "비밀번호 변경에 실패했어요.");
    } finally {
      setPasswordSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex max-h-[85vh] w-[440px] max-w-[90vw] flex-col gap-[22px] overflow-y-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[18px] font-bold text-[#111827]">개인설정</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSaveProfile} className="flex w-full flex-col gap-[14px]">
          <p className="text-[13px] font-bold text-[#111827]">프로필 정보</p>

          <Field label="이메일" value={user.email} disabled className="bg-[#f9fafb] text-[#9ca3af]" />
          <Field
            label="이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <Field
            label="휴대폰 번호"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />

          {profileError && (
            <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
              <p className="text-[13px] text-[#ef4444]">{profileError}</p>
            </div>
          )}
          {profileSuccess && (
            <div className="w-full rounded-[10px] bg-[#e9f9f1] px-4 py-3">
              <p className="text-[13px] text-[#109568]">프로필이 저장됐어요.</p>
            </div>
          )}

          <div className="flex w-full justify-end">
            <Button type="submit" variant="primary" disabled={profileSubmitting} className="disabled:opacity-60">
              {profileSubmitting ? "저장 중..." : "프로필 저장"}
            </Button>
          </div>
        </form>

        <Divider />

        <form onSubmit={handleChangePassword} className="flex w-full flex-col gap-[14px]">
          <p className="text-[13px] font-bold text-[#111827]">비밀번호 변경</p>

          <Field
            label="현재 비밀번호"
            type="password"
            placeholder="••••••••"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
          <Field
            label="새 비밀번호"
            type="password"
            placeholder="••••••••"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            required
          />
          <Field
            label="새 비밀번호 확인"
            type="password"
            placeholder="••••••••"
            value={newPasswordConfirm}
            onChange={(e) => setNewPasswordConfirm(e.target.value)}
            required
          />

          {passwordError && (
            <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
              <p className="text-[13px] text-[#ef4444]">{passwordError}</p>
            </div>
          )}
          {passwordSuccess && (
            <div className="w-full rounded-[10px] bg-[#e9f9f1] px-4 py-3">
              <p className="text-[13px] text-[#109568]">비밀번호가 변경됐어요.</p>
            </div>
          )}

          <div className="flex w-full justify-end">
            <Button type="submit" variant="primary" disabled={passwordSubmitting} className="disabled:opacity-60">
              {passwordSubmitting ? "변경 중..." : "비밀번호 변경"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
