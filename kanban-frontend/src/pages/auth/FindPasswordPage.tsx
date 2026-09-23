import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import { ApiError, resetPassword } from "../../lib/auth";

export default function FindPasswordPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword !== newPasswordConfirm) {
      setError("새 비밀번호가 일치하지 않아요.");
      return;
    }

    setSubmitting(true);
    try {
      await resetPassword(name, email, newPassword);
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "비밀번호 재설정에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <AuthCard title="비밀번호 재설정 완료" subtitle="새 비밀번호로 로그인해주세요">
        <div className="w-full rounded-[10px] bg-[#e9f9f1] px-4 py-3.5">
          <p className="text-[13px] text-[#109568]">비밀번호가 성공적으로 변경됐어요.</p>
        </div>
        <Link to="/login" className="w-full">
          <Button variant="primary" fullWidth className="py-[13px] text-[14px]">
            로그인하러 가기
          </Button>
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="비밀번호 찾기" subtitle="본인 확인 후 바로 새 비밀번호를 설정할 수 있어요">
      <form onSubmit={handleSubmit} className="flex w-full flex-col gap-[22px]">
        <Field
          label="이름"
          placeholder="홍길동"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Field
          label="이메일"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
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

        {error && (
          <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
            <p className="text-[13px] text-[#ef4444]">{error}</p>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          fullWidth
          disabled={submitting}
          className="py-[13px] text-[14px] disabled:opacity-60"
        >
          {submitting ? "변경 중..." : "비밀번호 재설정"}
        </Button>
      </form>

      <div className="flex w-full items-center justify-center">
        <Link to="/login" className="text-[13px] font-medium text-[#6366f1]">
          로그인 화면으로 돌아가기
        </Link>
      </div>
    </AuthCard>
  );
}
