// 팀원 권한을 고르는 체크박스 — 실제로는 OWNER/ADMIN/MEMBER 중 하나만 고를 수 있는 단일 선택.
export type Role = "OWNER" | "ADMIN" | "MEMBER";

// 실제 서버 권한 체크(ProjectAccessService.requireAdmin)를 그대로 설명한 것 — 소유자/관리자는
// 권한상 동일(둘 다 "관리자 이상"으로 취급)하고, 멤버와의 차이는 팀원 초대·권한 변경 가능 여부와
// 남의 카드/파일/일정까지 관리할 수 있는지 여부다. 초대·권한 변경 화면에서 바로 보이게 설명을 붙인다.
const ROLE_OPTIONS: { value: Role; label: string; description: string }[] = [
  {
    value: "OWNER",
    label: "소유자",
    description: "프로젝트를 만든 사람에게 자동으로 부여돼요. 관리자와 동일하게 모든 권한을 가져요.",
  },
  {
    value: "ADMIN",
    label: "관리자",
    description: "소유자와 동일한 권한이에요. 팀원을 초대하고 권한을 바꿀 수 있고, 다른 사람의 카드·파일·일정도 수정·삭제할 수 있어요.",
  },
  {
    value: "MEMBER",
    label: "멤버",
    description: "보드·라벨·일정은 자유롭게 쓸 수 있지만 팀원 초대나 권한 변경은 할 수 없고, 본인이 만든 카드·파일·일정만 수정·삭제할 수 있어요.",
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
}

export default function RoleCheckboxes({ value, onChange }: RoleCheckboxesProps) {
  return (
    <div className="flex w-full flex-col gap-2.5">
      {ROLE_OPTIONS.map((option) => (
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
