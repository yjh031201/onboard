import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import { ApiError, confirmPasswordReset, requestPasswordResetCode } from "../../lib/auth";

type Step = "email" | "code" | "done";

/**
 * 비밀번호 찾기 — 1) 이메일로 6자리 인증 코드를 받고 2) 코드와 새 비밀번호를 입력한다.
 * 메일함을 가진 본인만 코드를 받을 수 있어서, 이름·이메일만 알아도 남의 비밀번호를 바꿀 수 있던 예전 방식의 구멍을 막는다.
 */
export default function FindPasswordPage() {
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const sendCode = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await requestPasswordResetCode(email);
      setStep("code");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "인증 코드를 보내지 못했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequest = (e: FormEvent) => {
    e.preventDefault();
    sendCode();
  };

  const handleConfirm = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword !== newPasswordConfirm) {
      setError("새 비밀번호가 일치하지 않아요.");
      return;
    }

    setSubmitting(true);
    try {
      await confirmPasswordReset(email, code, newPassword);
      setStep("done");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "비밀번호 재설정에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  if (step === "done") {
    return (
      <AuthCard title="비밀번호 재설정 완료" subtitle="새 비밀번호로 로그인해주세요">
        <div className="w-full rounded-[10px] bg-[#e9f9f1] px-4 py-3.5">
          <p className="text-[13px] text-[#109568]">
            비밀번호가 변경됐어요. 보안을 위해 다른 기기의 로그인은 모두 해제됐어요.
          </p>
        </div>
        <Link to="/login" className="w-full">
          <Button variant="primary" fullWidth className="py-[13px] text-[14px]">
            로그인하러 가기
          </Button>
        </Link>
      </AuthCard>
    );
  }

  const errorBox = error && (
    <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
      <p className="text-[13px] text-[#ef4444]">{error}</p>
    </div>
  );

  return (
    <AuthCard
      title="비밀번호 찾기"
      subtitle={step === "email" ? "가입한 이메일로 인증 코드를 보내드려요" : `${email}로 보낸 6자리 코드를 입력해주세요`}
    >
      {step === "email" ? (
        <form onSubmit={handleRequest} className="flex w-full flex-col gap-[22px]">
          <Field
            label="이메일"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          {errorBox}
          <Button
            type="submit"
            variant="primary"
            fullWidth
            disabled={submitting}
            className="py-[13px] text-[14px] disabled:opacity-60"
          >
            {submitting ? "보내는 중..." : "인증 코드 받기"}
          </Button>
        </form>
      ) : (
        <form onSubmit={handleConfirm} className="flex w-full flex-col gap-[22px]">
          <Field
            label="인증 코드"
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="123456"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
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
          <p className="text-[12px] text-[#6b7280]">
            메일이 안 왔다면 스팸함을 확인하거나{" "}
            <button type="button" onClick={sendCode} disabled={submitting} className="font-medium text-[#6366f1]">
              코드를 다시 받아보세요
            </button>
            . 코드는 10분 동안 유효해요.
          </p>
          {errorBox}
          <Button
            type="submit"
            variant="primary"
            fullWidth
            disabled={submitting || code.length !== 6}
            className="py-[13px] text-[14px] disabled:opacity-60"
          >
            {submitting ? "변경 중..." : "비밀번호 재설정"}
          </Button>
        </form>
      )}

      <div className="flex w-full items-center justify-center">
        <Link to="/login" className="text-[13px] font-medium text-[#6366f1]">
          로그인 화면으로 돌아가기
        </Link>
      </div>
    </AuthCard>
  );
}
