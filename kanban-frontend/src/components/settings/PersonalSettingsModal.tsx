import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Field } from "../ui/Field";
import Button from "../ui/Button";
import { Divider } from "../ui/Section";
import { ApiError } from "../../lib/api";
import { changePassword, deleteAccount, updateProfile } from "../../lib/user";
import type { AuthUser } from "../../lib/auth";

interface PersonalSettingsModalProps {
  user: AuthUser;
  onClose: () => void;
  onUpdated: (user: AuthUser) => void;
}

/** 비밀번호가 없는 소셜 로그인 계정이 탈퇴할 때 대신 입력하는 확인 문구. */
const DELETE_CONFIRM_PHRASE = "탈퇴합니다";

export default function PersonalSettingsModal({ user, onClose, onUpdated }: PersonalSettingsModalProps) {
  const [name, setName] = useState(user.name);
  // 소셜 로그인 계정은 phone이 null일 수 있어서(controlled input에 null을 그대로 넣으면 안 됨) 빈 문자열로.
  const [phone, setPhone] = useState(user.phone ?? "");
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [profileSubmitting, setProfileSubmitting] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  const navigate = useNavigate();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  // 소셜 로그인 전용 계정은 확인할 비밀번호가 없어서 확인 문구를 대신 입력받는다.
  const confirmByPassword = user.hasPassword !== false;

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

  const handleDeleteAccount = async (e: FormEvent) => {
    e.preventDefault();
    setDeleteError(null);

    if (!confirmByPassword && deleteConfirm.trim() !== DELETE_CONFIRM_PHRASE) {
      setDeleteError(`확인을 위해 ${DELETE_CONFIRM_PHRASE}를 정확히 입력해주세요.`);
      return;
    }

    setDeleteSubmitting(true);
    try {
      await deleteAccount(confirmByPassword ? deleteConfirm : undefined);
      navigate("/login", { replace: true });
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : "회원 탈퇴에 실패했어요.");
      setDeleteSubmitting(false);
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

        <Divider />

        <form onSubmit={handleDeleteAccount} className="flex w-full flex-col gap-[14px]">
          <p className="text-[13px] font-bold text-[#111827]">회원 탈퇴</p>
          <p className="text-[12.5px] leading-[1.6] text-[#6b7280]">
            탈퇴하면 계정, 내가 올린 파일과 일정, 혼자 쓰던 프로젝트가 삭제되고 되돌릴 수 없어요. 팀 프로젝트에 남긴
            카드와 활동 기록은 지워지지 않고 이름만 탈퇴한 사용자로 바뀌어요.
          </p>

          {deleteOpen && (
            <Field
              label={confirmByPassword ? "비밀번호 확인" : `확인 문구 (${DELETE_CONFIRM_PHRASE})`}
              type={confirmByPassword ? "password" : "text"}
              placeholder={confirmByPassword ? "••••••••" : DELETE_CONFIRM_PHRASE}
              value={deleteConfirm}
              onChange={(e) => setDeleteConfirm(e.target.value)}
              required
            />
          )}

          {deleteError && (
            <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
              <p className="text-[13px] text-[#ef4444]">{deleteError}</p>
            </div>
          )}

          <div className="flex w-full justify-end gap-2">
            {deleteOpen ? (
              <>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setDeleteOpen(false);
                    setDeleteConfirm("");
                    setDeleteError(null);
                  }}
                >
                  취소
                </Button>
                <Button type="submit" variant="danger" disabled={deleteSubmitting} className="disabled:opacity-60">
                  {deleteSubmitting ? "탈퇴 처리 중..." : "탈퇴하기"}
                </Button>
              </>
            ) : (
              <Button variant="secondary" onClick={() => setDeleteOpen(true)} className="text-[#ef4444]">
                회원 탈퇴
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
