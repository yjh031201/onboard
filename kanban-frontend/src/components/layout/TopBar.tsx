import { useNavigate } from "react-router-dom";
import { getStoredUser, isLoggedIn, logout } from "../../lib/auth";
import Button from "../ui/Button";

interface TopBarProps {
  title: string;
  subtitle: string;
}

export default function TopBar({ title, subtitle }: TopBarProps) {
  const navigate = useNavigate();
  const loggedIn = isLoggedIn();
  const user = getStoredUser();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="flex w-full items-center justify-between">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-[26px] font-bold text-[#111827]">{title}</h1>
        <p className="text-[13px] text-[#6b7280]">{subtitle}</p>
      </div>

      {loggedIn && user ? (
        <button
          type="button"
          onClick={handleLogout}
          title="로그아웃"
          className="flex items-center gap-2.5 rounded-full py-1 pl-1 pr-3.5 transition hover:bg-[#f3f4f6]"
        >
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-[#6366f1]">
            <span className="text-[14px] font-bold text-white">{user.name.slice(0, 1)}</span>
          </span>
          <span className="text-[13px] font-medium text-[#374151]">{user.name}</span>
        </button>
      ) : (
        <Button variant="primary" onClick={() => navigate("/login")} className="px-5">
          로그인
        </Button>
      )}
    </div>
  );
}
