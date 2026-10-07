import { useEffect, useState } from "react";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Avatar from "../../components/ui/Avatar";
import Pill from "../../components/ui/Pill";
import { ApiError } from "../../lib/api";
import { listMembers } from "../../lib/user";
import { getStoredUser, type AuthUser } from "../../lib/auth";
import { deleteProject, getProjectSettings, updateTeamName } from "../../lib/settings";
import InvitePopup from "../../components/members/InvitePopup";
import RolePopup from "../../components/members/RolePopup";
import { roleLabel } from "../../components/members/RoleCheckboxes";

// 아직 아무도 팀 이름을 바꾸지 않았을 때 보여주는 이름.
const DEFAULT_TEAM_NAME = "칸반보드 프로젝트팀";

const ROLE_PILL: Record<string, { bg: string; text: string }> = {
  OWNER: { bg: "#eeeefe", text: "#6366f1" },
  ADMIN: { bg: "#ecf2fe", text: "#2f60e0" },
  MEMBER: { bg: "#f3f4f6", text: "#6b7280" },
};

interface IntegrationRow {
  id: string;
  name: string;
  description: string;
  connected: boolean;
}

const INTEGRATIONS: IntegrationRow[] = [
  { id: "github", name: "GitHub", description: "코드 저장소와 이슈를 연결하세요", connected: true },
  { id: "slack", name: "Slack", description: "알림을 슬랙 채널로 받아보세요", connected: false },
  {
    id: "drive",
    name: "Google Drive",
    description: "파일을 동기화하고 첨부하세요",
    connected: false,
  },
];

