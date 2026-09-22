import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import NotificationToasts from "./NotificationToasts";
import { isLoggedIn } from "../../lib/auth";
import { connectRealtime, disconnectRealtime } from "../../lib/realtime";

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (isLoggedIn()) {
      connectRealtime();
    }
    return () => disconnectRealtime();
  }, []);

  return (
    <div className="flex min-h-screen w-full items-start bg-[#fafafa]">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((prev) => !prev)} />
      <Outlet />
      <NotificationToasts />
    </div>
  );
}
