import { useState } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  return (
    <AuthCard title="로그인" subtitle="계정에 로그인하고 프로젝트를 이어가세요">
      <Field
        label="이메일"
        type="email"
        placeholder="you@example.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Field
        label="비밀번호"
        type="password"
        placeholder="••••••••"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
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

      <Button variant="primary" fullWidth className="py-[13px] text-[14px]">
        로그인
      </Button>

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
