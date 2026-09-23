import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import { ApiError, login, loginWithProvider } from "../../lib/auth";

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.874 2.684-6.615z" />
      <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" />
      <path fill="#FBBC05" d="M3.964 10.706A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.706V4.962H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.038l3.007-2.332z" />
      <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.962L3.964 7.294C4.672 5.167 6.656 3.58 9 3.58z" />
    </svg>
  );
}

function NaverIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 18 18" aria-hidden="true">
      <path fill="#fff" d="M11.4 9.6 6.6 3H3v12h3.6V8.4l4.8 6.6H15V3h-3.6z" />
    </svg>
  );
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  // 소셜 로그인 실패 시 백엔드가 /login?oauthError=... 로 돌려보낸 메시지를 초기값으로 표시.
  const [error, setError] = useState<string | null>(
    () => new URLSearchParams(window.location.search).get("oauthError"),
  );
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard title="로그인" subtitle="계정에 로그인하고 프로젝트를 이어가세요">
      <form onSubmit={handleSubmit} className="flex w-full flex-col gap-[22px]">
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
          required
        />

        <div className="flex w-full items-center justify-between">
          <label className="flex items-center gap-2 text-[12.5px] text-[#6b7280]">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              className="size-4 rounded border-[#e5e7eb]"
            />
            로그인 상태 유지
          </label>
          <Link to="/find-password" className="text-[12.5px] font-medium text-[#6366f1]">
            비밀번호 찾기
          </Link>
        </div>

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
          {submitting ? "로그인 중..." : "로그인"}
        </Button>
      </form>

      <div className="flex w-full items-center gap-3">
        <div className="h-px flex-1 bg-[#e5e7eb]" />
        <span className="text-[12px] text-[#9ca3af]">또는</span>
        <div className="h-px flex-1 bg-[#e5e7eb]" />
      </div>

      <div className="flex w-full flex-col gap-2.5">
        <button
          type="button"
          onClick={() => loginWithProvider("google")}
          className="flex w-full items-center justify-center gap-2.5 rounded-lg border border-[#e5e7eb] bg-white py-[13px] text-[14px] font-medium text-[#111827] transition-colors hover:bg-[#f9fafb]"
        >
          <GoogleIcon />
          Google로 계속하기
        </button>
        <button
          type="button"
          onClick={() => loginWithProvider("naver")}
          className="flex w-full items-center justify-center gap-2.5 rounded-lg bg-[#03c75a] py-[13px] text-[14px] font-bold text-white transition-colors hover:bg-[#02b350]"
        >
          <NaverIcon />
          네이버로 계속하기
        </button>
      </div>

      <div className="flex w-full items-start justify-center gap-1.5 text-[12.5px]">
        <Link to="/find-id" className="text-[#6b7280]">
          아이디 찾기
        </Link>
        <span className="text-[#9ca3af]">·</span>
        <Link to="/find-password" className="text-[#6b7280]">
          비밀번호 찾기
        </Link>
      </div>

      <div className="flex w-full items-center justify-center gap-1 text-[13px]">
        <span className="text-[#6b7280]">계정이 없으신가요?</span>
        <Link to="/signup" className="font-medium text-[#6366f1]">
          회원가입
        </Link>
      </div>
    </AuthCard>
  );
}
