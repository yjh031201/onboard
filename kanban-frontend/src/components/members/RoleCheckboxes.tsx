// 팀원 권한을 고르는 체크박스 — 실제로는 OWNER/ADMIN/MEMBER 중 하나만 고를 수 있는 단일 선택.
export type Role = "OWNER" | "ADMIN" | "MEMBER";

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: "OWNER", label: "소유자" },
  { value: "ADMIN", label: "관리자" },
  { value: "MEMBER", label: "멤버" },
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
          className="flex w-full cursor-pointer items-center gap-2.5 rounded-lg border border-[#e5e7eb] px-3.5 py-3 has-[:checked]:border-[#6366f1] has-[:checked]:bg-[#eeeefe]"
        >
          <input
            type="checkbox"
            checked={value === option.value}
            onChange={() => onChange(option.value)}
            className="size-4 accent-[#6366f1]"
          />
          <span className="text-[13.5px] font-medium text-[#111827]">{option.label}</span>
        </label>
      ))}
    </div>
  );
}
