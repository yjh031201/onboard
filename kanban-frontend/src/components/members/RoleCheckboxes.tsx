// 팀원 권한을 고르는 체크박스 — 실제로는 OWNER/ADMIN/MEMBER 중 하나만 고를 수 있는 단일 선택.
export type Role = "OWNER" | "ADMIN" | "MEMBER";

// 서버 권한 규칙(ProjectAccessService, ProjectMemberService)을 그대로 설명한 것.
// 소유자는 프로젝트마다 한 명이라 초대로는 고를 수 없고, 권한 변경 창에서 "소유자 넘기기"로만 고른다.
const ROLE_OPTIONS: { value: Role; label: string; description: string }[] = [
  {
    value: "OWNER",
    label: "소유자 넘기기",
    description: "이 팀원이 소유자가 되고, 나는 관리자가 돼요. 소유자는 프로젝트마다 한 명이고, 팀원 권한 변경과 프로젝트 삭제를 할 수 있어요.",
  },
  {
    value: "ADMIN",
    label: "관리자",
    description: "팀원을 초대하고 연동을 관리할 수 있고, 다른 사람의 카드·파일·일정도 수정·삭제할 수 있어요. 권한 변경과 프로젝트 삭제는 소유자만 해요.",
  },
  {
    value: "MEMBER",
    label: "멤버",
    description: "보드·라벨·일정은 자유롭게 쓸 수 있지만 팀원 초대는 할 수 없고, 본인이 만든 카드·파일·일정만 수정·삭제할 수 있어요.",
  },
];

const ROLE_LABEL: Record<Role, string> = {
  OWNER: "소유자",
  ADMIN: "관리자",
  MEMBER: "멤버",
};

export function roleLabel(role: string): string {
  return ROLE_LABEL[role as Role] ?? role;
}

interface RoleCheckboxesProps {
  value: Role;
  onChange: (role: Role) => void;
  /** 소유자 넘기기를 보여 줄지 — 초대 화면에서는 false. */
  allowOwner?: boolean;
}

export default function RoleCheckboxes({ value, onChange, allowOwner = false }: RoleCheckboxesProps) {
  const options = allowOwner ? ROLE_OPTIONS : ROLE_OPTIONS.filter((option) => option.value !== "OWNER");
  return (
    <div className="flex w-full flex-col gap-2.5">
      {options.map((option) => (
        <label
          key={option.value}
          className="flex w-full cursor-pointer items-start gap-2.5 rounded-lg border border-[#e5e7eb] px-3.5 py-3 has-[:checked]:border-[#6366f1] has-[:checked]:bg-[#eeeefe]"
        >
          <input
            type="checkbox"
            checked={value === option.value}
            onChange={() => onChange(option.value)}
            className="mt-0.5 size-4 accent-[#6366f1]"
          />
          <span className="flex flex-col gap-0.5">
            <span className="text-[13.5px] font-medium text-[#111827]">{option.label}</span>
            <span className="text-[12px] text-[#6b7280]">{option.description}</span>
          </span>
        </label>
      ))}
    </div>
  );
}
