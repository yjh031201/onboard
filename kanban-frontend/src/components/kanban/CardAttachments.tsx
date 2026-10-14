import { useEffect, useState, type ReactNode } from "react";
import Button from "../ui/Button";
import { ApiError } from "../../lib/api";
import { alertCardError, attachToCard, canDetach, detachFromCard, importGithubIssues } from "../../lib/board";
import { fetchDriveFiles, fetchGithubIssues, type DriveFile, type GithubIssue } from "../../lib/integrations";
import type { ColumnDef } from "../../lib/boardColumns";
import type { ConnectedIntegrations } from "../../hooks/useConnectedIntegrations";
import type { CardAttachment, TaskCard } from "../../types/dashboard";

/** Drive 파일 종류별 아이콘. */
function driveIcon(mimeType: string | null): string {
  if (!mimeType) return "📎";
  if (mimeType.endsWith(".folder")) return "📁";
  if (mimeType.includes("spreadsheet") || mimeType.includes("excel")) return "📊";
  if (mimeType.includes("presentation") || mimeType.includes("powerpoint")) return "📽️";
  if (mimeType.startsWith("image/")) return "🖼️";
  if (mimeType.includes("pdf")) return "📕";
  return "📄";
}

function attachmentIcon(attachment: CardAttachment): string {
  return attachment.kind === "GITHUB_ISSUE" ? "🐙" : driveIcon(attachment.mimeType);
}

interface AttachmentListProps {
  projectId: number;
  task: TaskCard;
  /** 이 프로젝트에서 OWNER/ADMIN인지 — 남이 붙인 첨부를 뗄 수 있는지 판단에 쓴다. */
  isAdmin: boolean;
}

/** 카드에 붙은 GitHub 이슈·Drive 파일 링크 — 누르면 새 탭에서 열린다. */
export function AttachmentList({ projectId, task, isAdmin }: AttachmentListProps) {
  if (task.attachments.length === 0) return null;

  const detach = (attachment: CardAttachment) => {
    if (!window.confirm(`'${attachment.title}' 첨부를 뗄까요?`)) return;
    detachFromCard(projectId, task.id, attachment.id).catch((err) => alertCardError(err, "첨부를 떼지 못했어요."));
  };

  return (
    <div className="flex w-full flex-col gap-1 pl-[18px]">
      {task.attachments.map((attachment) => (
        <div key={attachment.id} className="group/attachment flex w-full min-w-0 items-center gap-1">
          <a
            href={attachment.url}
            target="_blank"
            rel="noreferrer"
            title={`${attachment.title} — ${attachment.attachedByName}님이 첨부`}
            className="flex min-w-0 flex-1 items-center gap-1.5 rounded-md bg-[#f6f7f9] px-2 py-1 text-[11.5px] text-[#374151] hover:bg-[#eeeefe] hover:text-[#4f46e5]"
          >
            <span className="shrink-0">{attachmentIcon(attachment)}</span>
            <span className="truncate">{attachment.title}</span>
          </a>
          {canDetach(task, attachment, isAdmin) && (
            <button
              type="button"
              title="첨부 떼기"
              aria-label={`'${attachment.title}' 첨부 떼기`}
              onClick={() => detach(attachment)}
              className="flex size-5 shrink-0 items-center justify-center rounded text-[13px] leading-none text-[#9ca3af] opacity-0 group-hover/attachment:opacity-100 hover:bg-[#fee2e2] hover:text-[#ef4444]"
            >
              ×
            </button>
          )}
        </div>
      ))}
    </div>
  );
}

