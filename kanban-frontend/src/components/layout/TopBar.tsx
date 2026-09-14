interface TopBarProps {
  title: string;
  subtitle: string;
  userInitial: string;
}

export default function TopBar({ title, subtitle, userInitial }: TopBarProps) {
  return (
    <div className="flex w-full items-center justify-between">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[26px] font-bold text-[#111827]">{title}</h1>
        <p className="text-[13px] text-[#6b7280]">{subtitle}</p>
      </div>
      <div className="flex size-9 items-center justify-center rounded-full bg-[#6366f1]">
        <span className="text-[14px] font-bold text-white">{userInitial}</span>
      </div>
    </div>
  );
}
