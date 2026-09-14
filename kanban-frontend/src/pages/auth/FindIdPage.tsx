import { useState } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";

export default function FindIdPage() {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [result, setResult] = useState<string | null>(null);

  const handleFindId = () => {
    // TODO: wire up to a real lookup once a backend exists.
    setResult("jo***@gmail.com");
  };

  return (
    <AuthCard title="아이디 찾기" subtitle="가입 시 등록한 정보로 아이디를 찾아드려요">
      <Field
        label="이름"
        placeholder="홍길동"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <Field
        label="휴대폰 번호"
        placeholder="010-0000-0000"
        value={phone}
        onChange={(e) => setPhone(e.target.value)}
      />

      <Button variant="primary" fullWidth className="py-[13px] text-[14px]" onClick={handleFindId}>
        아이디 찾기
      </Button>

      {result && (
        <div className="w-full rounded-[10px] bg-[#eeeefe] px-4 py-3.5">
          <p className="text-[13px] text-[#6366f1]">회원님의 아이디는 {result} 입니다.</p>
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
