import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Avatar from "../../components/ui/Avatar";
import Pill from "../../components/ui/Pill";
import { ApiError } from "../../lib/api";
import { listMembers } from "../../lib/user";
import { getStoredUser, type AuthUser } from "../../lib/auth";
import InvitePopup from "../../components/members/InvitePopup";
import RolePopup from "../../components/members/RolePopup";
import { roleLabel } from "../../components/members/RoleCheckboxes";
import IntegrationManagePopup from "../../components/members/IntegrationManagePopup";
import { fetchIntegrations, startConnect, type IntegrationProvider, type IntegrationStatus } from "../../lib/integrations";

const ROLE_PILL: Record<string, { bg: string; text: string }> = {
  OWNER: { bg: "#eeeefe", text: "#6366f1" },
  ADMIN: { bg: "#ecf2fe", text: "#2f60e0" },
  MEMBER: { bg: "#f3f4f6", text: "#6b7280" },
};

const INTEGRATION_META: Record<IntegrationProvider, { name: string; description: string }> = {
  GITHUB: { name: "GitHub", description: "코드 저장소와 이슈를 연결하세요" },
  SLACK: { name: "Slack", description: "알림을 슬랙 채널로 받아보세요" },
  GOOGLE_DRIVE: { name: "Google Drive", description: "파일을 동기화하고 첨부하세요" },
};

export default function MembersPage() {
  const [teamName, setTeamName] = useState("칸반보드 프로젝트팀");
  const currentUser = getStoredUser();

  const [members, setMembers] = useState<AuthUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [editingMember, setEditingMember] = useState<AuthUser | null>(null);

  const [integrations, setIntegrations] = useState<IntegrationStatus[]>([]);
  const [managingIntegration, setManagingIntegration] = useState<IntegrationStatus | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const [integrationBanner, setIntegrationBanner] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const loadIntegrations = () => {
    fetchIntegrations().then(setIntegrations).catch(() => {
      /* 연동 상태는 부가 정보라, 실패해도 나머지 페이지는 그대로 쓸 수 있게 조용히 둔다. */
    });
  };

  useEffect(() => {
    setLoading(true);
    setError(null);
    listMembers()
      .then(setMembers)
      .catch((err) => setError(err instanceof ApiError ? err.message : "팀원 목록을 불러오지 못했어요."))
      .finally(() => setLoading(false));

    loadIntegrations();
  }, []);

  // OAuth 콜백에서 돌아왔을 때(?integration=github&status=connected) 배너 한 번 띄우고 URL 정리.
  useEffect(() => {
    const status = searchParams.get("status");
    if (!status) return;

    const message = searchParams.get("message");
    setIntegrationBanner({
      type: status === "connected" ? "success" : "error",
      text: status === "connected" ? "연동이 완료됐어요." : (message ?? "연동에 실패했어요."),
    });
    if (status === "connected") loadIntegrations();
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.delete("integration");
      next.delete("status");
      next.delete("message");
      return next;
    }, { replace: true });
  }, []);

  const handleConnect = (provider: IntegrationProvider) => {
    startConnect(provider).catch((err) => {
      setIntegrationBanner({
        type: "error",
        text: err instanceof ApiError ? err.message : "연동을 시작하지 못했어요.",
      });
    });
  };

  // OWNER/ADMIN만 구성원을 초대하거나 권한을 바꿀 수 있음 (서버에서도 동일하게 검증됨).
  const canManageRoles = currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";

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
        <Field label="팀 이름" value={teamName} onChange={(e) => setTeamName(e.target.value)} />
        <div className="flex w-full justify-end">
          <Button variant="primary">변경사항 저장</Button>
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
        {integrationBanner && (
          <div
            className={`w-full rounded-[10px] px-4 py-3 ${
              integrationBanner.type === "error" ? "bg-[#fef2f2]" : "bg-[#e9f9f1]"
            }`}
          >
            <p className={`text-[13px] ${integrationBanner.type === "error" ? "text-[#ef4444]" : "text-[#109568]"}`}>
              {integrationBanner.text}
            </p>
          </div>
        )}
        {integrations.map((integration, i) => {
          const meta = INTEGRATION_META[integration.provider];
          return (
            <div key={integration.provider} className="flex w-full flex-col gap-[18px]">
              {i > 0 && <Divider />}
              <div className="flex w-full items-center justify-between">
                <div className="flex flex-col gap-[3px]">
                  <p className="text-[13.5px] font-medium text-[#111827]">{meta.name}</p>
                  <p className="text-[12px] text-[#6b7280]">{meta.description}</p>
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
                  {canManageRoles ? (
                    <Button
                      variant={integration.connected ? "secondary" : "primary"}
                      onClick={() =>
                        integration.connected ? setManagingIntegration(integration) : handleConnect(integration.provider)
                      }
                    >
                      {integration.connected ? "관리" : "연결하기"}
                    </Button>
                  ) : (
                    integration.connected && (
                      <Button variant="secondary" onClick={() => setManagingIntegration(integration)}>
                        보기
                      </Button>
                    )
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </Section>

      <Section title="팀 삭제" description="팀과 관련된 모든 데이터가 영구적으로 삭제되며 이 작업은 되돌릴 수 없습니다." danger>
        <Button variant="danger">팀 삭제</Button>
      </Section>

      {inviteOpen && <InvitePopup onClose={() => setInviteOpen(false)} onAdded={handleMemberAdded} />}

      {editingMember && (
        <RolePopup member={editingMember} onClose={() => setEditingMember(null)} onUpdated={handleMemberUpdated} />
      )}

      {managingIntegration && (
        <IntegrationManagePopup
          status={managingIntegration}
          canManage={canManageRoles}
          onClose={() => setManagingIntegration(null)}
          onDisconnected={loadIntegrations}
        />
      )}
    </PageShell>
  );
}
