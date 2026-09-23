import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import { ApiError, signup } from "../../lib/auth";

export default function SignupPage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않아요.");
      return;
    }
    if (!agreed) {
      setError("이용약관 및 개인정보처리방침에 동의해주세요.");
      return;
    }

    setSubmitting(true);
    try {
      await signup(name, email, password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "회원가입에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard title="회원가입" subtitle="몇 가지 정보만 입력하면 바로 시작할 수 있어요">
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
          label="비밀번호"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <Field
          label="비밀번호 확인"
          type="password"
          placeholder="••••••••"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          required
        />

        <label className="flex items-center gap-2 text-[12.5px] text-[#6b7280]">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(e) => setAgreed(e.target.checked)}
            className="size-4 rounded border-[#e5e7eb]"
          />
          이용약관 및 개인정보처리방침에 동의합니다
        </label>

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
          {submitting ? "가입 중..." : "회원가입"}
        </Button>
      </form>

      <div className="flex w-full items-center justify-center gap-1 text-[13px]">
        <span className="text-[#6b7280]">이미 계정이 있으신가요?</span>
        <Link to="/login" className="font-medium text-[#6366f1]">
          로그인
        </Link>
      </div>
    </AuthCard>
  );
}