export default function MembersPage() {
  const [teamName, setTeamName] = useState("");
  const [teamNameLoading, setTeamNameLoading] = useState(true);
  const [savingTeamName, setSavingTeamName] = useState(false);
  const [teamNameMessage, setTeamNameMessage] = useState<{ type: "error" | "success"; text: string } | null>(null);
  const currentUser = getStoredUser();

  const [members, setMembers] = useState<AuthUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [editingMember, setEditingMember] = useState<AuthUser | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    listMembers()
      .then(setMembers)
      .catch((err) => setError(err instanceof ApiError ? err.message : "팀원 목록을 불러오지 못했어요."))
      .finally(() => setLoading(false));

    getProjectSettings()
      .then((settings) => setTeamName(settings.teamName || DEFAULT_TEAM_NAME))
      .catch((err) =>
        setTeamNameMessage({
          type: "error",
          text: err instanceof ApiError ? err.message : "팀 이름을 불러오지 못했어요.",
        }),
      )
      .finally(() => setTeamNameLoading(false));
  }, []);

  // OWNER/ADMIN만 구성원을 초대하거나 권한을 바꿀 수 있음 (서버에서도 동일하게 검증됨).
  const canManageRoles = currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";
  // 팀 삭제는 OWNER만 (서버에서도 동일하게 검증됨).
  const canDeleteTeam = currentUser?.role === "OWNER";

  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // 단일 워크스페이스 구조라 팀 삭제는 프로젝트 삭제와 같은 범위(같은 API)로 동작한다.
  const handleDeleteTeam = async () => {
    // 되돌릴 수 없는 작업이라 실수로 누른 것이 아닌지, 직접 입력받아 확인한다.
    const confirmText = "삭제";
    const typed = window.prompt(
      "팀의 모든 카드, 일정, 파일, 활동 기록이 영구적으로 삭제되며 되돌릴 수 없어요.\n" +
        "(팀원 계정과 컬럼·라벨 구성은 남아요.)\n\n" +
        `계속하려면 "${confirmText}"을(를) 입력하세요.`,
    );
    if (typed === null) return;
    if (typed.trim() !== confirmText) {
      setDeleteError("입력한 내용이 일치하지 않아 삭제하지 않았어요.");
      return;
    }

    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteProject();
      window.alert("팀을 삭제했어요.");
      // 화면 곳곳에 남아 있는 카드·일정·파일 목록을 한 번에 비우기 위해 새로 불러온다.
      window.location.assign("/dashboard");
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : "팀을 삭제하지 못했어요.");
      setDeleting(false);
    }
  };

  const handleSaveTeamName = async () => {
    if (!teamName.trim()) {
      setTeamNameMessage({ type: "error", text: "팀 이름을 입력해주세요." });
      return;
    }
    setSavingTeamName(true);
    setTeamNameMessage(null);
    try {
      const saved = await updateTeamName(teamName.trim());
      setTeamName(saved.teamName);
      setTeamNameMessage({ type: "success", text: "저장했어요." });
    } catch (err) {
      setTeamNameMessage({
        type: "error",
        text: err instanceof ApiError ? err.message : "팀 이름을 저장하지 못했어요.",
      });
    } finally {
      setSavingTeamName(false);
    }
  };

  const handleMemberUpdated = (updated: AuthUser) => {
    setMembers((prev) => prev.map((m) => (m.id === updated.id ? updated : m)));
  };

  const handleMemberAdded = (added: AuthUser) => {
    setMembers((prev) => {
      const exists = prev.some((m) => m.id === added.id);
      return exists ? prev.map((m) => (m.id === added.id ? added : m)) : [...prev, added];
    });
  };

  return (
    <PageShell title="팀원" subtitle="팀원을 초대하고 팀 정보를 관리하세요">
      <Section title="일반" description="팀의 기본 정보를 관리하세요">
        <Field
          label="팀 이름"
          placeholder={teamNameLoading ? "불러오는 중..." : "팀 이름을 입력하세요"}
          maxLength={100}
          disabled={teamNameLoading || !canManageRoles}
          value={teamName}
          onChange={(e) => setTeamName(e.target.value)}
        />
        <div className="flex w-full items-center justify-between gap-3">
          <p
            className={`text-[12.5px] ${
              teamNameMessage?.type === "error" ? "text-[#ef4444]" : "text-[#6b7280]"
            }`}
          >
            {teamNameMessage?.text ?? (!canManageRoles ? "팀 이름은 소유자/관리자만 변경할 수 있어요." : "")}
          </p>
          {canManageRoles && (
            <Button
              variant="primary"
              disabled={teamNameLoading || savingTeamName}
              onClick={handleSaveTeamName}
              className="shrink-0 disabled:opacity-60"
            >
              {savingTeamName ? "저장 중..." : "변경사항 저장"}
            </Button>
          )}
        </div>
      </Section>

      <Section title="구성원 및 권한" description="이메일 또는 휴대폰번호로 팀원을 검색해서 추가하고 권한을 관리하세요">
        <div className="flex w-full items-center justify-between">
          <p className="text-[12.5px] font-medium text-[#6b7280]">총 {members.length}명</p>
          {canManageRoles && (
            <Button variant="primary" onClick={() => setInviteOpen(true)}>
              + 구성원 초대
            </Button>
          )}
        </div>

        {loading && <p className="text-[13px] text-[#9ca3af]">불러오는 중...</p>}

        {error && (
          <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
            <p className="text-[13px] text-[#ef4444]">{error}</p>
          </div>
        )}

        {!loading &&
          !error &&
          members.map((member, i) => (
            <div key={member.id} className="flex w-full flex-col gap-[18px]">
              {i > 0 && <Divider />}
              <div className="flex w-full items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <Avatar initial={member.name.slice(0, 1)} />
                  <div className="flex flex-col gap-0.5">
                    <p className="text-[13.5px] font-medium text-[#111827]">{member.name}</p>
                    <p className="text-[12px] text-[#6b7280]">{member.email}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3.5">
                  <Pill bg={ROLE_PILL[member.role]?.bg ?? "#f3f4f6"} text={ROLE_PILL[member.role]?.text ?? "#6b7280"}>
                    {roleLabel(member.role)}
                  </Pill>
                  {canManageRoles && member.id !== currentUser?.id && (
                    <button
                      type="button"
                      onClick={() => setEditingMember(member)}
                      className="text-[12.5px] text-[#6366f1]"
                    >
                      권한 변경
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
      </Section>

      <Section title="연동" description="외부 서비스와 연결해 팀 작업을 더 편리하게 만드세요">
        {INTEGRATIONS.map((integration, i) => (
          <div key={integration.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex flex-col gap-[3px]">
                <p className="text-[13.5px] font-medium text-[#111827]">{integration.name}</p>
                <p className="text-[12px] text-[#6b7280]">{integration.description}</p>
              </div>
              <div className="flex items-center gap-3">
                {integration.connected ? (
                  <Pill bg="#e9f9f1" text="#109568">
                    연결됨
                  </Pill>
                ) : (
                  <Pill bg="#f3f4f6" text="#6b7280">
                    연결 안 됨
                  </Pill>
                )}
                <Button variant={integration.connected ? "secondary" : "primary"}>
                  {integration.connected ? "관리" : "연결하기"}
                </Button>
              </div>
            </div>
          </div>
        ))}
      </Section>

      <Section
        title="팀 삭제"
        description="팀의 모든 카드·일정·파일·활동 기록이 영구적으로 삭제되며 이 작업은 되돌릴 수 없습니다. 팀원 계정과 컬럼·라벨 구성은 남아요."
        danger
      >
        <Button
          variant="danger"
          disabled={deleting || !canDeleteTeam}
          title={canDeleteTeam ? undefined : "소유자만 삭제할 수 있어요"}
          onClick={handleDeleteTeam}
          className="disabled:cursor-not-allowed disabled:opacity-60"
        >
          팀 삭제
        </Button>
        {deleteError && <p className="text-[12.5px] text-[#ef4444]">{deleteError}</p>}
      </Section>

      {inviteOpen && <InvitePopup onClose={() => setInviteOpen(false)} onAdded={handleMemberAdded} />}

      {editingMember && (
        <RolePopup member={editingMember} onClose={() => setEditingMember(null)} onUpdated={handleMemberUpdated} />
      )}
    </PageShell>
  );
}
