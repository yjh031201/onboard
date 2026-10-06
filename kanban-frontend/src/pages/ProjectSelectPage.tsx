import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listProjects, type ProjectSummary } from "../lib/projects";
import { roleLabel } from "../components/members/RoleCheckboxes";
import { ApiError } from "../lib/api";
import Button from "../components/ui/Button";
import Logo from "../components/ui/Logo";
import Pill from "../components/ui/Pill";
import UserMenu from "../components/layout/UserMenu";
import CreateProjectModal from "../components/projects/CreateProjectModal";
import InvitationBell from "../components/invitations/InvitationBell";
import { useInvitations } from "../hooks/useInvitations";

const ROLE_PILL: Record<string, { bg: string; text: string }> = {
  OWNER: { bg: "#eeeefe", text: "#6366f1" },
  ADMIN: { bg: "#ecf2fe", text: "#2f60e0" },
  MEMBER: { bg: "#f3f4f6", text: "#6b7280" },
};

/**
 * 로그인한 상태에서 "/"로 접속했을 때 보여주는 프로젝트 선택 화면.
 * 한 사람이 여러 프로젝트를 가질 수 있어서, 내가 속한 프로젝트 목록을 그대로 카드로 보여주고,
 * "+ 새 프로젝트"로 만들면 바로 그 프로젝트의 팀원 페이지로 이동해서 초대할 수 있다.
 */
export default function ProjectSelectPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const { invitations, loading: invitationsLoading, accept, decline } = useInvitations();

  const loadProjects = () => {
    setLoading(true);
    setError(null);
    listProjects()
      .then(setProjects)
      .catch((err) => setError(err instanceof ApiError ? err.message : "프로젝트 목록을 불러오지 못했어요."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadProjects();
  }, []);

  // 초대를 수락하면 그 프로젝트가 바로 "내 프로젝트" 목록에도 나타나야 하니 같이 새로고침한다.
  const handleAccept = (projectId: number) => accept(projectId).then(() => loadProjects());

  return (
    <div className="flex min-h-screen w-full flex-col bg-[#fafafa]">
      <nav className="flex w-full items-center justify-between bg-white px-[60px] py-[22px] shadow-[0px_2px_8px_rgba(0,0,0,0.04)]">
        <Logo textClassName="text-[16px] font-bold text-[#6366f1]" />
        <div className="flex items-center gap-3.5">
          <Button
            variant="primary"
            onClick={() => setCreateOpen(true)}
            className="px-[18px] py-[9px] text-[13.5px]"
          >
            + 새 프로젝트
          </Button>
          <InvitationBell
            invitations={invitations}
            loading={invitationsLoading}
            onAccept={handleAccept}
            onDecline={decline}
          />
          <UserMenu />
        </div>
      </nav>

      <div className="flex flex-1 flex-col gap-7 px-[60px] pb-[100px] pt-12">
        {invitations.length > 0 && (
          <div className="flex flex-col gap-3.5">
            <h2 className="text-[15.5px] font-bold text-[#111827]">나에게 온 초대</h2>
            <div className="flex flex-wrap gap-4">
              {invitations.map((invite) => (
                <div
                  key={invite.projectId}
                  className="flex w-[320px] flex-col gap-2.5 rounded-[14px] border border-[#e5e7eb] bg-white p-5 shadow-[0px_2px_8px_rgba(0,0,0,0.04)]"
                >
                  <p className="text-[14.5px] font-bold text-[#111827]">{invite.projectName}</p>
                  {invite.projectDescription && (
                    <p className="line-clamp-2 text-[12.5px] text-[#6b7280]">{invite.projectDescription}</p>
                  )}
                  <p className="text-[12px] text-[#6b7280]">{roleLabel(invite.role)}(으)로 초대받았어요</p>
                  <div className="flex w-full justify-end gap-2 pt-1">
                    <Button variant="secondary" onClick={() => decline(invite.projectId)} className="px-3.5 py-1.5 text-[12.5px]">
                      거절
                    </Button>
                    <Button variant="primary" onClick={() => handleAccept(invite.projectId)} className="px-3.5 py-1.5 text-[12.5px]">
                      수락
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="flex flex-col gap-1.5">
          <h1 className="text-[24px] font-bold text-[#111827]">내 프로젝트</h1>
          <p className="text-[13.5px] text-[#6b7280]">최근에 작업한 프로젝트를 선택해서 이어가세요</p>
        </div>

        {loading && <p className="text-[13.5px] text-[#9ca3af]">불러오는 중...</p>}

        {error && (
          <div className="w-full rounded-[10px] bg-[#fef2f2] px-4 py-3">
            <p className="text-[13px] text-[#ef4444]">{error}</p>
          </div>
        )}

        {!loading && !error && (
          <div className="flex flex-wrap gap-6">
            {projects.map((project) => {
              const rolePill = ROLE_PILL[project.myRole] ?? ROLE_PILL.MEMBER;
              return (
                <button
                  key={project.id}
                  type="button"
                  onClick={() => navigate(`/projects/${project.id}/dashboard`)}
                  className="flex w-[320px] flex-col overflow-hidden rounded-[14px] bg-white text-left shadow-[0px_4px_10px_rgba(0,0,0,0.05)] transition-transform hover:-translate-y-0.5"
                >
                  <div className="flex h-[140px] w-full gap-2 rounded-t-[14px] bg-[#f3f4f6] p-4">
                    <div className="flex flex-1 flex-col justify-start gap-1.5">
                      {Array.from({ length: 3 }).map((_, i) => (
                        <div key={i} className="h-2.5 w-full rounded bg-[#9ca3af]/55" />
                      ))}
                    </div>
                    <div className="flex flex-1 flex-col justify-start gap-1.5">
                      {Array.from({ length: 2 }).map((_, i) => (
                        <div key={i} className="h-2.5 w-full rounded bg-[#f59e0b]/55" />
                      ))}
                    </div>
                    <div className="flex flex-1 flex-col justify-start gap-1.5">
                      {Array.from({ length: 4 }).map((_, i) => (
                        <div key={i} className="h-2.5 w-full rounded bg-[#10b981]/55" />
                      ))}
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 px-5 pb-5 pt-4">
                    <p className="text-[15.5px] font-bold text-[#111827]">{project.name}</p>
                    {project.description && (
                      <p className="line-clamp-2 text-[12.5px] text-[#6b7280]">{project.description}</p>
                    )}
                    <Pill bg={rolePill.bg} text={rolePill.text}>
                      {roleLabel(project.myRole)}
                    </Pill>
                  </div>
                </button>
              );
            })}

            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="flex h-[244px] w-[320px] flex-col items-center justify-center gap-2 rounded-[14px] border-[1.5px] border-dashed border-[#e5e7eb] bg-white text-[#9ca3af] hover:border-[#6366f1] hover:text-[#6366f1]"
            >
              <span className="text-[28px] font-bold">+</span>
              <span className="text-[13px] font-medium">새 프로젝트 만들기</span>
            </button>
          </div>
        )}
      </div>

      {createOpen && <CreateProjectModal onClose={() => setCreateOpen(false)} />}
    </div>
  );
}
