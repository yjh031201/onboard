import { useEffect, useRef, useState } from "react";
import { roleLabel } from "../members/RoleCheckboxes";
import type { InvitationDto } from "../../lib/projectMembers";

interface InvitationBellProps {
  invitations: InvitationDto[];
  loading: boolean;
  onAccept: (projectId: number) => Promise<unknown>;
  onDecline: (projectId: number) => Promise<unknown>;
}

/** 메인화면 상단 알림 벨 — 받은 초대가 있으면 빨간 점이 뜨고, 누르면 드롭다운에서 바로 수락/거절할 수 있다. */
export default function InvitationBell({ invitations, loading, onAccept, onDecline }: InvitationBellProps) {
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  const handle = async (action: (projectId: number) => Promise<unknown>, projectId: number) => {
    setBusyId(projectId);
    try {
      await action(projectId);
    } catch {
      /* 실패해도 드롭다운을 다시 열면 최신 상태로 다시 불러와진다. */
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        title="받은 초대"
        className="relative flex size-9 items-center justify-center rounded-full bg-[#f3f4f6] hover:bg-[#e9ecf0]"
      >
        <span className="text-[15px]">🔔</span>
        {invitations.length > 0 && (
          <span className="absolute right-1.5 top-1.5 size-2 rounded-full border border-white bg-[#ef4444]" />
        )}
      </button>

      {open && (
        <div className="absolute top-full right-0 z-10 mt-2 w-[320px] overflow-hidden rounded-[10px] border border-[#ececee] bg-white py-2 shadow-lg">
          <p className="px-4 pb-1.5 text-[12.5px] font-bold text-[#111827]">받은 초대</p>

          {loading && <p className="px-4 py-3 text-[12.5px] text-[#9ca3af]">불러오는 중...</p>}
          {!loading && invitations.length === 0 && (
            <p className="px-4 py-3 text-[12.5px] text-[#9ca3af]">받은 초대가 없어요.</p>
          )}

          {!loading &&
            invitations.map((invite) => (
              <div
                key={invite.projectId}
                className="flex flex-col gap-1.5 border-t border-[#f0f0f2] px-4 py-3 first:border-t-0"
              >
                <p className="text-[13px] font-medium text-[#111827]">{invite.projectName}</p>
                <p className="text-[11.5px] text-[#6b7280]">{roleLabel(invite.role)}(으)로 초대받았어요</p>
                <div className="flex w-full justify-end gap-1.5 pt-1">
                  <button
                    type="button"
                    disabled={busyId === invite.projectId}
                    onClick={() => handle(onDecline, invite.projectId)}
                    className="rounded-md px-2.5 py-1 text-[12px] text-[#6b7280] hover:bg-[#f0f0f2] disabled:opacity-50"
                  >
                    거절
                  </button>
                  <button
                    type="button"
                    disabled={busyId === invite.projectId}
                    onClick={() => handle(onAccept, invite.projectId)}
                    className="rounded-md bg-[#6366f1] px-2.5 py-1 text-[12px] font-medium text-white hover:bg-[#4f46e5] disabled:opacity-50"
                  >
                    수락
                  </button>
                </div>
              </div>
            ))}
        </div>
      )}
    </div>
  );
}
