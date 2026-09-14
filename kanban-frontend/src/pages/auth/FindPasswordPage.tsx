import { useState } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";

export default function FindPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const handleSend = () => {
    // TODO: wire up to a real reset-link request once a backend exists.
    setSent(true);
  };

  return (
    <AuthCard title="비밀번호 찾기" subtitle="가입하신 이메일로 재설정 링크를 보내드려요">
      <Field
        label="이메일"
        type="email"
        placeholder="you@example.com"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      <Button variant="primary" fullWidth className="py-[13px] text-[14px]" onClick={handleSend}>
        재설정 링크 보내기
      </Button>

      {sent && (
        <div className="w-full rounded-[10px] bg-[#e9f9f1] px-4 py-3.5">
          <p className="text-[13px] text-[#109568]">
            재설정 링크를 이메일로 보냈어요. 받은편지함을 확인해주세요.
          </p>
        </div>
      )}

      <div className="flex w-full items-center justify-center">
        <Link to="/login" className="text-[13px] font-medium text-[#6366f1]">
          로그인 화면으로 돌아가기
        </Link>
      </div>
    </AuthCard>
  );
}
