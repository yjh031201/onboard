import UserMenu from "./UserMenu";

interface TopBarProps {
  title: string;
  subtitle: string;
}

export default function TopBar({ title, subtitle }: TopBarProps) {
  return (
    <div className="flex w-full items-center justify-between">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[26px] font-bold text-[#111827]">{title}</h1>
        <p className="text-[13px] text-[#6b7280]">{subtitle}</p>
      </div>

      <UserMenu />
    </div>
  );
}