interface PopupProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/** 연동 관리 팝업(IntegrationManagePopup)과 같은 모양의 모달. */
function Popup({ title, onClose, children }: PopupProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button type="button" aria-label="닫기" onClick={onClose} className="absolute inset-0 bg-black/45" />
      <div className="relative flex w-[480px] max-w-[90vw] max-h-[85vh] flex-col gap-4 overflow-auto rounded-2xl bg-white p-7 shadow-[0px_12px_32px_0px_rgba(0,0,0,0.18)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">{title}</p>
          <button type="button" onClick={onClose} aria-label="닫기" className="text-[15px] text-[#9ca3af] hover:text-[#6b7280]">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

const ROW_CLASS =
  "flex w-full items-center justify-between gap-3 rounded-lg border border-[#f0f0f2] p-2.5 text-left";

interface AttachPopupProps {
  projectId: number;
  task: TaskCard;
  integrations: ConnectedIntegrations;
  onClose: () => void;
}

/** 카드에 GitHub 이슈 / Drive 파일을 붙이는 팝업. 연결된 연동만 탭으로 보인다. */
export function AttachPopup({ projectId, task, integrations, onClose }: AttachPopupProps) {
  const [tab, setTab] = useState<"GITHUB_ISSUE" | "DRIVE_FILE">(integrations.githubRepo ? "GITHUB_ISSUE" : "DRIVE_FILE");
  const [pendingRef, setPendingRef] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const isAttached = (kind: CardAttachment["kind"], externalId: string) =>
    task.attachments.some((attachment) => attachment.kind === kind && attachment.externalId === externalId);

  const attach = async (ref: string) => {
    setError(null);
    setPendingRef(ref);
    try {
      await attachToCard(projectId, task.id, tab, ref);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "첨부하지 못했어요.");
    } finally {
      setPendingRef(null);
    }
  };

  return (
    <Popup title={`'${task.title}'에 첨부`} onClose={onClose}>
      <div className="flex gap-1.5">
        {integrations.githubRepo && (
          <TabButton active={tab === "GITHUB_ISSUE"} onClick={() => setTab("GITHUB_ISSUE")}>
            GitHub 이슈
          </TabButton>
        )}
        {integrations.drive && (
          <TabButton active={tab === "DRIVE_FILE"} onClick={() => setTab("DRIVE_FILE")}>
            Google Drive 파일
          </TabButton>
        )}
      </div>

      {error && <p className="text-[12.5px] text-[#ef4444]">{error}</p>}

      {tab === "GITHUB_ISSUE" ? (
        <IssueList
          projectId={projectId}
          render={(issue) => {
            const attached = isAttached("GITHUB_ISSUE", `${integrations.githubRepo}#${issue.number}`);
            return (
              <Button
                variant={attached ? "secondary" : "ghost"}
                disabled={attached || pendingRef !== null}
                onClick={() => attach(String(issue.number))}
                className="shrink-0 px-3 py-1.5 disabled:opacity-60"
              >
                {attached ? "첨부됨" : pendingRef === String(issue.number) ? "첨부 중..." : "첨부"}
              </Button>
            );
          }}
        />
      ) : (
        <DriveFileList
          projectId={projectId}
          render={(file) => {
            const attached = isAttached("DRIVE_FILE", file.id);
            return (
              <Button
                variant={attached ? "secondary" : "ghost"}
                disabled={attached || pendingRef !== null}
                onClick={() => attach(file.id)}
                className="shrink-0 px-3 py-1.5 disabled:opacity-60"
              >
                {attached ? "첨부됨" : pendingRef === file.id ? "첨부 중..." : "첨부"}
              </Button>
            );
          }}
        />
      )}
      {tab === "DRIVE_FILE" && (
        <p className="text-[11.5px] text-[#9ca3af]">
          파일 자체가 아니라 링크가 붙어요. 팀원이 열려면 그 파일에 Drive 공유 권한이 있어야 해요.
        </p>
      )}
    </Popup>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full border px-3 py-1 text-[12.5px] font-medium transition-colors ${
        active ? "border-[#6366f1] bg-[#eeeefe] text-[#4f46e5]" : "border-[#e5e7eb] bg-white text-[#374151] hover:bg-[#f9fafb]"
      }`}
    >
      {children}
    </button>
  );
}

/** 연결된 저장소의 열린 이슈 목록 — 오른쪽 버튼은 render로 받는다. */
function IssueList({ projectId, render }: { projectId: number; render: (issue: GithubIssue) => ReactNode }) {
  const [issues, setIssues] = useState<GithubIssue[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchGithubIssues(projectId)
      .then(setIssues)
      .catch((err) => setError(err instanceof ApiError ? err.message : "이슈를 불러오지 못했어요."))
      .finally(() => setLoading(false));
  }, [projectId]);

  return (
    <ListBox loading={loading} error={error} empty={issues.length === 0} emptyText="열려있는 이슈가 없어요.">
      {issues.map((issue) => (
        <div key={issue.number} className={ROW_CLASS}>
          <a href={issue.htmlUrl} target="_blank" rel="noreferrer" className="flex min-w-0 flex-col gap-0.5 hover:underline">
            <p className="truncate text-[12.5px] font-medium text-[#111827]">
              #{issue.number} {issue.title}
            </p>
            <p className="text-[11.5px] text-[#6b7280]">{issue.authorLogin}</p>
          </a>
          {render(issue)}
        </div>
      ))}
    </ListBox>
  );
}

/** 연결된 Drive의 최근 파일 목록. */
function DriveFileList({ projectId, render }: { projectId: number; render: (file: DriveFile) => ReactNode }) {
  const [files, setFiles] = useState<DriveFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDriveFiles(projectId)
      .then(setFiles)
      .catch((err) => setError(err instanceof ApiError ? err.message : "파일 목록을 불러오지 못했어요."))
      .finally(() => setLoading(false));
  }, [projectId]);

  return (
    <ListBox loading={loading} error={error} empty={files.length === 0} emptyText="표시할 파일이 없어요.">
      {files.map((file) => (
        <div key={file.id} className={ROW_CLASS}>
          <a
            href={file.webViewLink}
            target="_blank"
            rel="noreferrer"
            className="flex min-w-0 items-center gap-1.5 hover:underline"
          >
            <span className="shrink-0">{driveIcon(file.mimeType)}</span>
            <span className="truncate text-[12.5px] font-medium text-[#111827]">{file.name}</span>
          </a>
          {render(file)}
        </div>
      ))}
    </ListBox>
  );
}

interface ListBoxProps {
  loading: boolean;
  error: string | null;
  empty: boolean;
  emptyText: string;
  children: ReactNode;
}

function ListBox({ loading, error, empty, emptyText, children }: ListBoxProps) {
  if (loading) return <p className="text-[12.5px] text-[#9ca3af]">불러오는 중...</p>;
  if (error) return <p className="text-[12.5px] text-[#ef4444]">{error}</p>;
  if (empty) return <p className="text-[12.5px] text-[#9ca3af]">{emptyText}</p>;
  return <div className="flex max-h-[320px] w-full flex-col gap-2 overflow-auto">{children}</div>;
}

interface GithubImportPopupProps {
  projectId: number;
  githubRepo: string;
  columns: ColumnDef[];
  tasks: TaskCard[];
  onClose: () => void;
}

/** 연결된 저장소의 이슈를 골라 칸반 카드로 가져오는 팝업. 이미 가져온 이슈는 고를 수 없다. */
export function GithubImportPopup({ projectId, githubRepo, columns, tasks, onClose }: GithubImportPopupProps) {
  const [selected, setSelected] = useState<number[]>([]);
  const [columnId, setColumnId] = useState(columns[0]?.id ?? "");
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const importedNumbers = new Set(
    tasks.flatMap((task) =>
      task.attachments
        .filter((attachment) => attachment.kind === "GITHUB_ISSUE" && attachment.externalId.startsWith(`${githubRepo}#`))
        .map((attachment) => Number(attachment.externalId.slice(githubRepo.length + 1))),
    ),
  );

  const toggle = (number: number) =>
    setSelected((prev) => (prev.includes(number) ? prev.filter((n) => n !== number) : [...prev, number]));

  const submit = async () => {
    setError(null);
    setImporting(true);
    try {
      await importGithubIssues(projectId, columnId, selected);
      onClose(); // 새 카드는 board 스냅샷으로 들어온다.
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "이슈를 가져오지 못했어요.");
    } finally {
      setImporting(false);
    }
  };

  return (
    <Popup title="GitHub 이슈 가져오기" onClose={onClose}>
      <p className="text-[12.5px] text-[#6b7280]">
        <span className="font-medium text-[#111827]">{githubRepo}</span>의 열린 이슈를 골라 카드로 만들어요. 카드에는 원본
        이슈 링크가 같이 붙어요.
      </p>

      <IssueList
        projectId={projectId}
        render={(issue) => {
          const imported = importedNumbers.has(issue.number);
          return imported ? (
            <span className="shrink-0 text-[11.5px] text-[#9ca3af]">가져옴</span>
          ) : (
            <input
              type="checkbox"
              aria-label={`#${issue.number} 선택`}
              checked={selected.includes(issue.number)}
              onChange={() => toggle(issue.number)}
              className="size-4 shrink-0 accent-[#6366f1]"
            />
          );
        }}
      />

      <label className="flex items-center gap-2 text-[12.5px] text-[#374151]">
        넣을 컬럼
        <select
          value={columnId}
          onChange={(e) => setColumnId(e.target.value)}
          className="rounded-lg border border-[#e5e7eb] bg-white px-2.5 py-1.5 text-[12.5px]"
        >
          {columns.map((column) => (
            <option key={column.id} value={column.id}>
              {column.name}
            </option>
          ))}
        </select>
      </label>

      {error && <p className="text-[12.5px] text-[#ef4444]">{error}</p>}

      <div className="flex w-full justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>
          취소
        </Button>
        <Button onClick={submit} disabled={importing || selected.length === 0 || !columnId} className="disabled:opacity-60">
          {importing ? "가져오는 중..." : `${selected.length}개 가져오기`}
        </Button>
      </div>
    </Popup>
  );
}
