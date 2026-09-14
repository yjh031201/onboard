import { useState } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";

export default function SignupPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreed, setAgreed] = useState(false);

  return (
    <AuthCard title="회원가입" subtitle="몇 가지 정보만 입력하면 바로 시작할 수 있어요">
      <Field
        label="이름"
        placeholder="홍길동"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
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
      <Field
        label="비밀번호 확인"
        type="password"
        placeholder="••••••••"
        value={passwordConfirm}
        onChange={(e) => setPasswordConfirm(e.target.value)}
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

      <Button variant="primary" fullWidth className="py-[13px] text-[14px]">
        회원가입
      </Button>

      <div className="flex w-full items-center justify-center gap-1 text-[13px]">
        <span className="text-[#6b7280]">이미 계정이 있으신가요?</span>
        <Link to="/login" className="font-medium text-[#6366f1]">
          로그인
        </Link>
      </div>
    </AuthCard>
  );
}
