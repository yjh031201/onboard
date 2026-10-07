import { useEffect, useState } from "react";
import { Field } from "../ui/Field";
import Button from "../ui/Button";
import { Divider } from "../ui/Section";
import { ApiError } from "../../lib/api";
import {
  disconnectIntegration,
  fetchDriveFiles,
  fetchGithubIssues,
  sendSlackTestMessage,
  setGithubRepo,
  type DriveFile,
  type GithubIssue,
  type IntegrationStatus,
} from "../../lib/integrations";

interface IntegrationManagePopupProps {
  status: IntegrationStatus;
  canManage: boolean;
  onClose: () => void;
  onDisconnected: () => void;
}

const PROVIDER_TITLE: Record<IntegrationStatus["provider"], string> = {
  GITHUB: "GitHub 연동 관리",
  SLACK: "Slack 연동 관리",
  GOOGLE_DRIVE: "Google Drive 연동 관리",
};

export default function IntegrationManagePopup({ status, canManage, onClose, onDisconnected }: IntegrationManagePopupProps) {
  const [error, setError] = useState<string | null>(null);
  const [disconnecting, setDisconnecting] = useState(false);

  const handleDisconnect = async () => {
    setError(null);
    setDisconnecting(true);
    try {
      await disconnectIntegration(status.provider);
      onDisconnected();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "연동 해제에 실패했어요.");
    } finally {
      setDisconnecting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[460px] max-w-[90vw] max-h-[85vh] flex-col gap-[18px] overflow-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">{PROVIDER_TITLE[status.provider]}</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]"
          >
            ✕
          </button>
        </div>

        <div className="flex flex-col gap-0.5">
          <p className="text-[13.5px] font-medium text-[#111827]">{status.accountLabel}</p>
          {status.detail && <p className="text-[12px] text-[#6b7280]">{status.detail}</p>}
        </div>

        <Divider />

        {status.provider === "GITHUB" && <GithubPanel status={status} canManage={canManage} />}
        {status.provider === "SLACK" && <SlackPanel canManage={canManage} />}
        {status.provider === "GOOGLE_DRIVE" && <DrivePanel />}

        {canManage && (
          <>
            <Divider />

            {error && (
              <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
                <p className="text-[13px] text-[#ef4444]">{error}</p>
              </div>
            )}

            <div className="flex w-full justify-end">
              <Button variant="danger" onClick={handleDisconnect} disabled={disconnecting} className="disabled:opacity-60">
                {disconnecting ? "해제 중..." : "연동 해제"}
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function GithubPanel({ status, canManage }: { status: IntegrationStatus; canManage: boolean }) {
  const [owner, setOwner] = useState(status.detail?.split("/")[0] ?? "");
  const [repo, setRepo] = useState(status.detail?.split("/")[1] ?? "");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const [issues, setIssues] = useState<GithubIssue[]>([]);
  const [loadingIssues, setLoadingIssues] = useState(false);
  const [issuesError, setIssuesError] = useState<string | null>(null);

  const loadIssues = () => {
    setLoadingIssues(true);
    setIssuesError(null);
    fetchGithubIssues()
      .then(setIssues)
      .catch((err) => setIssuesError(err instanceof ApiError ? err.message : "이슈를 불러오지 못했어요."))
      .finally(() => setLoadingIssues(false));
  };

  useEffect(() => {
    if (status.detail) loadIssues();
  }, [status.detail]);

  const handleSaveRepo = async () => {
    setSaveError(null);
    setSaving(true);
    try {
      await setGithubRepo(owner.trim(), repo.trim());
      loadIssues();
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : "저장소 연결에 실패했어요.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="flex w-full flex-col gap-3.5">
      <p className="text-[13px] font-medium text-[#111827]">연결할 저장소</p>
      {canManage ? (
        <div className="flex w-full items-end gap-2.5">
          <div className="flex-1">
            <Field label="소유자" placeholder="owner" value={owner} onChange={(e) => setOwner(e.target.value)} />
          </div>
          <div className="flex-1">
            <Field label="저장소" placeholder="repo" value={repo} onChange={(e) => setRepo(e.target.value)} />
          </div>
          <Button
            variant="secondary"
            onClick={handleSaveRepo}
            disabled={saving || !owner.trim() || !repo.trim()}
            className="shrink-0 disabled:opacity-60"
          >
            {saving ? "저장 중..." : "저장"}
          </Button>
        </div>
      ) : (
        <p className="text-[12.5px] text-[#6b7280]">{status.detail ?? "아직 설정되지 않았어요."}</p>
      )}
      {saveError && <p className="text-[12.5px] text-[#ef4444]">{saveError}</p>}

      <p className="text-[13px] font-medium text-[#111827]">최근 열린 이슈</p>
      {loadingIssues && <p className="text-[12.5px] text-[#9ca3af]">불러오는 중...</p>}
      {issuesError && <p className="text-[12.5px] text-[#ef4444]">{issuesError}</p>}
      {!loadingIssues && !issuesError && issues.length === 0 && (
        <p className="text-[12.5px] text-[#9ca3af]">열려있는 이슈가 없어요.</p>
      )}
      <div className="flex max-h-[220px] w-full flex-col gap-2 overflow-auto">
        {issues.map((issue) => (
          <a
            key={issue.number}
            href={issue.htmlUrl}
            target="_blank"
            rel="noreferrer"
            className="flex w-full flex-col gap-0.5 rounded-lg border border-[#f0f0f2] p-2.5 hover:bg-[#f9fafb]"
          >
            <p className="text-[12.5px] font-medium text-[#111827]">#{issue.number} {issue.title}</p>
            <p className="text-[11.5px] text-[#6b7280]">{issue.authorLogin}</p>
          </a>
        ))}
      </div>
    </div>
  );
}

function SlackPanel({ canManage }: { canManage: boolean }) {
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const handleTest = async () => {
    setSending(true);
    setResult(null);
    try {
      await sendSlackTestMessage();
      setResult({ type: "success", text: "테스트 메시지를 보냈어요. 슬랙 채널을 확인해보세요." });
    } catch (err) {
      setResult({ type: "error", text: err instanceof ApiError ? err.message : "메시지 전송에 실패했어요." });
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="flex w-full flex-col gap-2.5">
      <p className="text-[12.5px] text-[#6b7280]">
        카드 이동 같은 알림 이벤트가 생기면 연결된 채널로 자동 전송돼요.
      </p>
      {canManage && (
        <Button variant="secondary" onClick={handleTest} disabled={sending} className="disabled:opacity-60">
          {sending ? "전송 중..." : "테스트 메시지 보내기"}
        </Button>
      )}
      {result && (
        <p className={`text-[12.5px] ${result.type === "error" ? "text-[#ef4444]" : "text-[#109568]"}`}>
          {result.text}
        </p>
      )}
    </div>
  );
}

function DrivePanel() {
  const [files, setFiles] = useState<DriveFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDriveFiles()
      .then(setFiles)
      .catch((err) => setError(err instanceof ApiError ? err.message : "파일 목록을 불러오지 못했어요."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="flex w-full flex-col gap-2.5">
      <p className="text-[13px] font-medium text-[#111827]">최근 파일</p>
      {loading && <p className="text-[12.5px] text-[#9ca3af]">불러오는 중...</p>}
      {error && <p className="text-[12.5px] text-[#ef4444]">{error}</p>}
      {!loading && !error && files.length === 0 && (
        <p className="text-[12.5px] text-[#9ca3af]">표시할 파일이 없어요.</p>
      )}
      <div className="flex max-h-[260px] w-full flex-col gap-2 overflow-auto">
        {files.map((file) => (
          <a
            key={file.id}
            href={file.webViewLink}
            target="_blank"
            rel="noreferrer"
            className="flex w-full items-center justify-between rounded-lg border border-[#f0f0f2] p-2.5 hover:bg-[#f9fafb]"
          >
            <p className="truncate text-[12.5px] font-medium text-[#111827]">{file.name}</p>
          </a>
        ))}
      </div>
    </div>
  );
}
