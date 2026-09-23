import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import AuthCard from "../../components/auth/AuthCard";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import { ApiError, findId } from "../../lib/auth";

export default function FindIdPage() {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const response = await findId(name, phone);
      setResult(response.maskedEmail);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "아이디를 찾지 못했어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard title="아이디 찾기" subtitle="가입 시 등록한 정보로 아이디를 찾아드려요">
      <form onSubmit={handleSubmit} className="flex w-full flex-col gap-[22px]">
        <Field
          label="이름"
          placeholder="홍길동"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Field
          label="휴대폰 번호"
          type="tel"
          placeholder="010-0000-0000"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
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
          {submitting ? "찾는 중..." : "아이디 찾기"}
        </Button>
      </form>

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
