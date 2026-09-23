import { useEffect, useState } from "react";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field, TextareaField } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Toggle from "../../components/ui/Toggle";
import { ApiError } from "../../lib/api";
import { getStoredUser } from "../../lib/auth";
import { getProjectSettings, updateProjectSettings } from "../../lib/settings";

interface BoardColumn {
  id: string;
  name: string;
}

const INITIAL_COLUMNS: BoardColumn[] = [
  { id: "todo", name: "할 일" },
  { id: "in-progress", name: "진행 중" },
  { id: "done", name: "완료" },
];

interface LabelDef {
  id: string;
  name: string;
  color: string;
}

const INITIAL_LABELS: LabelDef[] = [
  { id: "bug", name: "버그", color: "#ef4444" },
  { id: "feature", name: "기능", color: "#6366f1" },
  { id: "design", name: "디자인", color: "#a855f7" },
  { id: "urgent", name: "긴급", color: "#f59e0b" },
];

export default function SettingsPage() {
  const currentUser = getStoredUser();
  // OWNER/ADMIN만 프로젝트 정보를 바꿀 수 있음 (서버에서도 동일하게 검증됨).
  const canEditProject = currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";

  const [projectName, setProjectName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "error" | "success"; text: string } | null>(null);
  const [columns] = useState<BoardColumn[]>(INITIAL_COLUMNS);
  const [labels] = useState<LabelDef[]>(INITIAL_LABELS);
  const [cardMoveNotif, setCardMoveNotif] = useState(true);
  const [commentNotif, setCommentNotif] = useState(true);
  const [dueDateNotif, setDueDateNotif] = useState(false);

  useEffect(() => {
    getProjectSettings()
      .then((settings) => {
        setProjectName(settings.projectName);
        setDescription(settings.description ?? "");
      })
      .catch((err) =>
        setMessage({
          type: "error",
          text: err instanceof ApiError ? err.message : "프로젝트 설정을 불러오지 못했어요.",
        }),
      )
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (!projectName.trim()) {
      setMessage({ type: "error", text: "프로젝트 이름을 입력해주세요." });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const saved = await updateProjectSettings(projectName.trim(), description);
      setProjectName(saved.projectName);
      setDescription(saved.description ?? "");
      setMessage({ type: "success", text: "저장했어요." });
    } catch (err) {
      setMessage({
        type: "error",
        text: err instanceof ApiError ? err.message : "프로젝트 설정을 저장하지 못했어요.",
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageShell title="설정" subtitle="프로젝트와 보드 설정을 관리하세요">
      <Section title="일반" description="프로젝트의 기본 정보를 관리하세요">
        <Field
          label="프로젝트 이름"
          placeholder={loading ? "불러오는 중..." : "프로젝트 이름을 입력하세요"}
          maxLength={100}
          disabled={loading || !canEditProject}
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
        />
        <TextareaField
          label="설명"
          rows={2}
          maxLength={2000}
          disabled={loading || !canEditProject}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div className="flex w-full items-center justify-between gap-3">
          <p
            className={`text-[12.5px] ${
              message?.type === "error" ? "text-[#ef4444]" : "text-[#6b7280]"
            }`}
          >
            {message?.text ??
              (canEditProject ? "" : "프로젝트 정보는 소유자/관리자만 변경할 수 있어요.")}
          </p>
          {canEditProject && (
            <Button
              variant="primary"
              disabled={loading || saving}
              onClick={handleSave}
              className="shrink-0 disabled:opacity-60"
            >
              {saving ? "저장 중..." : "변경사항 저장"}
            </Button>
          )}
        </div>
      </Section>

      <Section title="보드 컬럼" description="칸반보드에 표시할 컬럼을 관리하세요">
        {columns.map((column, i) => (
          <div key={column.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="text-[13px] font-bold text-[#9ca3af]">⠿</span>
                <p className="text-[13.5px] font-medium text-[#111827]">{column.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <button type="button" className="text-[#6366f1]">
                  이름 변경
                </button>
                <button type="button" className="text-[#9ca3af]">
                  삭제
                </button>
              </div>
            </div>
          </div>
        ))}
        <Button variant="secondary">+ 컬럼 추가</Button>
      </Section>

      <Section title="라벨" description="카드에 붙일 라벨 색상과 이름을 관리하세요">
        {labels.map((label, i) => (
          <div key={label.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span
                  className="size-3.5 rounded-[4px]"
                  style={{ backgroundColor: label.color }}
                />
                <p className="text-[13.5px] font-medium text-[#111827]">{label.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <button type="button" className="text-[#6366f1]">
                  편집
                </button>
                <button type="button" className="text-[#9ca3af]">
                  삭제
                </button>
              </div>
            </div>
          </div>
        ))}
        <Button variant="secondary">+ 라벨 추가</Button>
      </Section>

      <Section title="알림" description="이 프로젝트에서 받을 알림을 설정하세요">
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">카드 이동 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드가 다른 컬럼으로 이동하면 알려드려요</p>
          </div>
          <Toggle checked={cardMoveNotif} onChange={setCardMoveNotif} label="카드 이동 알림" />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">댓글 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드에 새 댓글이 달리면 알려드려요</p>
          </div>
          <Toggle checked={commentNotif} onChange={setCommentNotif} label="댓글 알림" />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">마감일 알림</p>
            <p className="text-[12px] text-[#6b7280]">마감일이 임박한 카드를 알려드려요</p>
          </div>
          <Toggle checked={dueDateNotif} onChange={setDueDateNotif} label="마감일 알림" />
        </div>
      </Section>

      <Section
        title="프로젝트 관리"
        description="프로젝트를 보관하거나 영구적으로 삭제할 수 있어요"
        danger
      >
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">프로젝트 보관</p>
            <p className="text-[12px] text-[#6b7280]">읽기 전용으로 전환되며 언제든 복구할 수 있어요</p>
          </div>
          <Button variant="secondary">보관하기</Button>
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">프로젝트 삭제</p>
            <p className="text-[12px] text-[#6b7280]">
              모든 카드와 데이터가 영구적으로 삭제되며 되돌릴 수 없어요
            </p>
          </div>
          <Button variant="danger">프로젝트 삭제</Button>
        </div>
      </Section>
    </PageShell>
  );
}
