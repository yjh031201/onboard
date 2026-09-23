import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getStoredUser, isLoggedIn, logout, type AuthUser } from "../../lib/auth";
import Button from "../ui/Button";
import PersonalSettingsModal from "../settings/PersonalSettingsModal";

interface TopBarProps {
  title: string;
  subtitle: string;
}

export default function TopBar({ title, subtitle }: TopBarProps) {
  const navigate = useNavigate();
  const loggedIn = isLoggedIn();
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser());

  const [menuOpen, setMenuOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  const handleLogout = () => {
    setMenuOpen(false);
    logout();
    navigate("/login", { replace: true });
  };

  const handleOpenSettings = () => {
    setMenuOpen(false);
    setSettingsOpen(true);
  };

  return (
    <div className="flex w-full items-center justify-between">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[26px] font-bold text-[#111827]">{title}</h1>
        <p className="text-[13px] text-[#6b7280]">{subtitle}</p>
      </div>

      {loggedIn && user ? (
        <div ref={menuRef} className="relative">
          <button
            type="button"
            onClick={() => setMenuOpen((prev) => !prev)}
            className="flex items-center gap-2.5 rounded-full py-1 pl-1 pr-3.5 transition hover:bg-[#f3f4f6]"
          >
            <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[#6366f1]">
              <span className="text-[14px] font-bold text-white">{user.name.slice(0, 1)}</span>
            </span>
            <span className="text-[13px] font-medium text-[#374151]">{user.name}</span>
          </button>

          {menuOpen && (
            <div className="absolute top-full right-0 z-10 mt-2 w-40 overflow-hidden rounded-[10px] border border-[#ececee] bg-white py-1.5 shadow-lg">
              <button
                type="button"
                onClick={handleOpenSettings}
                className="flex w-full items-center px-4 py-2.5 text-left text-[13px] text-[#374151] hover:bg-[#f3f4f6]"
              >
                설정
              </button>
              <button
                type="button"
                onClick={handleLogout}
                className="flex w-full items-center px-4 py-2.5 text-left text-[13px] text-[#ef4444] hover:bg-[#fef2f2]"
              >
                로그아웃
              </button>
            </div>
          )}

          {settingsOpen && (
            <PersonalSettingsModal
              user={user}
              onClose={() => setSettingsOpen(false)}
              onUpdated={(updated) => setUser(updated)}
            />
          )}
        </div>
      ) : (
        <div className="flex items-center gap-2.5">
          <Button variant="secondary" onClick={() => navigate("/signup")} className="px-5">
            회원가입
          </Button>
          <Button variant="primary" onClick={() => navigate("/login")} className="px-5">
            로그인
          </Button>
        </div>
      )}
    </div>
  );
}
