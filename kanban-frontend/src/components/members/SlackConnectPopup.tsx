import { useState } from "react";
import { Field } from "../ui/Field";
import Button from "../ui/Button";
import { Divider } from "../ui/Section";
import { ApiError } from "../../lib/api";
import { connectSlackWebhook, startConnect } from "../../lib/integrations";

interface SlackConnectPopupProps {
  onClose: () => void;
  onConnected: () => void;
}

const WEBHOOK_URL_PREFIX = "https://hooks.slack.com/services/";

export default function SlackConnectPopup({ onClose, onConnected }: SlackConnectPopupProps) {
  const [webhookUrl, setWebhookUrl] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const trimmed = webhookUrl.trim();

  const handleConnect = async () => {
    if (!trimmed.startsWith(WEBHOOK_URL_PREFIX)) {
      setError(`웹훅 주소는 ${WEBHOOK_URL_PREFIX} 로 시작해야 해요.`);
      return;
    }
    setError(null);
    setConnecting(true);
    try {
      await connectSlackWebhook(trimmed);
      onConnected();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Slack 연결에 실패했어요.");
    } finally {
      setConnecting(false);
    }
  };

  const handleOAuth = () => {
    setError(null);
    startConnect("SLACK").catch((err) => {
      setError(err instanceof ApiError ? err.message : "연동을 시작하지 못했어요.");
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[460px] max-w-[90vw] max-h-[85vh] flex-col gap-[18px] overflow-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">Slack 연결</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <p className="text-[12.5px] text-[#6b7280]">
          알림을 받을 채널의 Incoming Webhook 주소를 붙여 넣으세요.{" "}
          <a
            href="https://api.slack.com/apps"
            target="_blank"
            rel="noreferrer"
            className="text-[#6366f1] hover:underline"
          >
            Slack 앱 설정
          </a>
          의 Incoming Webhooks 메뉴에서 만들 수 있어요.
        </p>

        <Field
          label="웹훅 주소"
          placeholder={`${WEBHOOK_URL_PREFIX}...`}
          value={webhookUrl}
          onChange={(e) => setWebhookUrl(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && trimmed && !connecting) handleConnect();
          }}
          autoFocus
        />

        {error && (
          <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
            <p className="text-[13px] text-[#ef4444]">{error}</p>
          </div>
        )}

        <div className="flex w-full justify-end">
          <Button
            variant="primary"
            onClick={handleConnect}
            disabled={connecting || !trimmed}
            className="disabled:opacity-60"
          >
            {connecting ? "연결 중..." : "연결하기"}
          </Button>
        </div>

        <Divider />

        <div className="flex w-full items-center justify-between">
          <p className="text-[12.5px] text-[#6b7280]">Slack 앱 키가 서버에 설정돼 있다면</p>
          <Button variant="ghost" onClick={handleOAuth}>
            Slack 계정으로 연결
          </Button>
        </div>
      </div>
    </div>
  );
}
