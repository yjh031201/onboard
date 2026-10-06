import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getStoredUser } from "../lib/auth";
import { listMembers } from "../lib/user";
import { roleLabel } from "../components/members/RoleCheckboxes";
import Button from "../components/ui/Button";
import Logo from "../components/ui/Logo";
import Pill from "../components/ui/Pill";

const ROLE_PILL: Record<string, { bg: string; text: string }> = {
  OWNER: { bg: "#eeeefe", text: "#6366f1" },
  ADMIN: { bg: "#ecf2fe", text: "#2f60e0" },
  MEMBER: { bg: "#f3f4f6", text: "#6b7280" },
};

/**
 * 로그인한 상태에서 "/"로 접속했을 때 보여주는 프로젝트 선택 화면.
 * 이 워크스페이스는 단일 팀/단일 보드 구조라 실제 프로젝트는 1개뿐이고,
 * "검색/정렬/새 프로젝트"는 여러 프로젝트를 지원하게 되면 쓸 자리를 미리 보여주는 용도라 지금은 비활성 처리했어요.
 */
export default function ProjectSelectPage() {
  const navigate = useNavigate();
  const currentUser = getStoredUser();
  const [memberCount, setMemberCount] = useState<number | null>(null);

  useEffect(() => {
    listMembers()
      .then((members) => setMemberCount(members.length))
      .catch(() => setMemberCount(null));
  }, []);

  const rolePill = currentUser ? ROLE_PILL[currentUser.role] ?? ROLE_PILL.MEMBER : ROLE_PILL.MEMBER;

  return (
    <div className="flex min-h-screen w-full flex-col bg-[#fafafa]">
      <nav className="flex w-full items-center justify-between bg-white px-[60px] py-[22px] shadow-[0px_2px_8px_rgba(0,0,0,0.04)]">
        <Logo textClassName="text-[16px] font-bold text-[#6366f1]" />
        <div className="flex items-center gap-3.5">
          <Button
            variant="primary"
            disabled
            title="여러 프로젝트 지원은 준비 중이에요"
            className="px-[18px] py-[9px] text-[13.5px] disabled:opacity-60"
          >
            + 새 프로젝트
          </Button>
          <button
            type="button"
            disabled
            title="알림 기능은 준비 중이에요"
            className="relative flex size-9 items-center justify-center rounded-full bg-[#f3f4f6] disabled:opacity-70"
          >
            <span className="text-[15px]">🔔</span>
            <span className="absolute right-1.5 top-1.5 size-2 rounded-full border border-white bg-[#ef4444]" />
          </button>
          <div className="flex size-9 items-center justify-center rounded-full bg-[#eeeefe]">
            <span className="text-[13px] font-bold text-[#6366f1]">
              {currentUser?.name?.slice(0, 1) ?? "?"}
            </span>
          </div>
        </div>
      </nav>

      <div className="flex flex-1 flex-col gap-7 px-[60px] pb-[100px] pt-12">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[24px] font-bold text-[#111827]">내 프로젝트</h1>
          <p className="text-[13.5px] text-[#6b7280]">최근에 작업한 프로젝트를 선택해서 이어가세요</p>
        </div>

        <div className="flex w-full items-center justify-between">
          <div className="flex h-10 w-[280px] items-center gap-2 rounded-lg border border-[#e5e7eb] bg-white px-3.5">
            <span className="text-[14px] text-[#9ca3af]">🔍</span>
            <input
              type="text"
              placeholder="프로젝트 검색"
              disabled
              title="여러 프로젝트 지원은 준비 중이에요"
              className="w-full bg-transparent text-[13.5px] text-[#9ca3af] outline-none"
            />
          </div>
          <button
            type="button"
            disabled
            title="여러 프로젝트 지원은 준비 중이에요"
            className="flex items-center gap-1.5 rounded-lg border border-[#e5e7eb] bg-white px-3.5 py-2.5 text-[13.5px] font-medium text-[#374151] disabled:opacity-70"
          >
            최근 수정순 <span className="text-[12px] text-[#9ca3af]">▾</span>
          </button>
        </div>

        <div className="flex flex-wrap gap-6">
          <button
            type="button"
            onClick={() => navigate("/dashboard")}
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
              <p className="text-[15.5px] font-bold text-[#111827]">칸반보드 프로젝트</p>
              <div className="flex items-center gap-2 text-[12.5px]">
                <span className="text-[#6b7280]">팀원 {memberCount ?? "-"}명</span>
                <span className="text-[#9ca3af]">·</span>
                <span className="text-[#6b7280]">진행 중</span>
              </div>
              {currentUser && (
                <Pill bg={rolePill.bg} text={rolePill.text}>
                  {roleLabel(currentUser.role)}
                </Pill>
              )}
            </div>
          </button>

          <div className="flex h-[244px] w-[320px] flex-col items-center justify-center gap-2 rounded-[14px] border-[1.5px] border-dashed border-[#e5e7eb] bg-white text-[#9ca3af]">
            <span className="text-[28px] font-bold">+</span>
            <span className="text-[13px] font-medium text-[#6b7280]">새 프로젝트 만들기</span>
          </div>
        </div>
      </div>
    </div>
  );
}
