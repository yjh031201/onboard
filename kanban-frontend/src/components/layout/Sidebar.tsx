import { NavLink } from "react-router-dom";

interface NavItemDef {
  to: string;
  label: string;
  icon: string;
  end?: boolean;
}

const NAV_ITEMS: NavItemDef[] = [
  { to: "/", label: "대시보드", icon: "🏠", end: true },
  { to: "/kanban", label: "칸반보드", icon: "📌" },
  { to: "/timeline", label: "타임라인", icon: "🕒" },
  { to: "/members", label: "팀원", icon: "👥" },
  { to: "/files", label: "파일", icon: "📁" },
  { to: "/settings", label: "설정", icon: "⚙️" },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export default function Sidebar({ collapsed, onToggle }: SidebarProps) {
  return (
    <div
      className={`flex h-full shrink-0 flex-col gap-7 border border-[#ececee] bg-white py-6 ${
        collapsed ? "w-[72px] items-center px-4" : "w-[240px] px-5"
      }`}
    >
      <div
        className={`flex w-full items-center ${
          collapsed ? "justify-center" : "justify-between"
        }`}
      >
        {!collapsed && (
          <p className="whitespace-nowrap text-[17px] font-bold text-[#111827]">
            📋 칸반보드
          </p>
        )}
        <button
          type="button"
          onClick={onToggle}
          aria-label={collapsed ? "사이드바 열기" : "사이드바 닫기"}
          className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#f3f4f6] text-[15px] text-[#374151] hover:bg-[#e9ecf0]"
        >
          ☰
        </button>
      </div>

      <nav className="flex w-full flex-col gap-1">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={`flex w-full items-center gap-2.5 rounded-lg py-2.5 text-[14px] whitespace-nowrap ${
              collapsed ? "justify-center" : "pr-3 pl-2.5"
            }`}
          >
            {({ isActive }) => (
              <>
                <span
                  className={`shrink-0 rounded-sm ${
                    isActive ? "h-4 w-[3px] bg-[#6366f1]" : "h-4 w-[3px] bg-transparent"
                  }`}
                />
                <span
                  className={isActive ? "font-medium text-[#6366f1]" : "font-normal text-[#374151]"}
                >
                  {collapsed ? item.icon : `${item.icon}  ${item.label}`}
                </span>
              </>
            )}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
