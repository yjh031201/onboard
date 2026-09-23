import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { completeOAuthLogin } from "../../lib/auth";

const params = new URLSearchParams(window.location.search);

/** 백엔드가 소셜 로그인 성공 후 /oauth/callback?code=... 로 리다이렉트하는 착지 페이지. */
export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const code = params.get("code");
  const [error, setError] = useState<string | null>(
    !code ? "로그인 정보를 받아오지 못했어요." : null,
  );

  useEffect(() => {
    if (!code) return;

    completeOAuthLogin(code)
      .then(() => navigate("/", { replace: true }))
      .catch(() => setError("로그인 처리 중 오류가 발생했어요."));
  }, [code, navigate]);

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-[#fafafa]">
      <p className="text-[14px] text-[#6b7280]">{error ?? "로그인 처리 중입니다..."}</p>
    </div>
  );
}
